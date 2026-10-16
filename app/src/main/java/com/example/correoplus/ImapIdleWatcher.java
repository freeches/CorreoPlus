package com.example.correoplus;

import android.content.Context;
import android.content.SharedPreferences;
import com.sun.mail.imap.IMAPFolder;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import java.util.Properties;

/** IDLE is active only while the app is foregrounded; WorkManager covers background polling. */
final class ImapIdleWatcher {
  private final Context context;
  private volatile boolean running;
  private volatile Store store;
  private volatile IMAPFolder folder;
  private Thread thread;
  ImapIdleWatcher(Context ctx){context=ctx.getApplicationContext();}
  void start(){if(running)return;running=true;thread=new Thread(this::loop,"CorreoPlus-IMAP-IDLE");thread.start();}
  void stop(){running=false;try{if(folder!=null&&folder.isOpen())folder.close(false);}catch(Exception ignored){}try{if(store!=null)store.close();}catch(Exception ignored){}if(thread!=null)thread.interrupt();}
  private void loop(){int delay=2000;
    while(running){try{
      SharedPreferences p=context.getSharedPreferences("correoplus_settings",Context.MODE_PRIVATE);
      String account=p.getString("account","");String password=CredentialVault.load(context,account);
      String host=p.getString("imap_host","");String user=p.getString("username","");
      if(password==null||host.isEmpty()||user.isEmpty())return;
      int port=Integer.parseInt(p.getString("imap_port","993"));String protocol=port==993?"imaps":"imap";
      Properties props=new Properties();String prefix="mail."+protocol;
      props.put(prefix+".connectiontimeout","15000");props.put(prefix+".timeout","90000");
      if(port!=993){props.put(prefix+".starttls.enable","true");props.put(prefix+".starttls.required","true");}
      store=Session.getInstance(props).getStore(protocol);store.connect(host,port,user,password);
      Folder candidate=store.getFolder("INBOX");candidate.open(Folder.READ_ONLY);
      if(!(candidate instanceof IMAPFolder))return;
      folder=(IMAPFolder)candidate;
      folder.addMessageCountListener(new MessageCountAdapter(){@Override public void messagesAdded(MessageCountEvent e){MailBackground.syncNow(context);}});
      delay=2000;
      while(running&&folder.isOpen())folder.idle();
    }catch(Exception ignored){if(!running)return;try{Thread.sleep(delay);}catch(InterruptedException ex){return;}delay=Math.min(60000,delay*2);}
    finally{try{if(folder!=null&&folder.isOpen())folder.close(false);}catch(Exception ignored){}try{if(store!=null)store.close();}catch(Exception ignored){}folder=null;store=null;}
    }
  }
}
