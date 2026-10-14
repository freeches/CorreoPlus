package com.example.correoplus;

import android.app.Activity;
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
  String h,u,pw,sh; int imapPort,sPort; boolean configured=false; boolean rememberPassword=false; String folderName="INBOX";
  final int navy=Color.rgb(17,43,74), blue=Color.rgb(0,105,185);
  interface Task { void run() throws Exception; }
  void work(Task task){new Thread(()->{try{task.run();}catch(Exception e){error(e.getClass().getSimpleName()+": "+e.getMessage());}}).start();}
  void show(Runnable action){ui.post(action);} void error(String s){show(()->new AlertDialog.Builder(this).setTitle("CorreoPlus").setMessage(s).setPositiveButton("Aceptar",null).show());}
  TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(navy);t.setPadding(12,13,12,13);return t;}
  Button button(String name,Runnable run){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setOnClickListener(v->run.run());return b;}
  EditText input(LinearLayout target,String hint,String initial,boolean secret){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setText(initial);if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);target.addView(e);return e;}
  void layout(String heading){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);setContentView(root);TextView title=text("✉  CorreoPlus    "+heading,22);title.setTextColor(Color.WHITE);title.setBackgroundColor(navy);root.addView(title);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(14,12,14,12);sc.addView(content);root.addView(sc);}
  @Override public void onCreate(Bundle b){super.onCreate(b);cache=new MailCache(this);prefs=getSharedPreferences("correoplus_settings",MODE_PRIVATE);lastAccount=prefs.getString("account","");if(lastAccount.isEmpty())settings();else {
    h=prefs.getString("imap_host",""); u=prefs.getString("username","");
    sh=prefs.getString("smtp_host","");
    try { imapPort=Integer.parseInt(prefs.getString("imap_port","993")); sPort=Integer.parseInt(prefs.getString("smtp_port","587")); }
    catch(NumberFormatException e){imapPort=993;sPort=587;}
    try { pw=CredentialVault.load(this,lastAccount); configured=pw!=null && !h.isEmpty() && !u.isEmpty(); }
    catch(Exception e){configured=false;pw=null;}
    renderCached("INBOX");
    if(configured) openFolder("INBOX");
  }}
  void settings(){layout("Cuentas");content.addView(text("Correo IMAP / SMTP · Conexión cifrada",17));content.addView(text("Gmail y Outlook con OAuth2 todavía no disponibles. Puedes optar por guardar la contraseña cifrada con Android Keystore. Gmail y Outlook requieren OAuth2 y todavía no están integrados.",13));host=input(content,"Servidor IMAP",prefs.getString("imap_host",""),false);port=input(content,"Puerto IMAP 993 o 143",prefs.getString("imap_port","993"),false);user=input(content,"Correo / usuario",prefs.getString("username",""),false);pass=input(content,"Contraseña IMAP", "",true);
  CheckBox remember=new CheckBox(this);remember.setText("Recordar contraseña cifrada en este dispositivo");content.addView(remember);
  content.addView(button("Borrar contraseña guardada",()->{CredentialVault.clear(this);pw=null;configured=false;error("Credenciales guardadas eliminadas");}));smtpHost=input(content,"Servidor SMTP",prefs.getString("smtp_host",""),false);smtpPort=input(content,"Puerto SMTP 465 o 587",prefs.getString("smtp_port","587"),false);content.addView(button("Conectar y abrir bandeja",()->{try{h=host.getText().toString().trim();u=user.getText().toString().trim();pw=pass.getText().toString();sh=smtpHost.getText().toString().trim();imapPort=Integer.parseInt(port.getText().toString().trim());sPort=Integer.parseInt(smtpPort.getText().toString().trim());if(h.isEmpty()||u.isEmpty()||pw.isEmpty()||!(imapPort==993||imapPort==143)||!(sPort==465||sPort==587))throw new Exception("Revisa servidor, usuario, contraseña y puertos admitidos");rememberPassword=remember.isChecked();configured=true;lastAccount=u.toLowerCase(Locale.ROOT)+"@"+h.toLowerCase(Locale.ROOT);if(rememberPassword){CredentialVault.save(this,lastAccount,pw);}else{CredentialVault.clear(this);}prefs.edit().putString("account",lastAccount).putString("imap_host",h).putString("imap_port",String.valueOf(imapPort)).putString("username",u).putString("smtp_host",sh).putString("smtp_port",String.valueOf(sPort)).apply();openFolder("INBOX");}catch(Exception e){error(e.getMessage());}}));}
  Properties imapProperties(){Properties props=new Properties();props.put("mail.store.protocol",imapPort==993?"imaps":"imap");String prefix=imapPort==993?"mail.imaps":"mail.imap";props.put(prefix+".connectiontimeout","15000");props.put(prefix+".timeout","15000");if(imapPort==993)props.put("mail.imaps.ssl.enable","true");else{props.put("mail.imap.starttls.enable","true");props.put("mail.imap.starttls.required","true");}return props;}
  Store connect()throws Exception{Session s=Session.getInstance(imapProperties());Store st=s.getStore(imapPort==993?"imaps":"imap");st.connect(h,imapPort,u,pw);return st;}
  String decode(String s){if(s==null)return "(sin asunto)";try{return MimeUtility.decodeText(s);}catch(Exception e){return s;}}
  String sender(Message m)throws Exception{Address[] a=m.getFrom();return a==null||a.length==0?"Remitente desconocido":decode(a[0].toString());}
  void menu(){layout("Correo");content.addView(button("☰ Carpetas",()->folders()));content.addView(button("⚙ Cuenta",()->settings()));content.addView(button("✎ Redactar",()->compose("","","")));}
  void openFolder(String folder){
    folderName=folder;renderCached(folder);
    if(!configured){content.addView(text("Sin sesión activa · Abre Cuenta para conectar; la caché sigue disponible",14));return;}
    content.addView(text("Sincronizando…",14));
    work(()->{
      Store st=connect();
      try{
        Folder f=st.getFolder(folder);f.open(Folder.READ_ONLY);
        try{
          int n=f.getMessageCount();
          long validity=f instanceof IMAPFolder?((IMAPFolder)f).getUIDValidity():0;
          long lastUid=cache.maxUid(lastAccount,folder,validity);
          ArrayList<MailCache.Item> batch=new ArrayList<>();
          // Scan the most recent messages for updated read flags; download headers only for new UIDs.
          for(int i=n;i>Math.max(0,n-100);i--){
            Message m=f.getMessage(i);
            long uid=f instanceof IMAPFolder?((IMAPFolder)f).getUID(m):i;
            MailCache.Item existing=uid<=lastUid?cache.get(lastAccount,folder,uid):null;
            boolean seen=m.isSet(Flags.Flag.SEEN);
            if(existing!=null){
              if(existing.seen!=seen)cache.updateSeen(lastAccount,folder,validity,uid,seen);
            }else{
              Date d=m.getSentDate();
              batch.add(new MailCache.Item(uid,sender(m),decode(m.getSubject()),d==null?"":d.toString(),seen,null));
            }
          }
          cache.saveHeaders(lastAccount,folder,validity,batch);
          // Download only the bodies missing from the most recent messages.
          ArrayList<MailCache.Item> recent=cache.list(lastAccount,folder);
          for(int j=0;j<Math.min(20,recent.size());j++){
            MailCache.Item item=recent.get(j);
            if(item.body==null){
              try{
                Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(item.uid):null;
                if(m!=null)cache.saveBody(lastAccount,folder,item.uid,body(m,0));
              }catch(Exception ignored){}
            }
          }
          prefs.edit().putLong("synced_"+lastAccount+"_"+folder,System.currentTimeMillis()).apply();
          show(()->renderCached(folder));
        }finally{f.close(false);}
      }finally{st.close();}
    });
  }
  void renderCached(String folder){folderName=folder;menu();ArrayList<MailCache.Item> rows=cache.list(lastAccount,folder);content.addView(text(folder+" · "+rows.size()+" mensajes guardados",19));
    long last=prefs.getLong("synced_"+lastAccount+"_"+folder,0);
    content.addView(text(last==0?"Aún no sincronizado":"Última sincronización: "+new java.text.SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date(last)),12));
    EditText search=input(content,"Buscar en remitente o asunto (caché)","",false);
    LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.addView(results);
    search.addTextChangedListener(new android.text.TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){renderSearch(results,folder,s.toString());}
      public void afterTextChanged(android.text.Editable e){}
    });
    renderSearch(results,folder,"");content.addView(button("↻ Sincronizar",()->{if(configured)openFolder(folder);else settings();}));/* legacy listing removed */for(MailCache.Item r:new ArrayList<MailCache.Item>()){TextView item=text((r.seen?"":"● ")+r.from+"\n"+r.subject+"\n"+r.date,15);item.setBackgroundColor(Color.rgb(245,248,252));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;content.addView(item,lp);item.setOnClickListener(v->openCachedMessage(folder,r.uid));}}
  void renderSearch(LinearLayout target,String folder,String query){
    target.removeAllViews();
    ArrayList<MailCache.Item> rows=cache.search(lastAccount,folder,query);
    for(MailCache.Item r:rows){
      TextView item=text((r.seen?"":"● ")+r.from+"\n"+r.subject+"\n"+r.date,15);
      item.setBackgroundColor(Color.rgb(245,248,252));
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;
      target.addView(item,lp);item.setOnClickListener(v->openCachedMessage(folder,r.uid));
    }
  }
  void openCachedMessage(String folder,long uid){MailCache.Item cached=cache.get(lastAccount,folder,uid);if(cached==null)return;layout("Mensaje");content.addView(button("← Volver",()->renderCached(folder)));content.addView(text(cached.subject,22));content.addView(text("De: "+cached.from,15));content.addView(text(cached.body==null?"Contenido todavía no descargado. Conéctate para leerlo.":cached.body,16));if(configured){content.addView(button("↻ Descargar contenido",()->fetchBody(folder,uid)));content.addView(button("↩ Responder",()->compose(cached.from.replaceAll(".*<([^>]+)>.*","$1"),"Re: "+cached.subject,"")));content.addView(button("↪ Reenviar",()->compose("","Fwd: "+cached.subject,cached.body==null?"":cached.body)));}}
  void fetchBody(String folder,long uid){work(()->{Store st=connect();try{Folder f=st.getFolder(folder);f.open(Folder.READ_ONLY);try{Message m=f instanceof IMAPFolder?((IMAPFolder)f).getMessageByUID(uid):null;if(m==null)throw new Exception("Mensaje no disponible en el servidor");String bodyText=body(m,0);cache.saveBody(lastAccount,folder,uid,bodyText);show(()->openCachedMessage(folder,uid));}finally{f.close(false);}}finally{st.close();}});}

  void renderMessages(String folder,int total,ArrayList<String[]> rows){menu();content.addView(text(folder+" · "+total+" mensajes",19));content.addView(button("↻ Actualizar",()->openFolder(folder)));for(String[] r:rows){TextView item=text(r[4]+r[1]+"\n"+r[2]+"\n"+r[3],15);item.setBackgroundColor(Color.rgb(245,248,252));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;content.addView(item,lp);item.setOnClickListener(v->openMessage(folder,Integer.parseInt(r[0])));}}
  String body(Part part,int depth)throws Exception{if(depth>8)return "";if(part.isMimeType("text/plain")){Object c=part.getContent();return c==null?"":c.toString();}if(part.isMimeType("text/html")){Object c=part.getContent();return c==null?"":android.text.Html.fromHtml(c.toString(),android.text.Html.FROM_HTML_MODE_LEGACY).toString();}Object c=part.getContent();if(c instanceof Multipart){Multipart mp=(Multipart)c;String html="";for(int i=0;i<mp.getCount();i++){Part child=mp.getBodyPart(i);if(Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition()))continue;String result=body(child,depth+1);if(child.isMimeType("text/plain")&&!result.isEmpty())return result;if(html.isEmpty())html=result;}return html;}return "(Contenido no compatible o adjunto)";}
  void openMessage(String folder,int index){menu();content.addView(text("Abriendo mensaje…",16));work(()->{Store st=connect();try{Folder f=st.getFolder(folder);f.open(Folder.READ_ONLY);try{Message m=f.getMessage(index);String from=sender(m),subject=decode(m.getSubject()),message=body(m,0);show(()->{layout("Mensaje");content.addView(button("← Volver",()->openFolder(folder)));content.addView(text(subject,22));content.addView(text("De: "+from,15));content.addView(text(message,16));content.addView(button("↩ Responder",()->compose(from.replaceAll(".*<([^>]+)>.*","$1"),"Re: "+subject,"\n\n--- Mensaje original ---\n"+message)));content.addView(button("↪ Reenviar",()->compose("","Fwd: "+subject,"\n\n--- Mensaje reenviado ---\n"+message)));});}finally{f.close(false);}}finally{st.close();}});}
  void folders(){menu();content.addView(text("Cargando carpetas…",16));work(()->{Store st=connect();try{Folder[] fs=st.getDefaultFolder().list("*");ArrayList<String> names=new ArrayList<>();for(Folder f:fs)if((f.getType()&Folder.HOLDS_MESSAGES)!=0)names.add(f.getFullName());show(()->{menu();content.addView(text("Carpetas",20));for(String name:names)content.addView(button("📁 "+name,()->openFolder(name)));});}finally{st.close();}});}
  void compose(String toValue,String subjectValue,String bodyValue){layout("Redactar");EditText to=input(content,"Destinatario",toValue,false),subject=input(content,"Asunto",subjectValue,false);EditText body=new EditText(this);body.setHint("Escribe tu mensaje");body.setText(bodyValue);body.setMinLines(8);content.addView(body);content.addView(button("Enviar",()->{String dest=to.getText().toString().trim(),sub=subject.getText().toString(),msg=body.getText().toString();if(dest.isEmpty()||sh.isEmpty()){error("Completa destinatario y servidor SMTP");return;}work(()->{Properties props=new Properties();props.put("mail.smtp.auth","true");props.put("mail.smtp.host",sh);props.put("mail.smtp.port",String.valueOf(sPort));props.put("mail.smtp.connectiontimeout","15000");props.put("mail.smtp.timeout","15000");if(sPort==465)props.put("mail.smtp.ssl.enable","true");else{props.put("mail.smtp.starttls.enable","true");props.put("mail.smtp.starttls.required","true");}Session session=Session.getInstance(props,new Authenticator(){protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(u,pw);}});MimeMessage m=new MimeMessage(session);m.setFrom(new InternetAddress(u));m.setRecipients(Message.RecipientType.TO,InternetAddress.parse(dest,true));m.setSubject(sub,"UTF-8");m.setText(msg,"UTF-8");Transport.send(m);show(()->{new AlertDialog.Builder(this).setMessage("Mensaje enviado").setPositiveButton("Aceptar",(d,w)->openFolder(folderName)).show();});});}));content.addView(button("Cancelar",()->openFolder(folderName)));}
  @Override public void onBackPressed(){renderCached(folderName);}
}
