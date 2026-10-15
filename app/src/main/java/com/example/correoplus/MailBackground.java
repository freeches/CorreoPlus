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
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import com.sun.mail.imap.IMAPFolder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
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
    PeriodicWorkRequest req=new PeriodicWorkRequest.Builder(MailBackground.class,15,TimeUnit.MINUTES).setConstraints(c).build();
    manager.enqueueUniquePeriodicWork("correo_background",ExistingPeriodicWorkPolicy.UPDATE,req);
  }
  @NonNull @Override public Result doWork(){
    Context ctx=getApplicationContext();SharedPreferences p=ctx.getSharedPreferences("correoplus_settings",Context.MODE_PRIVATE);
    if(!p.getBoolean("background_sync",false))return Result.success();
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
      long newest=previous;String latestFrom="",latestSubject="";int fresh=0,updated=0;
      for(int i=total;i>Math.max(0,total-100);i--){
        Message m=folder.getMessage(i);long uid=imap.getUID(m);if(uid<=0)continue;
        MailCache.Item existing=cache.get(account,"INBOX",uid);
        if(existing==null){Date date=m.getSentDate();String from=m.getFrom()==null||m.getFrom().length==0?"Remitente":MimeUtility.decodeText(m.getFrom()[0].toString());
          String subject=m.getSubject()==null?"(sin asunto)":MimeUtility.decodeText(m.getSubject());
          batch.add(new MailCache.Item(uid,from,subject,date==null?"":date.toString(),m.isSet(Flags.Flag.SEEN),null,-1));
          if(uid>newest){newest=uid;latestFrom=from;latestSubject=subject;}
          if(previous>0&&uid>previous&&!m.isSet(Flags.Flag.SEEN))fresh++;
        }else {if(existing.seen!=m.isSet(Flags.Flag.SEEN))updated++;cache.updateSeen(account,"INBOX",validity,uid,m.isSet(Flags.Flag.SEEN));}
      }
      cache.saveHeaders(account,"INBOX",validity,batch);
      p.edit().putString("background_last_status","Correcto · "+total+" en servidor · "+batch.size()+" nuevos · "+updated+" estados actualizados").putLong("background_last_success",System.currentTimeMillis()).apply();
      p.edit().putLong("synced_"+account+"_INBOX",System.currentTimeMillis()).apply();
      if(fresh>0)notifyNew(ctx,fresh,latestFrom,latestSubject);
      return Result.success();
    }catch(Exception ex){
      // Never persist exception messages: servers may include private data.
      p.edit().putString("background_last_status","Fallo: "+ex.getClass().getSimpleName()).putLong("background_last_failure",System.currentTimeMillis()).apply();
      return Result.retry();
    }
    finally{try{if(folder!=null&&folder.isOpen())folder.close(false);}catch(Exception ignored){}try{if(store!=null&&store.isConnected())store.close();}catch(Exception ignored){}RUNNING.set(false);}
  }
  private void notifyNew(Context ctx,int count,String from,String subject){
    NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel("new_mail","Correo nuevo",NotificationManager.IMPORTANCE_DEFAULT));
    if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
    Intent intent=new Intent(ctx,MainActivity.class);PendingIntent pending=PendingIntent.getActivity(ctx,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification n=new Notification.Builder(ctx,"new_mail").setSmallIcon(android.R.drawable.ic_dialog_email)
      .setContentTitle(count==1?"Correo nuevo":"Correos nuevos: "+count)
      .setContentText(count==1?from+" · "+subject:"Abre CorreoPlus para leerlos")
      .setAutoCancel(true).setContentIntent(pending).build();manager.notify(903,count==1?n:n);
  }
}
