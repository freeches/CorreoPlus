package com.example.correoplus;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class MainActivity extends Activity {
  LinearLayout root; EditText host, port, username, password, smtpHost, smtpPort; TextView output;
  String currentHost, currentUser, currentPassword, currentSmtpHost, currentSmtpPort;
  int currentPort;
  final Handler handler = new Handler(Looper.getMainLooper());
  interface Work { String run() throws Exception; }
  void background(Work work) { output.setText("Conectando…"); new Thread(() -> { String result; try { result=work.run(); } catch(Exception e) { result="Error: "+e.getClass().getSimpleName()+": "+e.getMessage(); } String finalResult=result; handler.post(()->output.setText(finalResult)); }).start(); }
  EditText field(String hint,String initial,boolean secret){ EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setText(initial); if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(e);return e; }
  Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);root.addView(b);b.setOnClickListener(v->action.run());return b;}
  @Override public void onCreate(Bundle state){super.onCreate(state); ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,24,28,24);scroll.addView(root);setContentView(scroll);
    TextView title=new TextView(this);title.setText("CorreoPlus · IMAP/SMTP");title.setTextSize(23);title.setTextColor(Color.rgb(20,60,90));root.addView(title);
    TextView note=new TextView(this);note.setText("Prototipo IMAP para servidores que admiten contraseña o contraseña de aplicación. Gmail y Outlook con OAuth2 todavía NO están integrados. No guarda credenciales.");root.addView(note);
    host=field("Servidor IMAP", "imap.example.com",false);port=field("Puerto IMAP (TLS)","993",false);username=field("Correo / usuario","",false);password=field("Contraseña de aplicación / IMAP","",true);
    smtpHost=field("Servidor SMTP","smtp.example.com",false);smtpPort=field("Puerto SMTP STARTTLS","587",false);
    button("Leer últimos 15 mensajes",()->{if(!capture())return;background(()->readMail());});
    button("Redactar y enviar",()->{if(!capture())return;compose();});
    output=new TextView(this);output.setTextIsSelectable(true);output.setText("Configura un servidor IMAP para comenzar.");output.setTextSize(15);output.setPadding(0,20,0,20);root.addView(output);
  }
  boolean capture(){try{currentHost=host.getText().toString().trim();currentPort=Integer.parseInt(port.getText().toString().trim());currentUser=username.getText().toString().trim();currentPassword=password.getText().toString();currentSmtpHost=smtpHost.getText().toString().trim();currentSmtpPort=smtpPort.getText().toString().trim();if(currentHost.isEmpty()||currentUser.isEmpty()||currentPassword.isEmpty())throw new Exception("Completa servidor, usuario y contraseña");if(currentPort!=993)throw new Exception("Esta versión solo permite IMAP TLS en puerto 993");return true;}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return false;}}
  String readMail() throws Exception {Properties props=new Properties();props.put("mail.store.protocol","imaps");props.put("mail.imaps.ssl.enable","true");props.put("mail.imaps.connectiontimeout","15000");props.put("mail.imaps.timeout","15000"); Session session=Session.getInstance(props);Store store=session.getStore("imaps");try{store.connect(currentHost,currentPort,currentUser,currentPassword);Folder folder=store.getFolder("INBOX");folder.open(Folder.READ_ONLY);try{int count=folder.getMessageCount();StringBuilder sb=new StringBuilder("Bandeja de entrada · "+count+" mensajes\n\n");for(int i=count;i>Math.max(0,count-15);i--){Message m=folder.getMessage(i);sb.append("✉ ").append(m.getSubject()==null?"(sin asunto)":m.getSubject()).append("\n");Address[] a=m.getFrom();if(a!=null&&a.length>0)sb.append("De: ").append(a[0]).append("\n");sb.append("Fecha: ").append(m.getSentDate()).append("\n\n");}return sb.toString();}finally{folder.close(false);}}finally{store.close();}}
  void compose(){LinearLayout form=new LinearLayout(this);form.setPadding(20,10,20,10);form.setOrientation(LinearLayout.VERTICAL);EditText to=new EditText(this);to.setHint("Destinatario");form.addView(to);EditText subject=new EditText(this);subject.setHint("Asunto");form.addView(subject);EditText body=new EditText(this);body.setHint("Mensaje");body.setMinLines(4);form.addView(body);new AlertDialog.Builder(this).setTitle("Nuevo correo").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Enviar",(d,w)->{String dest=to.getText().toString(),sub=subject.getText().toString(),msg=body.getText().toString();background(()->sendMail(dest,sub,msg));}).show();}
  String sendMail(String to,String subject,String body)throws Exception{if(currentSmtpHost.isEmpty())return "Configura el servidor SMTP";int p=Integer.parseInt(currentSmtpPort);if(p!=587)return "Esta versión solo permite SMTP STARTTLS en puerto 587";Properties props=new Properties();props.put("mail.smtp.auth","true");props.put("mail.smtp.starttls.enable","true");props.put("mail.smtp.starttls.required","true");props.put("mail.smtp.ssl.protocols","TLSv1.2 TLSv1.3");props.put("mail.smtp.host",currentSmtpHost);props.put("mail.smtp.port",String.valueOf(p));props.put("mail.smtp.connectiontimeout","15000");props.put("mail.smtp.timeout","15000");Session session=Session.getInstance(props,new Authenticator(){protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(currentUser,currentPassword);}});MimeMessage message=new MimeMessage(session);message.setFrom(new InternetAddress(currentUser));message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to,true));message.setSubject(subject,"UTF-8");message.setText(body,"UTF-8");Transport.send(message);return "Mensaje enviado correctamente a "+to;}
}
