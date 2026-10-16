package com.example.correoplus;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import android.os.BatteryManager;
import android.content.IntentFilter;
import androidx.work.PeriodicWorkRequest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import androidx.work.BackoffPolicy;
import com.sun.mail.imap.IMAPFolder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.UIDFolder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeUtility;

public final class MailBackground extends Worker {
  private static final java.util.concurrent.atomic.AtomicBoolean RUNNING=new java.util.concurrent.atomic.AtomicBoolean(false);
  public MailBackground(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
  public static void schedule(Context context){
    SharedPreferences p=context.getSharedPreferences("correoplus_settings",Context.MODE_PRIVATE);
    WorkManager manager=WorkManager.getInstance(context);
    if(!p.getBoolean("background_sync",false)){manager.cancelUniqueWork("correo_background");return;}
    Constraints c=new Constraints.Builder()
      .setRequiredNetworkType(p.getBoolean("sync_wifi_only",false)?NetworkType.UNMETERED:NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(p.getBoolean("sync_battery_guard",true)).build();
    PeriodicWorkRequest req=new PeriodicWorkRequest.Builder(MailBackground.class,15,TimeUnit.MINUTES).setConstraints(c).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build();
    manager.enqueueUniquePeriodicWork("correo_background",ExistingPeriodicWorkPolicy.UPDATE,req);
  }
  public static void syncNow(Context context){
    SharedPreferences p=context.getSharedPreferences("correoplus_settings",Context.MODE_PRIVATE);
    Constraints c=new Constraints.Builder()
      .setRequiredNetworkType(p.getBoolean("sync_wifi_only",false)?NetworkType.UNMETERED:NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(p.getBoolean("sync_battery_guard",true)).build();
    WorkManager.getInstance(context).enqueueUniqueWork("correo_manual_background",ExistingWorkPolicy.KEEP,
      new OneTimeWorkRequest.Builder(MailBackground.class).addTag("manual").setConstraints(c).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build());
  }
  private static void history(SharedPreferences p,String entry){
    String old=p.getString("background_history","");
    String stamp=new java.text.SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date());
    String value=stamp+" · "+entry+"\n"+old;
    String[] lines=value.split("\n");StringBuilder limited=new StringBuilder();
    for(int i=0;i<Math.min(12,lines.length);i++)limited.append(lines[i]).append('\n');
    p.edit().putString("background_history",limited.toString()).apply();
  }
  @NonNull @Override public Result doWork(){
    Context ctx=getApplicationContext();SharedPreferences p=ctx.getSharedPreferences("correoplus_settings",Context.MODE_PRIVATE);
    if(!p.getBoolean("background_sync",false)&&!getTags().contains("manual"))return Result.success();
    String account=p.getString("account",""),host=p.getString("imap_host",""),user=p.getString("username","");
    if(account.isEmpty()||host.isEmpty()||user.isEmpty())return Result.success();
    // Avoid two background connections running concurrently across worker instances.
    if(!RUNNING.compareAndSet(false,true))return Result.success();
    p.edit().putString("background_last_status","Conectando…").putLong("background_last_attempt",System.currentTimeMillis()).apply();
    Store store=null;Folder folder=null;
    try{
      String password=CredentialVault.load(ctx,account);
      if(password==null)return Result.success(); // Never store passwords in worker input or logs.
      int port=Integer.parseInt(p.getString("imap_port","993"));
      Properties props=new Properties();String prefix=port==993?"mail.imaps":"mail.imap";
      props.put(prefix+".connectiontimeout","15000");props.put(prefix+".timeout","15000");
      if(port==993)props.put(prefix+".ssl.enable","true");
      else{props.put(prefix+".starttls.enable","true");props.put(prefix+".starttls.required","true");}
      store=Session.getInstance(props).getStore(port==993?"imaps":"imap");store.connect(host,port,user,password);
      folder=store.getFolder("INBOX");folder.open(Folder.READ_ONLY);
      if(!(folder instanceof IMAPFolder))return Result.success();
      IMAPFolder imap=(IMAPFolder)folder;long validity=imap.getUIDValidity();
      MailCache cache=new MailCache(ctx);long previous=cache.maxUid(account,"INBOX",validity);
      int total=folder.getMessageCount();ArrayList<MailCache.Item> batch=new ArrayList<>();
      // UID ranges avoid scanning the same 100 headers on every poll.
      // A small recent window still reconciles read/unread changes made elsewhere.
      Message[] newMessages=previous>0?imap.getMessagesByUID(previous+1,UIDFolder.LASTUID):new Message[0];
      int recentStart=Math.max(1,total-24);
      java.util.LinkedHashMap<Long,Message> candidates=new java.util.LinkedHashMap<>();
      for(int i=recentStart;i<=total;i++){Message m=folder.getMessage(i);long id=imap.getUID(m);if(id>0)candidates.put(id,m);}
      if(newMessages!=null)for(Message m:newMessages){if(m!=null){long id=imap.getUID(m);if(id>0)candidates.put(id,m);}}
      int workTotal=candidates.size();
      p.edit().putInt("background_progress_total",workTotal).putInt("background_progress_done",0).putString("background_phase","Leyendo encabezados").apply();
      long newest=previous,latestUid=-1;String latestFrom="",latestSubject="";int fresh=0,updated=0;
      int processed=0;
      for(Message m:candidates.values()){
        if(isStopped())return Result.retry();
        if(processed++%10==0)p.edit().putInt("background_progress_done",processed).apply();
        long uid=imap.getUID(m);if(uid<=0)continue;
        MailCache.Item existing=cache.get(account,"INBOX",uid);
        if(existing==null){Date date=m.getSentDate();String from=m.getFrom()==null||m.getFrom().length==0?"Remitente":MimeUtility.decodeText(m.getFrom()[0].toString());
          String subject=m.getSubject()==null?"(sin asunto)":MimeUtility.decodeText(m.getSubject());
          batch.add(new MailCache.Item(uid,from,subject,date==null?"":date.toString(),m.isSet(Flags.Flag.SEEN),null,-1));
          if(uid>newest){newest=uid;latestUid=uid;latestFrom=from;latestSubject=subject;}
          if(previous>0&&uid>previous&&!m.isSet(Flags.Flag.SEEN))fresh++;
        }else {if(existing.seen!=m.isSet(Flags.Flag.SEEN))updated++;cache.updateSeen(account,"INBOX",validity,uid,m.isSet(Flags.Flag.SEEN));}
      }
      cache.saveHeaders(account,"INBOX",validity,batch);
      // Check read/unread flags for ALL cached messages, in bounded UID batches.
      // Missing UIDs are not deleted here: an incomplete response must not lose local mail.
      int removed=0,checked=0,offset=0;
      while(true){
        ArrayList<MailCache.Item> cached=cache.page(account,"INBOX",offset,100);
        if(cached.isEmpty())break;
        long[] ids=new long[cached.size()];
        for(int i=0;i<ids.length;i++)ids[i]=cached.get(i).uid;
        Message[] remote=imap.getMessagesByUID(ids);
        if(remote==null||remote.length!=ids.length)throw new Exception("Incomplete UID response");
        for(int i=0;i<remote.length;i++){
          if(isStopped())return Result.retry();
          if(remote[i]!=null&&!remote[i].isExpunged()){
            boolean seen=remote[i].isSet(Flags.Flag.SEEN);
            if(cached.get(i).seen!=seen){cache.updateSeen(account,"INBOX",validity,ids[i],seen);updated++;}
          }
          checked++;
        }
        offset+=cached.size();
        if(cached.size()<100)break;
      }
      p.edit().putInt("background_progress_done",workTotal).putString("background_phase","Completado").apply();
      history(p,"Correcto · "+batch.size()+" nuevos · "+updated+" actualizados · "+removed+" eliminados · "+workTotal+" comprobados");
      p.edit().putString("background_last_status","Correcto · "+total+" en servidor · "+batch.size()+" nuevos · "+updated+" estados actualizados · "+removed+" eliminados · "+workTotal+" comprobados").putLong("background_last_success",System.currentTimeMillis()).apply();
      p.edit().putLong("synced_"+account+"_INBOX",System.currentTimeMillis()).apply();
      if(fresh>0)notifyNew(ctx,fresh,latestFrom,latestSubject,latestUid,account);
      return Result.success();
    }catch(Exception ex){
      // Never persist exception messages: servers may include private data.
      history(p,"Fallo · "+ex.getClass().getSimpleName());
      p.edit().putString("background_phase","Esperando reintento").putString("background_last_status","Fallo: "+ex.getClass().getSimpleName()).putLong("background_last_failure",System.currentTimeMillis()).apply();
      return Result.retry();
    }
    finally{try{if(folder!=null&&folder.isOpen())folder.close(false);}catch(Exception ignored){}try{if(store!=null&&store.isConnected())store.close();}catch(Exception ignored){}RUNNING.set(false);}
  }
  private void notifyNew(Context ctx,int count,String from,String subject,long uid,String account){
    NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    if(manager==null)return;
    if(Build.VERSION.SDK_INT>=26){
      NotificationChannel channel=new NotificationChannel("new_mail","Correos nuevos",NotificationManager.IMPORTANCE_DEFAULT);
      channel.setDescription("Avisos de mensajes nuevos recibidos en CorreoPlus");
      manager.createNotificationChannel(channel);
    }
    if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
    Intent intent=new Intent(ctx,MainActivity.class);
    intent.setAction("com.example.correoplus.OPEN_NEW_MAIL");
    intent.putExtra("notification_account",account);
    intent.putExtra("notification_folder","INBOX");
    intent.putExtra("notification_uid",count==1?uid:-1L);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pending=PendingIntent.getActivity(ctx,903,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    String heading=count==1?from:"Correos nuevos: "+count;
    String detail=count==1?subject:"Toca para ver los mensajes nuevos";
    Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(ctx,"new_mail"):new Notification.Builder(ctx);
    Notification n=builder.setSmallIcon(android.R.drawable.ic_dialog_email)
      .setContentTitle(heading).setContentText(detail)
      .setStyle(new Notification.BigTextStyle().bigText(count==1?from+"\n"+subject:detail))
      .setNumber(count).setWhen(System.currentTimeMillis()).setShowWhen(true)
      .setAutoCancel(true).setContentIntent(pending).build();
    manager.notify(903,n);
  }
}
