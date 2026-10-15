package com.example.correoplus;

import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Html;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.database.Cursor;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.sun.mail.imap.IMAPFolder;
import android.text.InputType;
import android.view.View;
import android.view.MotionEvent;
import android.widget.*;
import java.util.*;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

public class MainActivity extends Activity {
  final Handler ui=new Handler(Looper.getMainLooper());
  MailCache cache; String lastAccount=""; SharedPreferences prefs;
  LinearLayout root, content, toolbar; EditText host, port, user, pass, smtpHost, smtpPort;
  String h,u,pw,sh; int imapPort,sPort; boolean configured=false; boolean rememberPassword=false; String folderName="INBOX"; int inboxFilter=0; int pageOffset=0; String advancedSender="", advancedSubject="", advancedAfter=""; boolean advancedAttachments=false;
  final int navy=Color.rgb(17,43,74), blue=Color.rgb(0,105,185);
  boolean darkMode=false;
  final LinkedHashSet<Long> selectedUids=new LinkedHashSet<>();
  boolean selectionMode=false;
  static final int PICK_ATTACHMENT=701, SAVE_ATTACHMENT=702;
  final ArrayList<Uri> outgoingAttachments=new ArrayList<>();
  byte[] pendingDownload; String pendingFilename;
  LinearLayout attachmentPreview;
  LinearLayout receivedAttachments;
  String sizeKB(long size){return size<0?"tamaño no disponible":String.format(java.util.Locale.getDefault(),"%.1f KB",size/1024.0);}
  String mimeFor(String filename){String ext=MimeTypeMap.getFileExtensionFromUrl(filename);if(ext==null||ext.isEmpty()){int dot=filename.lastIndexOf('.');ext=dot>=0?filename.substring(dot+1):"";}String type=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(java.util.Locale.ROOT));return type==null?"application/octet-stream":type;}
  static class AttachmentInfo {String name; int index; long size; AttachmentInfo(String n,int i,long z){name=n;index=i;size=z;}}
  String safeName(String name){if(name==null||name.trim().isEmpty())return "adjunto";return name.replace('\\','_').replace('/','_').replace('\n','_').replace('\r','_');}
  void collectAttachments(Part part,ArrayList<AttachmentInfo> result,int depth)throws Exception{
    if(depth>12)return;
    if(part.isMimeType("multipart/*")){
      Object content=part.getContent();if(content instanceof Multipart){Multipart mp=(Multipart)content;
        for(int i=0;i<mp.getCount();i++)collectAttachments(mp.getBodyPart(i),result,depth+1);}
      return;
    }
    String disposition=part.getDisposition(),filename=part.getFileName();
    if(filename!=null || Part.ATTACHMENT.equalsIgnoreCase(disposition)){
      result.add(new AttachmentInfo(safeName(decode(filename)),result.size(),part.getSize()));
    }
  }
  Part attachmentByIndex(Part part,int[] count,int wanted,int depth)throws Exception{
    if(depth>12)return null;
    if(part.isMimeType("multipart/*")){
      Object c=part.getContent();if(c instanceof Multipart){Multipart mp=(Multipart)c;
        for(int i=0;i<mp.getCount();i++){Part found=attachmentByIndex(mp.getBodyPart(i),count,wanted,depth+1);if(found!=null)return found;}}
      return null;
    }
    if(part.getFileName()!=null || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())){
      if(count[0]++==wanted)return part;
    }
    return null;
  }
  void loadAttachments(String folder,long uid){
    if(receivedAttachments==null)return;
    LinearLayout target=receivedAttachments;
    target.removeAllViews();target.addView(text("Cargando adjuntos…",13));
    work(()->{Store st=connect();try{Folder f=st.getFolder(folder);try{f.open(Folder.READ_ONLY);
      Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;
      if(m==null)throw new Exception("El mensaje ya no está disponible en el servidor");
      ArrayList<AttachmentInfo> attachments=new ArrayList<>();collectAttachments(m,attachments,0);cache.updateAttachments(lastAccount,folder,uid,!attachments.isEmpty());
      show(()->{if(receivedAttachments!=target)return;target.removeAllViews();if(!attachments.isEmpty()){
        target.addView(text("📎 Adjuntos ("+attachments.size()+")",18));
        for(AttachmentInfo a:attachments){target.addView(button("📄 "+a.name+" · "+sizeKB(a.size)+"   Abrir",()->openAttachment(folder,uid,a.index,a.name)));
          target.addView(button("↓ Guardar "+a.name,()->downloadAttachment(folder,uid,a.index,a.name)));}
      }});
    }finally{if(f.isOpen())f.close(false);}}finally{st.close();}});
  }
  byte[] getAttachmentBytes(String folder,long uid,int index)throws Exception{
    Store st=connect();try{Folder f=st.getFolder(folder);try{f.open(Folder.READ_ONLY);
      Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;
      if(m==null)throw new Exception("Mensaje no disponible");
      Part part=attachmentByIndex(m,new int[]{0},index,0);if(part==null)throw new Exception("Adjunto no encontrado");
      ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=part.getInputStream()){
        byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>25*1024*1024)throw new Exception("El adjunto supera el límite de 25 MB");out.write(buffer,0,n);}}
      return out.toByteArray();
    }finally{if(f.isOpen())f.close(false);}}finally{st.close();}
  }
  void openAttachment(String folder,long uid,int index,String filename){
    work(()->{byte[] bytes=getAttachmentBytes(folder,uid,index);
      File dir=new File(getCacheDir(),"shared_attachments");if(!dir.exists()&&!dir.mkdirs())throw new Exception("No se pudo preparar el archivo temporal");
      File file=new File(dir,java.util.UUID.randomUUID().toString()+"_"+safeName(filename));
      try(FileOutputStream output=new FileOutputStream(file)){output.write(bytes);}
      Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
      show(()->{Intent intent=new Intent(Intent.ACTION_VIEW);intent.setDataAndType(uri,mimeFor(filename));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivity(Intent.createChooser(intent,"Abrir adjunto"));}catch(ActivityNotFoundException e){Toast.makeText(this,"No hay una aplicación para abrir este tipo de archivo",Toast.LENGTH_LONG).show();}});
    });
  }
  void downloadAttachment(String folder,long uid,int index,String filename){
    work(()->{byte[] bytes=getAttachmentBytes(folder,uid,index);show(()->{pendingDownload=bytes;pendingFilename=filename;
      Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.setType(mimeFor(filename));intent.addCategory(Intent.CATEGORY_OPENABLE);intent.putExtra(Intent.EXTRA_TITLE,filename);startActivityForResult(intent,SAVE_ATTACHMENT);});});
  }
  String attachmentName(Uri uri){try(android.database.Cursor c=getContentResolver().query(uri,null,null,null,null)){
    if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return safeName(c.getString(i));}}
    catch(Exception ignored){}return "archivo";
  }
  void updateAttachmentPreview(){if(attachmentPreview==null)return;attachmentPreview.removeAllViews();
    for(Uri uri:new ArrayList<>(outgoingAttachments))attachmentPreview.addView(button("✕ "+attachmentName(uri),()->{outgoingAttachments.remove(uri);updateAttachmentPreview();}));
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
    if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
    if(request==PICK_ATTACHMENT){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
      outgoingAttachments.add(uri);updateAttachmentPreview();}
    else if(request==SAVE_ATTACHMENT && pendingDownload!=null){byte[] bytes=pendingDownload;pendingDownload=null;
      work(()->{try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new Exception("No se puede guardar");out.write(bytes);}
        show(()->Toast.makeText(this,"Adjunto guardado",Toast.LENGTH_LONG).show());});}
  }

  final java.util.concurrent.atomic.AtomicBoolean syncing=new java.util.concurrent.atomic.AtomicBoolean(false);
  int foreground(){return darkMode?Color.WHITE:navy;}
  int background(){return darkMode?Color.rgb(24,30,39):Color.WHITE;}
  interface Task { void run() throws Exception; }
  void work(Task task){new Thread(()->{try{task.run();}catch(Exception e){error(e.getClass().getSimpleName()+": "+e.getMessage());}}).start();}
  void show(Runnable action){ui.post(action);} void error(String s){show(()->new AlertDialog.Builder(this).setTitle("CorreoPlus").setMessage(s).setPositiveButton("Aceptar",null).show());}
  TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(foreground());t.setPadding(12,13,12,13);return t;}
  Button button(String name,Runnable run){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setOnClickListener(v->run.run());return b;}
  EditText input(LinearLayout target,String hint,String initial,boolean secret){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setText(initial);if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);target.addView(e);return e;}
  void layout(String heading){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(background());setContentView(root);TextView title=text("✉  CorreoPlus    "+heading,22);title.setTextColor(Color.WHITE);title.setBackgroundColor(navy);root.addView(title);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(14,12,14,12);sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));}
  @Override public void onCreate(Bundle b){super.onCreate(b); MailBackground.schedule(this);cache=new MailCache(this);prefs=getSharedPreferences("correoplus_settings",MODE_PRIVATE);darkMode=prefs.getBoolean("dark_mode",false);lastAccount=prefs.getString("account","");if(lastAccount.isEmpty())settings();else {
    h=prefs.getString("imap_host",""); u=prefs.getString("username","");
    sh=prefs.getString("smtp_host","");
    try { imapPort=Integer.parseInt(prefs.getString("imap_port","993")); sPort=Integer.parseInt(prefs.getString("smtp_port","587")); }
    catch(NumberFormatException e){imapPort=993;sPort=587;}
    try { pw=CredentialVault.load(this,lastAccount); configured=pw!=null && !h.isEmpty() && !u.isEmpty(); }
    catch(Exception e){configured=false;pw=null;}
    renderCached("INBOX");
    if(configured) openFolder("INBOX");
  }}
  void settings(){layout("Cuentas");
  CheckBox notices=new CheckBox(this);notices.setText("Notificaciones y sincronización periódica (mínimo 15 minutos)");notices.setTextColor(foreground());notices.setChecked(prefs.getBoolean("background_sync",false));content.addView(notices);notices.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("background_sync",enabled).apply();MailBackground.schedule(this);});
  content.addView(text("Preferencias",19));
  CheckBox dark=new CheckBox(this);dark.setText("Modo oscuro");dark.setTextColor(foreground());dark.setChecked(darkMode);
  content.addView(dark);dark.setOnCheckedChangeListener((v,checked)->{prefs.edit().putBoolean("dark_mode",checked).apply();darkMode=checked;settings();});
  CheckBox defaultHtml=new CheckBox(this);
  defaultHtml.setText("Ver mensajes en HTML por defecto (desmarcado: texto plano)");
  defaultHtml.setTextColor(foreground());
  defaultHtml.setChecked(prefs.getBoolean("default_html",false));
  content.addView(defaultHtml);
  defaultHtml.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean("default_html",checked).apply());
  content.addView(text("Firma automática (se añade al enviar)",14));
  EditText signature=input(content,"Tu firma",prefs.getString("signature",""),false);
  content.addView(button("Guardar firma",()->{prefs.edit().putString("signature",signature.getText().toString()).apply();error("Firma guardada");}));content.addView(text("Correo IMAP / SMTP · Conexión cifrada",17));content.addView(text("Gmail y Outlook con OAuth2 todavía no disponibles. Puedes optar por guardar la contraseña cifrada con Android Keystore. Gmail y Outlook requieren OAuth2 y todavía no están integrados.",13));host=input(content,"Servidor IMAP",prefs.getString("imap_host",""),false);port=input(content,"Puerto IMAP 993 o 143",prefs.getString("imap_port","993"),false);user=input(content,"Correo / usuario",prefs.getString("username",""),false);pass=input(content,"Contraseña IMAP", "",true);
  CheckBox remember=new CheckBox(this);remember.setText("Recordar contraseña cifrada en este dispositivo");content.addView(remember);
  content.addView(button("Borrar contraseña guardada",()->{CredentialVault.clear(this);pw=null;configured=false;error("Credenciales guardadas eliminadas");}));smtpHost=input(content,"Servidor SMTP",prefs.getString("smtp_host",""),false);smtpPort=input(content,"Puerto SMTP 465 o 587",prefs.getString("smtp_port","587"),false);content.addView(button("Conectar y abrir bandeja",()->{try{h=host.getText().toString().trim();u=user.getText().toString().trim();pw=pass.getText().toString();sh=smtpHost.getText().toString().trim();imapPort=Integer.parseInt(port.getText().toString().trim());sPort=Integer.parseInt(smtpPort.getText().toString().trim());if(h.isEmpty()||u.isEmpty()||pw.isEmpty()||!(imapPort==993||imapPort==143)||!(sPort==465||sPort==587))throw new Exception("Revisa servidor, usuario, contraseña y puertos admitidos");rememberPassword=remember.isChecked();configured=true;lastAccount=u.toLowerCase(Locale.ROOT)+"@"+h.toLowerCase(Locale.ROOT);if(rememberPassword){CredentialVault.save(this,lastAccount,pw);}else{CredentialVault.clear(this);}prefs.edit().putString("account",lastAccount).putString("imap_host",h).putString("imap_port",String.valueOf(imapPort)).putString("username",u).putString("smtp_host",sh).putString("smtp_port",String.valueOf(sPort)).apply();openFolder("INBOX");}catch(Exception e){error(e.getMessage());}}));}
  Properties imapProperties(){Properties props=new Properties();props.put("mail.store.protocol",imapPort==993?"imaps":"imap");String prefix=imapPort==993?"mail.imaps":"mail.imap";props.put(prefix+".connectiontimeout","15000");props.put(prefix+".timeout","45000");if(imapPort==993)props.put("mail.imaps.ssl.enable","true");else{props.put("mail.imap.starttls.enable","true");props.put("mail.imap.starttls.required","true");}return props;}
  Store connect()throws Exception{Session s=Session.getInstance(imapProperties());Store st=s.getStore(imapPort==993?"imaps":"imap");st.connect(h,imapPort,u,pw);return st;}
  String decode(String s){if(s==null)return "(sin asunto)";try{return MimeUtility.decodeText(s);}catch(Exception e){return s;}}
  String sender(Message m)throws Exception{Address[] a=m.getFrom();return a==null||a.length==0?"Remitente desconocido":decode(a[0].toString());}
  void menu(){layout("Correo");content.addView(button("☰ Carpetas",()->folders()));content.addView(button("⚙ Cuenta",()->settings()));content.addView(button("✎ Redactar",()->compose("","","")));}
  boolean temporaryImapFailure(Throwable e){
    for(Throwable t=e;t!=null;t=t.getCause()){
      if(t instanceof java.net.SocketException || t instanceof java.net.SocketTimeoutException ||
         t instanceof java.net.ConnectException || t instanceof java.net.UnknownHostException ||
         t instanceof java.io.EOFException || t instanceof javax.mail.FolderClosedException ||
         t instanceof javax.mail.StoreClosedException)return true;
      String msg=t.getMessage();
      if(msg!=null){String lower=msg.toLowerCase(Locale.ROOT);
        if(lower.contains("connection reset") || lower.contains("connection abort") ||
           lower.contains("connection refused") || lower.contains("timed out") ||
           lower.contains("broken pipe") || lower.contains("* bye") ||
           lower.contains("failed to connect") || lower.contains("couldn't connect"))return true;
      }
    }
    return false;
  }
  void waitBeforeRetry(int attempt)throws InterruptedException{
    Thread.sleep(attempt==0?1500L:3000L);
  }
  void syncOnce(String folder)throws Exception{
    Store st=connect();
    try{
      Folder f=st.getFolder(folder);
      try{
        f.open(Folder.READ_ONLY);
        int n=f.getMessageCount();
        long validity=f instanceof IMAPFolder?((IMAPFolder)f).getUIDValidity():0;
        long lastUid=cache.maxUid(lastAccount,folder,validity);
        // Initial synchronization fetches just the newest 50 headers. Later syncs
        // extend the cached window by 50, without downloading MIME attachments.
        int cachedCount=cache.count(lastAccount,folder);
        int window=Math.min(n,Math.max(50,cachedCount+50));
        int lower=Math.max(0,n-window);
        ArrayList<MailCache.Item> batch=new ArrayList<>();
        for(int i=n;i>lower;i--){
          Message m=f.getMessage(i);
          long uid=f instanceof IMAPFolder?((IMAPFolder)f).getUID(m):i;
          MailCache.Item existing=cache.get(lastAccount,folder,uid);
          if(existing!=null){
            // Do not request flags/content for already cached messages during bulk sync.
            continue;
          }
          boolean seen=m.isSet(Flags.Flag.SEEN);
          Date d=m.getSentDate();
          // Attachment detection requires MIME body fetch and is deferred until open.
          batch.add(new MailCache.Item(uid,sender(m),decode(m.getSubject()),
              d==null?"":d.toString(),seen,null,-1));
          if(batch.size()>=25){
            cache.saveHeaders(lastAccount,folder,validity,batch);
            batch.clear();
          }
        }
        if(!batch.isEmpty())cache.saveHeaders(lastAccount,folder,validity,batch);
        prefs.edit().putLong("synced_"+lastAccount+"_"+folder,System.currentTimeMillis()).apply();
      }finally{try{if(f.isOpen())f.close(false);}catch(Exception ignored){}}
    }finally{try{if(st.isConnected())st.close();}catch(Exception ignored){}}
  }
  void openFolder(String folder){
    if(!folder.equals(folderName))pageOffset=0;
    folderName=folder;renderCached(folder);
    if(!configured){content.addView(text("Sin sesión activa · Abre Cuenta para conectar; la caché sigue disponible",14));return;}
    if(!syncing.compareAndSet(false,true)){
      content.addView(text("Ya hay una sincronización en curso…",14));return;
    }
    content.addView(text("Sincronizando cabeceras en bloques de 25 (hasta 50 correos nuevos por vez)…",14));
    work(()->{
      try{
        for(int attempt=0;attempt<3;attempt++){
          try{
            // syncOnce always creates and closes its own Store and Folder.
            syncOnce(folder);
            show(()->{if(folder.equals(folderName)){renderCached(folder);content.addView(text("Sincronización por bloques completada. Pulsa Sincronizar otra vez para descargar correos más antiguos.",13));}});
            return;
          }catch(Exception ex){
            if(!temporaryImapFailure(ex))throw ex;
            if(attempt<2){
              try{waitBeforeRetry(attempt);}
              catch(InterruptedException ie){Thread.currentThread().interrupt();return;}
            }
          }
        }
        show(()->{if(folder.equals(folderName)){
          renderCached(folder);
          content.addView(text("La conexión IMAP se interrumpió tras 3 intentos. Los bloques descargados siguen guardados. Pulsa Sincronizar para continuar.",14));
        }});
      }finally{syncing.set(false);}
    });
  }

  void renderCached(String folder){if(!folder.equals(folderName)){selectionMode=false;selectedUids.clear();}folderName=folder;menu();ArrayList<MailCache.Item> rows=cache.page(lastAccount,folder,pageOffset,100);content.addView(text(folder+" · "+cache.count(lastAccount,folder)+" mensajes guardados",19));
    long last=prefs.getLong("synced_"+lastAccount+"_"+folder,0);
    content.addView(text(last==0?"Aún no sincronizado":"Última sincronización: "+new java.text.SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date(last)),12));
    LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
    content.addView(filters);
    filters.addView(button("Todos",()->{inboxFilter=0;renderCached(folder);}));
    filters.addView(button("No leídos",()->{inboxFilter=1;renderCached(folder);}));
    filters.addView(button("★ Favoritos",()->{inboxFilter=2;renderCached(folder);}));
    filters.addView(button("📎 Adjuntos",()->{inboxFilter=3;renderCached(folder);}));
    content.addView(text("Filtro: "+(inboxFilter==1?"No leídos":inboxFilter==2?"Favoritos":inboxFilter==3?"Con adjuntos":"Todos"),12));
    EditText search=input(content,"Buscar remitente, asunto o contenido (caché)","",false);
    LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.addView(results);
    search.addTextChangedListener(new android.text.TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){renderSearch(results,folder,s.toString());}
      public void afterTextChanged(android.text.Editable e){}
    });
    renderSearch(results,folder,"");content.addView(button("🔎 Búsqueda avanzada",()->advancedSearchScreen(folder)));content.addView(button("☑ Seleccionar varios",()->{selectionMode=true;selectedUids.clear();renderSearch(results,folder,search.getText().toString());}));content.addView(button("↻ Sincronizar",()->{if(configured)openFolder(folder);else settings();}));
    content.addView(button("← 100 anteriores",()->{pageOffset=Math.max(0,pageOffset-100);renderCached(folder);}));
    content.addView(button("100 siguientes →",()->{if(pageOffset+100<cache.count(lastAccount,folder)){pageOffset+=100;renderCached(folder);}}));/* legacy listing removed */for(MailCache.Item r:new ArrayList<MailCache.Item>()){TextView item=text((r.seen?"":"● ")+r.from+"\n"+r.subject+"\n"+r.date,15);item.setBackgroundColor(darkMode?Color.rgb(45,55,69):Color.rgb(245,248,252));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;content.addView(item,lp);item.setOnClickListener(v->openCachedMessage(folder,r.uid));}}
  boolean isFavorite(String folder,long uid){
    return prefs.getBoolean("fav_"+lastAccount+"_"+folder+"_"+uid,false);
  }
  void toggleFavorite(String folder,long uid){
    String key="fav_"+lastAccount+"_"+folder+"_"+uid;
    prefs.edit().putBoolean(key,!prefs.getBoolean(key,false)).apply();
    renderCached(folder);
  }
  String senderDisplay(String raw){
    if(raw==null||raw.trim().isEmpty())return "Remitente desconocido";
    String value=raw.trim();int lt=value.indexOf('<');
    if(lt>0){String name=value.substring(0,lt).trim().replace("\"","");if(!name.isEmpty())return name;}
    return value;
  }
  String relativeMailDate(String raw){
    if(raw==null||raw.isEmpty())return "";
    try{
      java.text.SimpleDateFormat parser=new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",java.util.Locale.ENGLISH);
      parser.setLenient(true);Date d=parser.parse(raw);if(d==null)return raw;
      Calendar now=Calendar.getInstance(),mail=Calendar.getInstance();mail.setTime(d);
      java.text.SimpleDateFormat format;
      if(now.get(Calendar.YEAR)==mail.get(Calendar.YEAR)&&now.get(Calendar.DAY_OF_YEAR)==mail.get(Calendar.DAY_OF_YEAR))format=new java.text.SimpleDateFormat("HH:mm",Locale.getDefault());
      else {Calendar week=Calendar.getInstance();week.add(Calendar.DAY_OF_YEAR,-6);
        if(!mail.before(week)&&!mail.after(now))format=new java.text.SimpleDateFormat("EEEE",Locale.getDefault());
        else format=new java.text.SimpleDateFormat("dd/MM/yyyy",Locale.getDefault());}
      return format.format(d);
    }catch(Exception ex){return raw;}
  }
  void renderSearch(LinearLayout target,String folder,String query){
    target.removeAllViews();
    ArrayList<MailCache.Item> rows=query.trim().isEmpty()?cache.page(lastAccount,folder,pageOffset,100):cache.search(lastAccount,folder,query);
    if(selectionMode){
      LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);target.addView(actions);
      actions.addView(text(selectedUids.size()+" seleccionados",17));
      actions.addView(button("✓ Seleccionar todos los visibles",()->{for(MailCache.Item r:rows)selectedUids.add(r.uid);renderSearch(target,folder,query);}));
      actions.addView(button("✉ Marcar leídos",()->setSeenBatch(folder,true,new ArrayList<>(selectedUids))));
      actions.addView(button("✉ Marcar no leídos",()->setSeenBatch(folder,false,new ArrayList<>(selectedUids))));
      actions.addView(button("📁 Mover a carpeta",()->chooseMoveFolder(folder,new ArrayList<>(selectedUids))));
      actions.addView(button("▣ Archivar seleccionados",()->confirmBatchMove(folder,false)));
      actions.addView(button("🗑 Eliminar seleccionados",()->confirmBatchMove(folder,true)));
      actions.addView(button("✕ Cancelar selección",()->{selectionMode=false;selectedUids.clear();renderSearch(target,folder,query);}));
    }
    for(MailCache.Item r:rows){
      if(inboxFilter==1 && r.seen)continue;
      if(inboxFilter==2 && !isFavorite(folder,r.uid))continue;
      if(inboxFilter==3 && r.attachmentState!=1)continue;
      LinearLayout item=new LinearLayout(this);
      item.setOrientation(LinearLayout.VERTICAL);
      item.setPadding(18,14,18,14);
      item.setBackgroundColor(selectedUids.contains(r.uid)?(darkMode?Color.rgb(52,78,110):Color.rgb(211,232,249)):(darkMode?Color.rgb(45,55,69):Color.rgb(245,248,252)));
      String senderLabel=senderDisplay(r.from);
      String prefix=selectionMode?(selectedUids.contains(r.uid)?"☑ ":"☐ "):"";
      TextView senderView=text(prefix+(r.seen?"":"● ")+senderLabel,16);
      senderView.setSingleLine(true);senderView.setEllipsize(TextUtils.TruncateAt.END);
      senderView.setTypeface(null,r.seen?Typeface.NORMAL:Typeface.BOLD);
      item.addView(senderView);
      LinearLayout subjectRow=new LinearLayout(this);subjectRow.setOrientation(LinearLayout.HORIZONTAL);
      TextView subjectView=text((isFavorite(folder,r.uid)?"★ ":"")+(r.attachmentState==1?"📎 ":"")+(r.subject==null?"(Sin asunto)":r.subject),14);
      subjectView.setSingleLine(true);subjectView.setEllipsize(TextUtils.TruncateAt.END);
      subjectRow.addView(subjectView,new LinearLayout.LayoutParams(0,-2,1));
      TextView when=text(relativeMailDate(r.date),12);
      when.setPadding(12,0,0,0);subjectRow.addView(when);
      item.addView(subjectRow);
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;
      target.addView(item,lp);
      // Consume DOWN: otherwise Android triggers the native long-click before UP,
      // incorrectly entering selection mode while a swipe is in progress.
      final float[] down={0,0};
      final boolean[] swiping={false}, longPressed={false};
      final Runnable longPress=()->{if(!swiping[0]&&!selectionMode){longPressed[0]=true;selectionMode=true;selectedUids.add(r.uid);renderSearch(target,folder,query);}};
      item.setOnTouchListener((v,event)->{
        int action=event.getActionMasked();
        float threshold=65*getResources().getDisplayMetrics().density;
        if(action==MotionEvent.ACTION_DOWN){down[0]=event.getX();down[1]=event.getY();swiping[0]=false;longPressed[0]=false;ui.postDelayed(longPress,android.view.ViewConfiguration.getLongPressTimeout());return true;}
        if(action==MotionEvent.ACTION_MOVE){float dx=event.getX()-down[0],dy=event.getY()-down[1];
          if(Math.abs(dx)>20*getResources().getDisplayMetrics().density||Math.abs(dy)>20*getResources().getDisplayMetrics().density)ui.removeCallbacks(longPress);
          if(Math.abs(dx)>threshold&&Math.abs(dx)>Math.abs(dy)*1.3f)swiping[0]=true;
          return swiping[0];}
        if(action==MotionEvent.ACTION_CANCEL){ui.removeCallbacks(longPress);return true;}
        if(action==MotionEvent.ACTION_UP){ui.removeCallbacks(longPress);if(longPressed[0])return true;
          float dx=event.getX()-down[0],dy=event.getY()-down[1];
          if(!selectionMode&&Math.abs(dx)>threshold&&Math.abs(dx)>Math.abs(dy)*1.3f){confirmMove(folder,r.uid,dx<0);return true;}
          if(Math.abs(dx)<20*getResources().getDisplayMetrics().density&&Math.abs(dy)<20*getResources().getDisplayMetrics().density){
            if(selectionMode){if(!selectedUids.add(r.uid))selectedUids.remove(r.uid);renderSearch(target,folder,query);}else openCachedMessage(folder,r.uid);
          }return true;
        }return true;
      });
    }
  }
  void advancedSearchScreen(String folder){
    menu();content.addView(text("Búsqueda avanzada · "+folder,20));
    EditText senderField=input(content,"Remitente contiene",advancedSender,false);
    EditText subjectField=input(content,"Asunto contiene",advancedSubject,false);
    EditText dateField=input(content,"Desde fecha (AAAA-MM-DD)",advancedAfter,false);
    CheckBox attachments=new CheckBox(this);attachments.setText("Solo mensajes con adjuntos");attachments.setChecked(advancedAttachments);content.addView(attachments);
    LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.addView(results);
    content.addView(button("Buscar",()->{
      advancedSender=senderField.getText().toString().trim();advancedSubject=subjectField.getText().toString().trim();advancedAfter=dateField.getText().toString().trim();advancedAttachments=attachments.isChecked();pageOffset=0;
      showAdvancedResults(folder,results);
    }));
    content.addView(button("Volver a bandeja",()->renderCached(folder)));
  }
  void showAdvancedResults(String folder,LinearLayout results){
    results.removeAllViews();ArrayList<MailCache.Item> found=cache.advancedSearch(lastAccount,folder,advancedSender,advancedSubject,advancedAfter,advancedAttachments,pageOffset);
    results.addView(text("Resultados: "+found.size()+" en esta página",15));
    for(MailCache.Item item:found)results.addView(button(senderDisplay(item.from)+" · "+item.subject+" · "+relativeMailDate(item.date),()->openCachedMessage(folder,item.uid)));
    if(pageOffset>0)results.addView(button("← Anteriores",()->{pageOffset=Math.max(0,pageOffset-100);showAdvancedResults(folder,results);}));
    if(found.size()==100)results.addView(button("Más resultados →",()->{pageOffset+=100;showAdvancedResults(folder,results);}));
  }
  void chooseMoveFolder(String folder,ArrayList<Long> ids){
    if(ids.isEmpty()){error("Selecciona algún mensaje");return;}
    work(()->{Store st=connect();try{ArrayList<String> names=new ArrayList<>();for(Folder f:st.getDefaultFolder().list("*"))if((f.getType()&Folder.HOLDS_MESSAGES)!=0&&!f.getFullName().equalsIgnoreCase(folder))names.add(f.getFullName());
      show(()->new AlertDialog.Builder(this).setTitle("Mover a carpeta").setItems(names.toArray(new String[0]),(dialog,which)->new AlertDialog.Builder(this).setTitle("Confirmar movimiento").setMessage("Mover "+ids.size()+" mensajes a "+names.get(which)+"?").setNegativeButton("Cancelar",null).setPositiveButton("Mover",(d,w)->moveToFolder(folder,ids,names.get(which))).show()).show());
    }finally{st.close();}});
  }
  void moveToFolder(String folder,ArrayList<Long> ids,String destinationName){
    work(()->{Store st=connect();try{Folder source=st.getFolder(folder),destination=st.getFolder(destinationName);
      if(!destination.exists())throw new Exception("La carpeta no existe");source.open(Folder.READ_WRITE);try{
        ArrayList<Message> messages=new ArrayList<>();ArrayList<Long> valid=new ArrayList<>();for(long uid:ids){Message m=source instanceof IMAPFolder?((IMAPFolder)source).getMessageByUID(uid):null;if(m!=null){messages.add(m);valid.add(uid);}}
        if(!messages.isEmpty()){Message[] batch=messages.toArray(new Message[0]);source.copyMessages(batch,destination);for(Message m:batch)m.setFlag(Flags.Flag.DELETED,true);source.close(true);for(long uid:valid)cache.remove(lastAccount,folder,uid);}
        show(()->{selectionMode=false;selectedUids.clear();renderCached(folder);Toast.makeText(this,"Movidos: "+valid.size(),Toast.LENGTH_SHORT).show();});
      }finally{if(source.isOpen())source.close(false);}
    }finally{st.close();}});
  }
  void setSeenBatch(String folder,boolean seen,ArrayList<Long> ids){
    if(ids.isEmpty()){Toast.makeText(this,"Selecciona algún correo",Toast.LENGTH_SHORT).show();return;}
    work(()->{Store st=connect();try{Folder f=st.getFolder(folder);f.open(Folder.READ_WRITE);try{
      long validity=f instanceof IMAPFolder?((IMAPFolder)f).getUIDValidity():-1;
      for(long uid:ids){Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;
        if(m!=null){m.setFlag(Flags.Flag.SEEN,seen);cache.updateSeen(lastAccount,folder,validity,uid,seen);}}
    }finally{f.close(false);}}finally{st.close();}
    show(()->{selectionMode=false;selectedUids.clear();renderCached(folder);});});
  }
  void confirmBatchMove(String folder,boolean delete){
    ArrayList<Long> ids=new ArrayList<>(selectedUids);
    if(ids.isEmpty()){Toast.makeText(this,"Selecciona algún correo",Toast.LENGTH_SHORT).show();return;}
    new AlertDialog.Builder(this).setTitle(delete?"Eliminar seleccionados":"Archivar seleccionados")
      .setMessage("Se moverán "+ids.size()+" mensajes en el servidor. ¿Continuar?")
      .setNegativeButton("Cancelar",null).setPositiveButton("Continuar",(d,w)->moveBatch(folder,ids,delete)).show();
  }
  void moveBatch(String folder,ArrayList<Long> ids,boolean delete){
    work(()->{Store st=connect();try{Folder source=st.getFolder(folder);source.open(Folder.READ_WRITE);try{
      String[] targets=delete?new String[]{"Trash","INBOX.Trash","Deleted Items","Papelera"}:new String[]{"Archive","INBOX.Archive","Archives","Archivo"};
      Folder destination=null;for(String name:targets){Folder candidate=st.getFolder(name);if(candidate.exists()){destination=candidate;break;}}
      if(destination==null)throw new Exception("No se encontró la carpeta de destino");
      ArrayList<Message> messages=new ArrayList<>();ArrayList<Long> validIds=new ArrayList<>();
      for(long uid:ids){Message m=source instanceof IMAPFolder?((IMAPFolder)source).getMessageByUID(uid):null;if(m!=null){messages.add(m);validIds.add(uid);}}
      if(!messages.isEmpty()){Message[] batch=messages.toArray(new Message[0]);source.copyMessages(batch,destination);
        for(Message m:batch)m.setFlag(Flags.Flag.DELETED,true);
        source.close(true);for(long uid:validIds)cache.remove(lastAccount,folder,uid);
      }
      show(()->{selectionMode=false;selectedUids.clear();renderCached(folder);Toast.makeText(this,"Mensajes movidos: "+validIds.size(),Toast.LENGTH_SHORT).show();});
    }finally{if(source.isOpen())source.close(false);}}finally{st.close();}});
  }
  String htmlBody(Part part,int depth)throws Exception{
    if(depth>10)return "";
    if(part.isMimeType("text/html")){Object c=part.getContent();return c==null?"":c.toString();}
    Object c=part.getContent();
    if(c instanceof Multipart){Multipart mp=(Multipart)c;
      for(int i=0;i<mp.getCount();i++){Part child=mp.getBodyPart(i);
        if(Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition())||child.getFileName()!=null)continue;
        String found=htmlBody(child,depth+1);if(!found.isEmpty())return found;
      }
    }
    return "";
  }
  // Keep the message header, expandable recipients/attachments and fixed action bar.
  // Only the body region is replaced when switching between plain text and HTML.
  LinearLayout messageBodySlot;
  String displayedFolder=""; long displayedUid=-1;
  boolean htmlVisible=false;
  void showHtmlMessage(String folder,long uid){
    if(!configured){Toast.makeText(this,"Conéctate para ver el HTML",Toast.LENGTH_SHORT).show();return;}
    if(messageBodySlot==null||!folder.equals(displayedFolder)||uid!=displayedUid){openCachedMessage(folder,uid);}
    final LinearLayout slot=messageBodySlot;
    if(slot==null)return;
    Toast.makeText(this,"Cargando HTML…",Toast.LENGTH_SHORT).show();
    work(()->{Store st=connect();try{Folder f=st.getFolder(folder);f.open(Folder.READ_ONLY);
      try{Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;
        if(m==null)throw new Exception("Mensaje no disponible");
        String html=htmlBody(m,0);
        show(()->{
          if(slot!=messageBodySlot||!folder.equals(displayedFolder)||uid!=displayedUid)return;
          if(html.isEmpty()){Toast.makeText(this,"Este correo no tiene versión HTML",Toast.LENGTH_LONG).show();return;}
          slot.removeAllViews();
          WebView view=new WebView(this);
          view.getSettings().setJavaScriptEnabled(false);
          view.getSettings().setBlockNetworkLoads(true);
          view.getSettings().setAllowFileAccess(false);
          view.getSettings().setAllowContentAccess(false);
          view.getSettings().setUseWideViewPort(true);
          view.getSettings().setLoadWithOverviewMode(true);
          view.getSettings().setBuiltInZoomControls(true);
          view.getSettings().setDisplayZoomControls(false);
          view.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){
              if(!req.isForMainFrame()||!req.hasGesture())return true;
              String scheme=req.getUrl().getScheme();
              if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme)&&!"mailto".equalsIgnoreCase(scheme))return true;
              try{startActivity(new Intent(Intent.ACTION_VIEW,req.getUrl()));}catch(Exception ignored){}return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){
              return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));
            }
          });
          // A fixed-height WebView scrolls its own HTML document. Do not resize it
          // to document height: long quoted threads otherwise get cut off.
          view.getSettings().setDefaultTextEncodingName("UTF-8");
          view.setVerticalScrollBarEnabled(true);
          view.setNestedScrollingEnabled(true);
          view.setOnTouchListener((v,event)->{
            if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN ||
               event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE){
              v.getParent().requestDisallowInterceptTouchEvent(true);
            }else if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP ||
                     event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL){
              v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
          });
          int htmlHeight=(int)(getResources().getDisplayMetrics().heightPixels*0.64f);
          slot.addView(view,new LinearLayout.LayoutParams(-1,Math.max(htmlHeight,300)));
          String viewport="<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">";
          // Scale wide email layouts to the phone while retaining readable tables.
          String responsive="<style>html,body{max-width:100%!important;overflow-wrap:anywhere;word-wrap:break-word;}"+
            "img,video,iframe{max-width:100%!important;height:auto!important;}"+
            "table{max-width:100%!important;}pre{white-space:pre-wrap;overflow-wrap:anywhere;}"+
            "</style>";
          String safeHtml=html.replaceFirst("(?i)<head[^>]*>","$0"+viewport+responsive);
          if(safeHtml.equals(html))safeHtml=viewport+responsive+html;
          view.loadDataWithBaseURL(null,safeHtml,"text/html","UTF-8",null);
          htmlVisible=true;
        });
      }finally{f.close(false);}}finally{st.close();}});
  }
  void showPlainMessage(String folder,long uid){
    MailCache.Item cached=cache.get(lastAccount,folder,uid);
    if(cached==null||messageBodySlot==null)return;
    messageBodySlot.removeAllViews();
    messageBodySlot.addView(text(cached.body==null?"Contenido todavía no descargado.":cached.body,16));
    htmlVisible=false;
  }
  void styleSelection(EditText editor,int style){int a=editor.getSelectionStart(),b=editor.getSelectionEnd();
    if(a<0||b<=a){Toast.makeText(this,"Selecciona texto primero",Toast.LENGTH_SHORT).show();return;}
    if(style==3)editor.getText().setSpan(new UnderlineSpan(),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    else editor.getText().setSpan(new StyleSpan(style==1?Typeface.BOLD:Typeface.ITALIC),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
  }
  String htmlEscape(String value){return android.text.TextUtils.htmlEncode(value).replace("\n","<br>");}
  String recipientList(Address[] addresses){
    if(addresses==null||addresses.length==0)return "—";
    StringBuilder result=new StringBuilder();
    for(Address address:addresses){
      if(result.length()>0)result.append("\n");
      result.append(decode(address.toString()));
    }
    return result.toString();
  }
  String shortSender(String full){
    if(full==null)return "Remitente";
    String value=full.trim();int bracket=value.indexOf('<');
    if(bracket>0)value=value.substring(0,bracket).trim().replace("\"","");
    return value.isEmpty()?full:value;
  }
  String summaryRecipients(String to,String cc){
    String combined="—".equals(to)?"":to.replace("\n",", ");
    if(!"—".equals(cc))combined+=(combined.isEmpty()?"":", ")+cc.replace("\n",", ");
    return combined.isEmpty()?"Destinatarios no disponibles":"Para: "+combined;
  }
  void addRecipientRows(LinearLayout parent,String title,Address[] addresses){
    parent.addView(text(title,14));
    if(addresses==null||addresses.length==0){parent.addView(text("—",14));return;}
    for(Address address:addresses){
      String name=address instanceof InternetAddress?((InternetAddress)address).getPersonal():null;
      String email=address instanceof InternetAddress?((InternetAddress)address).getAddress():address.toString();
      LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);
      row.setPadding(18,5,8,9);
      if(name!=null&&!name.trim().isEmpty()){
        TextView nameView=text(decode(name),15);nameView.setTypeface(null,Typeface.BOLD);row.addView(nameView);
      }
      row.addView(text(email==null?"—":email,14));parent.addView(row);
    }
  }
  void loadMessageRecipients(String folder,long uid,TextView summary,LinearLayout details){
    work(()->{
      Store store=connect();
      try{
        Folder f=store.getFolder(folder);f.open(Folder.READ_ONLY);
        try{
          Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;
          if(m==null)throw new Exception("Mensaje no disponible");
          String from=recipientList(m.getFrom());
          String to=recipientList(m.getRecipients(Message.RecipientType.TO));
          String cc=recipientList(m.getRecipients(Message.RecipientType.CC));
          String bcc=recipientList(m.getRecipients(Message.RecipientType.BCC));
          String date=m.getSentDate()==null?"—":new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm",java.util.Locale.getDefault()).format(m.getSentDate());
          final String brief=summaryRecipients(to,cc);
          final Address[] fromAddresses=m.getFrom(),toAddresses=m.getRecipients(Message.RecipientType.TO),ccAddresses=m.getRecipients(Message.RecipientType.CC),bccAddresses=m.getRecipients(Message.RecipientType.BCC);
          show(()->{if(summary.getParent()!=null){
            summary.setText(brief);details.removeAllViews();details.addView(text("Fecha: "+date,14));
            addRecipientRows(details,"De:",fromAddresses);addRecipientRows(details,"Para:",toAddresses);
            addRecipientRows(details,"CC:",ccAddresses);
            if(bccAddresses!=null&&bccAddresses.length>0)addRecipientRows(details,"CCO:",bccAddresses);
          }});
        }finally{f.close(false);}
      }finally{store.close();}
    });
  }
  void openCachedMessage(String folder,long uid){
    MailCache.Item cached=cache.get(lastAccount,folder,uid);if(cached==null)return;
    layout("Mensaje");
    displayedFolder=folder; displayedUid=uid; htmlVisible=false;
    // A true vertical layout: scroll uses remaining space; actions are a fixed sibling.
    // No overlay and no pixel-sized controls (which were clipped on high-density screens).
    ScrollView messageScroll=(ScrollView)root.getChildAt(1);
    int actionHeight=(int)(76*getResources().getDisplayMetrics().density+0.5f);
    content.addView(button("← Volver",()->renderCached(folder)));
    // Navigation uses the already cached folder ordering, without extra IMAP requests.
    ArrayList<MailCache.Item> nearby=cache.list(lastAccount,folder);
    int position=-1;
    for(int i=0;i<nearby.size();i++)if(nearby.get(i).uid==uid){position=i;break;}
    if(position>=0){
      LinearLayout navigation=new LinearLayout(this);
      navigation.setOrientation(LinearLayout.HORIZONTAL);
      final long newer=position>0?nearby.get(position-1).uid:-1;
      final long older=position+1<nearby.size()?nearby.get(position+1).uid:-1;
      Button newerButton=button("← Más reciente",()->openCachedMessage(folder,newer));
      newerButton.setEnabled(newer!=-1);
      navigation.addView(newerButton,new LinearLayout.LayoutParams(0,-2,1));
      Button olderButton=button("Más antiguo →",()->openCachedMessage(folder,older));
      olderButton.setEnabled(older!=-1);
      navigation.addView(olderButton,new LinearLayout.LayoutParams(0,-2,1));
      content.addView(navigation);
    }
    TextView subject=text(cached.subject,23);subject.setTypeface(null,Typeface.BOLD);content.addView(subject);
    LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(android.view.Gravity.CENTER_VERTICAL);
    TextView avatar=text(shortSender(cached.from).substring(0,1).toUpperCase(java.util.Locale.getDefault()),23);
    avatar.setGravity(android.view.Gravity.CENTER);avatar.setTextColor(Color.WHITE);avatar.setBackgroundColor(blue);
    LinearLayout.LayoutParams avatarLp=new LinearLayout.LayoutParams(58,58);avatarLp.setMargins(0,0,12,0);header.addView(avatar,avatarLp);
    LinearLayout senderColumn=new LinearLayout(this);senderColumn.setOrientation(LinearLayout.VERTICAL);
    TextView sender=text(shortSender(cached.from),18);sender.setTypeface(null,Typeface.BOLD);senderColumn.addView(sender);
    TextView summary=text("Para y CC: cargando…",13);summary.setSingleLine(true);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);senderColumn.addView(summary);
    header.addView(senderColumn,new LinearLayout.LayoutParams(0,-2,1));
    TextView toggle=text("⌄",26);toggle.setContentDescription("Mostrar u ocultar destinatarios");header.addView(toggle);
    content.addView(header);
    LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
    details.setPadding(14,12,14,12);details.addView(text("De: "+cached.from+"\nPara y CC: cargando…",14));
    details.setBackgroundColor(darkMode?Color.rgb(43,54,68):Color.rgb(245,247,250));
    details.setVisibility(View.GONE);content.addView(details);
    header.setOnClickListener(v->{boolean expand=details.getVisibility()!=View.VISIBLE;details.setVisibility(expand?View.VISIBLE:View.GONE);toggle.setText(expand?"⌃":"⌄");});
    if(configured)loadMessageRecipients(folder,uid,summary,details);
    LinearLayout attachmentSection=new LinearLayout(this);attachmentSection.setOrientation(LinearLayout.VERTICAL);
    TextView attachmentToggle=text("📎 Adjuntos  ⌄",17);
    attachmentToggle.setContentDescription("Mostrar u ocultar adjuntos");
    attachmentSection.addView(attachmentToggle);
    receivedAttachments=new LinearLayout(this);receivedAttachments.setOrientation(LinearLayout.VERTICAL);
    receivedAttachments.setVisibility(View.GONE);
    attachmentSection.addView(receivedAttachments);content.addView(attachmentSection);
    attachmentToggle.setOnClickListener(v->{boolean expand=receivedAttachments.getVisibility()!=View.VISIBLE;
      receivedAttachments.setVisibility(expand?View.VISIBLE:View.GONE);
      attachmentToggle.setText(expand?"📎 Adjuntos  ⌃":"📎 Adjuntos  ⌄");});
    if(configured)loadAttachments(folder,uid);
    messageBodySlot=new LinearLayout(this);messageBodySlot.setOrientation(LinearLayout.VERTICAL);
    content.addView(messageBodySlot);
    showPlainMessage(folder,uid);
    LinearLayout actions=new LinearLayout(this);
    actions.setOrientation(LinearLayout.HORIZONTAL);
    actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
    actions.setBackgroundColor(darkMode?Color.rgb(30,39,51):Color.WHITE);
    actions.setElevation(6*getResources().getDisplayMetrics().density);
    // Root is VERTICAL: the weighted ScrollView ends above this always-visible bar.
    root.addView(actions,new LinearLayout.LayoutParams(-1,actionHeight));
    if(configured){
      addMessageAction(actions,"↩","Responder",()->prepareResponse(folder,uid,"reply"));
      addMessageAction(actions,"↩↩","A todos",()->prepareResponse(folder,uid,"all"));
      addMessageAction(actions,"➜","Reenviar",()->prepareResponse(folder,uid,"forward"));
      addMessageAction(actions,"▣","Archivar",()->confirmMove(folder,uid,false));
      addMessageAction(actions,"✕","Eliminar",()->confirmMove(folder,uid,true));
    }else{
      addMessageAction(actions,"☆","Favorito",()->toggleFavorite(folder,uid));
    }
    addMessageMenu(actions,folder,uid,cached.seen);
    if(configured && prefs.getBoolean("default_html",false))showHtmlMessage(folder,uid);
  }
  void addMessageMenu(LinearLayout bar,String folder,long uid,boolean seen){
    addMessageAction(bar,"⋮","Más",()->{
      View anchor=bar.getChildAt(bar.getChildCount()-1);
      MailCache.Item current=cache.get(lastAccount,folder,uid);
      showMessageOptions(anchor,folder,uid,current==null?seen:current.seen);
    });
  }

  void showMessageOptions(View anchor,String folder,long uid,boolean seen){
    PopupMenu popup=new PopupMenu(this,anchor);
    popup.getMenu().add(seen?"Marcar como no leído":"Marcar como leído");
    popup.getMenu().add("Responder a todos");
    popup.getMenu().add("Descargar contenido");
    popup.getMenu().add(htmlVisible?"Ver texto plano":"Ver HTML original");
    popup.getMenu().add("Mover a carpeta (archivar)");
    popup.setOnMenuItemClickListener(item->{
      String choice=item.getTitle().toString();
      if(choice.startsWith("Marcar como"))setSeenBatch(folder,!seen,new ArrayList<>(java.util.Collections.singletonList(uid)));
      else if(choice.equals("Responder a todos"))prepareResponse(folder,uid,"all");
      else if(choice.equals("Descargar contenido"))fetchBody(folder,uid);
      else if(choice.equals("Ver HTML original"))showHtmlMessage(folder,uid);
      else if(choice.equals("Ver texto plano"))showPlainMessage(folder,uid);
      else if(choice.startsWith("Mover a carpeta"))confirmMove(folder,uid,false);
      return true;
    });
    popup.show();
  }
  void addMessageAction(LinearLayout bar,String symbol,String label,Runnable action){
    LinearLayout cell=new LinearLayout(this);
    cell.setOrientation(LinearLayout.VERTICAL);
    cell.setGravity(android.view.Gravity.CENTER);
    cell.setMinimumHeight((int)(64*getResources().getDisplayMetrics().density+0.5f));
    TextView glyph=text(symbol,25);
    glyph.setGravity(android.view.Gravity.CENTER);
    glyph.setIncludeFontPadding(false);
    TextView caption=text(label,11);
    caption.setGravity(android.view.Gravity.CENTER);
    caption.setSingleLine(true);
    caption.setIncludeFontPadding(false);
    cell.addView(glyph,new LinearLayout.LayoutParams(-1,-2));
    cell.addView(caption,new LinearLayout.LayoutParams(-1,-2));
    cell.setContentDescription(label);
    cell.setOnClickListener(v->action.run());
    bar.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
  }
  void confirmMove(String folder,long uid,boolean delete){
    new AlertDialog.Builder(this).setTitle(delete?"Mover a Papelera":"Archivar mensaje")
      .setMessage("Esta operación moverá el mensaje en el servidor IMAP. ¿Continuar?")
      .setNegativeButton("Cancelar",null).setPositiveButton("Continuar",(dialog,which)->moveMessage(folder,uid,delete)).show();
  }
  void moveMessage(String folder,long uid,boolean delete){
    work(()->{Store st=connect();try{
      Folder source=st.getFolder(folder);source.open(Folder.READ_WRITE);
      try{Message message=source instanceof IMAPFolder?((IMAPFolder)source).getMessageByUID(uid):null;
        if(message==null)throw new Exception("El mensaje ya no está en el servidor");
        String[] targets=delete?new String[]{"Trash","INBOX.Trash","Deleted Items","Papelera"}:new String[]{"Archive","INBOX.Archive","Archives","Archivo"};
        Folder target=null;for(String name:targets){Folder candidate=st.getFolder(name);if(candidate.exists()){target=candidate;break;}}
        if(target==null)throw new Exception("No se encontró la carpeta de destino. No se ha movido el mensaje.");
        source.copyMessages(new Message[]{message},target);
        message.setFlag(Flags.Flag.DELETED,true);
        source.close(true);
        cache.remove(lastAccount,folder,uid);
        show(()->{Toast.makeText(this,delete?"Movido a Papelera":"Archivado",Toast.LENGTH_SHORT).show();renderCached(folder);});
      }finally{if(source.isOpen())source.close(false);}
    }finally{st.close();}});
  }
  void fetchBody(String folder,long uid){work(()->{Store st=connect();try{Folder f=st.getFolder(folder);f.open(Folder.READ_ONLY);try{Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;if(m==null)throw new Exception("Mensaje no disponible en el servidor");String bodyText=body(m,0);cache.saveBody(lastAccount,folder,uid,bodyText);show(()->openCachedMessage(folder,uid));}finally{f.close(false);}}finally{st.close();}});}

  void renderMessages(String folder,int total,ArrayList<String[]> rows){menu();content.addView(text(folder+" · "+total+" mensajes",19));content.addView(button("↻ Actualizar",()->openFolder(folder)));for(String[] r:rows){TextView item=text(r[4]+r[1]+"\n"+r[2]+"\n"+r[3],15);item.setBackgroundColor(darkMode?Color.rgb(45,55,69):Color.rgb(245,248,252));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;content.addView(item,lp);item.setOnClickListener(v->openMessage(folder,Integer.parseInt(r[0])));}}
  String body(Part part,int depth)throws Exception{if(depth>8)return "";if(part.isMimeType("text/plain")){Object c=part.getContent();return c==null?"":c.toString();}if(part.isMimeType("text/html")){Object c=part.getContent();return c==null?"":android.text.Html.fromHtml(c.toString(),android.text.Html.FROM_HTML_MODE_LEGACY).toString();}Object c=part.getContent();if(c instanceof Multipart){Multipart mp=(Multipart)c;String html="";for(int i=0;i<mp.getCount();i++){Part child=mp.getBodyPart(i);if(Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition()))continue;String result=body(child,depth+1);if(child.isMimeType("text/plain")&&!result.isEmpty())return result;if(html.isEmpty())html=result;}return html;}return "(Contenido no compatible o adjunto)";}
  void openMessage(String folder,int index){menu();content.addView(text("Abriendo mensaje…",16));work(()->{Store st=connect();try{Folder f=st.getFolder(folder);f.open(Folder.READ_ONLY);try{Message m=f.getMessage(index);String from=sender(m),subject=decode(m.getSubject()),message=body(m,0),to=recipientList(m.getRecipients(Message.RecipientType.TO)),cc=recipientList(m.getRecipients(Message.RecipientType.CC)),bcc=recipientList(m.getRecipients(Message.RecipientType.BCC));show(()->{layout("Mensaje");content.addView(button("← Volver",()->openFolder(folder)));content.addView(text(subject,22));content.addView(text("De: "+from,15));content.addView(text("Para: "+to+"\nCC: "+cc+("—".equals(bcc)?"":"\nCCO: "+bcc),15));content.addView(text(message,16));content.addView(button("↩ Responder",()->compose(from.replaceAll(".*<([^>]+)>.*","$1"),"Re: "+subject,"\n\n--- Mensaje original ---\n"+message)));content.addView(button("↪ Reenviar",()->compose("","Fwd: "+subject,"\n\n--- Mensaje reenviado ---\n"+message)));});}finally{f.close(false);}}finally{st.close();}});}
  void folders(){menu();content.addView(text("Cargando carpetas…",16));work(()->{Store st=connect();try{Folder[] fs=st.getDefaultFolder().list("*");ArrayList<String> names=new ArrayList<>();for(Folder f:fs)if((f.getType()&Folder.HOLDS_MESSAGES)!=0)names.add(f.getFullName());show(()->{menu();content.addView(text("Carpetas",20));for(String name:names)content.addView(button("📁 "+name,()->openFolder(name)));});}finally{st.close();}});}
  // Retrieve the live MIME headers for correct Reply-To, Reply-All and threading.
  void prepareResponse(String folder,long uid,String mode){
    MailCache.Item cached=cache.get(lastAccount,folder,uid);
    if(cached==null)return;
    if(!configured){error("Conecta con el servidor para responder con destinatarios correctos");return;}
    work(()->{
      Store store=connect();
      try{
        Folder f=store.getFolder(folder);f.open(Folder.READ_ONLY);
        try{
          Message original=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;
          if(original==null)throw new Exception("No se encuentra el mensaje original");
          String subject=decode(original.getSubject());
          String quoted=body(original,0);
          String quotedHtml=htmlPart(original,0);
          String from=recipientList(original.getFrom());
          String to=recipientList(original.getRecipients(Message.RecipientType.TO));
          String cc=recipientList(original.getRecipients(Message.RecipientType.CC));
          String stamp=original.getSentDate()==null?"":original.getSentDate().toString();
          String[] messageIdHeaders=original.getHeader("Message-ID");
          String messageId=(messageIdHeaders!=null && messageIdHeaders.length>0)?messageIdHeaders[0]:null;
          String[] referencesHeaders=original.getHeader("References");
          String references=(referencesHeaders!=null && referencesHeaders.length>0)?referencesHeaders[0]:null;
          String toValue="",ccValue="";
          if(!"forward".equals(mode)){
            LinkedHashMap<String,InternetAddress> recipients=new LinkedHashMap<>();
            addReplyAddresses(recipients,original.getReplyTo());
            if("all".equals(mode)){
              addReplyAddresses(recipients,original.getRecipients(Message.RecipientType.TO));
              addReplyAddresses(recipients,original.getRecipients(Message.RecipientType.CC));
            }
            LinkedHashMap<String,InternetAddress> ccRecipients=new LinkedHashMap<>();
            if("all".equals(mode)){
              // Primary reply recipient stays in To; other original recipients remain visible in CC.
              LinkedHashMap<String,InternetAddress> replyOnly=new LinkedHashMap<>();
              addReplyAddresses(replyOnly,original.getReplyTo());
              for(Map.Entry<String,InternetAddress> entry:recipients.entrySet())
                if(!replyOnly.containsKey(entry.getKey()))ccRecipients.put(entry.getKey(),entry.getValue());
              recipients=replyOnly;
            }
            toValue=joinAddresses(recipients);ccValue=joinAddresses(ccRecipients);
          }
          String quoteHeader="\n\n---------- Mensaje original ----------\nDe: "+from+"\nFecha: "+stamp+"\nPara: "+to+"\nCC: "+cc+"\nAsunto: "+subject+"\n\n";
          String draft="\n\n"+quoteHeader+quoted;
          String prefix="forward".equals(mode)?"Fwd: ":"Re: ";
          String cleanSubject=subject.replaceFirst("(?i)^(re|fwd|fw):\\s*","");
          final String finalTo=toValue,finalCc=ccValue,finalDraft=draft,finalSubject=prefix+cleanSubject;
          final String finalQuoteHtml=quotedHtml,finalQuoteHeader=quoteHeader;
          final String finalId=messageId,finalRefs=references;
          show(()->composeAdvanced(finalTo,finalCc,finalSubject,finalDraft,
              "forward".equals(mode)?null:finalId,"forward".equals(mode)?null:finalRefs,finalQuoteHtml,finalQuoteHeader));
        }finally{f.close(false);}
      }finally{store.close();}
    });
  }
  void addReplyAddresses(Map<String,InternetAddress> out,Address[] addresses){
    if(addresses==null)return;
    for(Address a:addresses)if(a instanceof InternetAddress){
      InternetAddress ia=(InternetAddress)a;String addr=ia.getAddress();
      if(addr!=null&&!addr.trim().isEmpty()&&!addr.equalsIgnoreCase(u))out.put(addr.toLowerCase(Locale.ROOT),ia);
    }
  }
  String joinAddresses(Map<String,InternetAddress> addresses){
    StringBuilder result=new StringBuilder();
    for(InternetAddress ia:addresses.values()){
      if(result.length()>0)result.append(", ");result.append(ia.toUnicodeString());
    }return result.toString();
  }
  // Locate the original HTML part, preferring it over the plain-text alternative.
  String htmlPart(Part part,int depth)throws Exception{
    if(depth>10)return null;
    if(part.isMimeType("text/html")){Object value=part.getContent();return value==null?null:value.toString();}
    Object value=part.getContent();
    if(value instanceof Multipart){Multipart mp=(Multipart)value;
      for(int i=0;i<mp.getCount();i++){
        Part child=mp.getBodyPart(i);
        if(Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition()))continue;
        String result=htmlPart(child,depth+1);if(result!=null)return result;
      }
    }return null;
  }
  void addRichFormatting(LinearLayout formatBar,EditText editor){
    for(String label:new String[]{"B","I","U"}){
      Button fmt=new Button(this);fmt.setText(label);
      formatBar.addView(fmt,new LinearLayout.LayoutParams(0,-2,1));
      fmt.setOnClickListener(v->styleSelection(editor,label.equals("B")?1:label.equals("I")?2:3));
    }
    Button link=new Button(this);link.setText("🔗");
    formatBar.addView(link,new LinearLayout.LayoutParams(0,-2,1));
    link.setOnClickListener(v->{int start=editor.getSelectionStart(),end=editor.getSelectionEnd();
      if(start<0||end<=start){error("Selecciona el texto del enlace");return;}
      EditText url=new EditText(this);url.setHint("https://...");url.setSingleLine(true);
      new AlertDialog.Builder(this).setTitle("Insertar enlace").setView(url)
        .setPositiveButton("Aplicar",(d,w)->{String address=url.getText().toString().trim();
          if(address.startsWith("https://")||address.startsWith("http://"))
            editor.getText().setSpan(new android.text.style.URLSpan(address),start,end,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          else error("Utiliza un enlace https:// o http://");})
        .setNegativeButton("Cancelar",null).show();
    });
  }
  void compose(String toValue,String subjectValue,String bodyValue){
    composeAdvanced(toValue,"",subjectValue,bodyValue,null,null,null,null);
  }
  void composeAdvanced(String toValue,String ccValue,String subjectValue,String bodyValue,String replyId,String references,String quotedHtml,String quoteHeader){
    outgoingAttachments.clear();layout("Redactar");
    EditText to=input(content,"Para",toValue,false),cc=input(content,"CC (copia)",ccValue,false),subject=input(content,"Asunto",subjectValue,false);
    boolean initialHtml=prefs.getBoolean("default_html",false);
    CheckBox htmlMode=new CheckBox(this);htmlMode.setText("Redactar en HTML (editor con formato)");htmlMode.setChecked(initialHtml);content.addView(htmlMode);
    EditText body=new EditText(this);body.setHint("Escribe tu mensaje");body.setMinLines(8);
    SpannableStringBuilder richDraft=new SpannableStringBuilder(bodyValue);
    if(quotedHtml!=null&&!quotedHtml.isEmpty()&&quoteHeader!=null){
      String richHeader="\n\n"+quoteHeader;
      int quotedStart=bodyValue.indexOf(quoteHeader);
      if(quotedStart>=0){
        richDraft=new SpannableStringBuilder(bodyValue.substring(0,quotedStart));
        richDraft.append(richHeader);
        richDraft.append(Html.fromHtml(quotedHtml,Html.FROM_HTML_MODE_LEGACY));
      }
    }
    body.setText(initialHtml?richDraft:new SpannableStringBuilder(bodyValue));content.addView(body);
    LinearLayout formatBar=new LinearLayout(this);formatBar.setOrientation(LinearLayout.HORIZONTAL);
    addRichFormatting(formatBar,body);content.addView(formatBar);
    TextView hint=text("Selecciona texto y pulsa B, I, U o 🔗. El correo se envía en el formato elegido.",12);content.addView(hint);
    formatBar.setVisibility(initialHtml?View.VISIBLE:View.GONE);hint.setVisibility(initialHtml?View.VISIBLE:View.GONE);
    final SpannableStringBuilder richTemplate=richDraft;
    htmlMode.setOnCheckedChangeListener((button,enabled)->{
      formatBar.setVisibility(enabled?View.VISIBLE:View.GONE);hint.setVisibility(enabled?View.VISIBLE:View.GONE);
      // Keep the current text on format changes; when switching from plain to HTML,
      // restore the formatted original quote if the draft is still untouched.
      if(enabled&&body.getText().toString().equals(bodyValue))body.setText(richTemplate);
    });
    attachmentPreview=new LinearLayout(this);attachmentPreview.setOrientation(LinearLayout.VERTICAL);content.addView(attachmentPreview);content.addView(button("📎 Añadir archivo",()->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,PICK_ATTACHMENT);}));content.addView(button("Enviar",()->{String dest=to.getText().toString().trim(),sub=subject.getText().toString(),msg=body.getText().toString();if(dest.isEmpty()||sh.isEmpty()){error("Completa destinatario y servidor SMTP");return;}work(()->{Properties props=new Properties();props.put("mail.smtp.auth","true");props.put("mail.smtp.host",sh);props.put("mail.smtp.port",String.valueOf(sPort));props.put("mail.smtp.connectiontimeout","15000");props.put("mail.smtp.timeout","15000");if(sPort==465)props.put("mail.smtp.ssl.enable","true");else{props.put("mail.smtp.starttls.enable","true");props.put("mail.smtp.starttls.required","true");}Session session=Session.getInstance(props,new Authenticator(){protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(u,pw);}});MimeMessage m=new MimeMessage(session);m.setFrom(new InternetAddress(u));m.setRecipients(Message.RecipientType.TO,InternetAddress.parse(dest,true));String ccText=cc.getText().toString().trim();if(!ccText.isEmpty())m.setRecipients(Message.RecipientType.CC,InternetAddress.parse(ccText,true));if(replyId!=null&&!replyId.trim().isEmpty()){m.setHeader("In-Reply-To",replyId);m.setHeader("References",(references==null||references.trim().isEmpty()?"":references.trim()+" ")+replyId);}m.setSubject(sub,"UTF-8");String signature=prefs.getString("signature","");
      String finalBody=msg+(signature.trim().isEmpty()?"":"\n\n-- \n"+signature);
      String formatted=android.text.Html.toHtml(body.getText(),android.text.Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
      String finalHtml="<html><body>"+formatted+(signature.trim().isEmpty()?"":"<p>--<br>"+htmlEscape(signature)+"</p>")+"</body></html>";
      ArrayList<Uri> files=new ArrayList<>(outgoingAttachments);
      MimeMultipart alternative=new MimeMultipart("alternative");
      MimeBodyPart plain=new MimeBodyPart();plain.setText(finalBody,"UTF-8");alternative.addBodyPart(plain);
      MimeBodyPart rich=new MimeBodyPart();rich.setContent(finalHtml,"text/html; charset=UTF-8");alternative.addBodyPart(rich);
      if(files.isEmpty()){if(htmlMode.isChecked())m.setContent(alternative);else m.setText(finalBody,"UTF-8");}
      else{MimeMultipart multi=new MimeMultipart("mixed");MimeBodyPart messagePart=new MimeBodyPart();if(htmlMode.isChecked())messagePart.setContent(alternative);else messagePart.setText(finalBody,"UTF-8");multi.addBodyPart(messagePart);
        for(Uri uri:files){byte[] bytes;try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
          if(in==null)throw new Exception("No se puede abrir el adjunto");byte[] buf=new byte[8192];int n;
          while((n=in.read(buf))!=-1){if(out.size()+n>25*1024*1024)throw new Exception("Archivo demasiado grande (máx. 25 MB)");out.write(buf,0,n);}bytes=out.toByteArray();}
          MimeBodyPart attachment=new MimeBodyPart();attachment.setFileName(MimeUtility.encodeText(attachmentName(uri),"UTF-8",null));
          attachment.setDataHandler(new javax.activation.DataHandler(new javax.mail.util.ByteArrayDataSource(bytes,"application/octet-stream")));
          multi.addBodyPart(attachment);
        }m.setContent(multi);
      }
      Transport.send(m);show(()->{new AlertDialog.Builder(this).setMessage("Mensaje enviado").setPositiveButton("Aceptar",(d,w)->openFolder(folderName)).show();});});}));content.addView(button("Cancelar",()->openFolder(folderName)));}
  @Override public void onBackPressed(){renderCached(folderName);}
}
