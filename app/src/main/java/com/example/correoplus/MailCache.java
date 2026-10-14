package com.example.correoplus;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.util.ArrayList;

/** Private offline cache. Keys include account, folder, UIDVALIDITY and UID. */
public final class MailCache extends SQLiteOpenHelper {
    public static class Item {
        public final long uid;
        public final String from, subject, date, body;
        public final boolean seen;
        public Item(long uid,String from,String subject,String date,boolean seen,String body){this.uid=uid;this.from=from;this.subject=subject;this.date=date;this.seen=seen;this.body=body;}
    }
    public MailCache(Context context){super(context,"mail_cache.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE messages(account TEXT NOT NULL, folder TEXT NOT NULL, validity INTEGER NOT NULL, uid INTEGER NOT NULL, sender TEXT, subject TEXT, date_text TEXT, seen INTEGER, body TEXT, PRIMARY KEY(account,folder,validity,uid))");
        db.execSQL("CREATE TABLE folders(account TEXT NOT NULL, folder TEXT NOT NULL, validity INTEGER NOT NULL, PRIMARY KEY(account,folder))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
    public synchronized void saveHeaders(String account,String folder,long validity,ArrayList<Item> items){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            long previous=-1;
            try(Cursor c=db.rawQuery("SELECT validity FROM folders WHERE account=? AND folder=?",new String[]{account,folder})){if(c.moveToFirst())previous=c.getLong(0);}
            if(previous!=-1&&previous!=validity)db.delete("messages","account=? AND folder=?",new String[]{account,folder});
            ContentValues fv=new ContentValues();fv.put("account",account);fv.put("folder",folder);fv.put("validity",validity);db.insertWithOnConflict("folders",null,fv,SQLiteDatabase.CONFLICT_REPLACE);
            for(Item i:items){ContentValues v=new ContentValues();v.put("account",account);v.put("folder",folder);v.put("validity",validity);v.put("uid",i.uid);v.put("sender",i.from);v.put("subject",i.subject);v.put("date_text",i.date);v.put("seen",i.seen?1:0);
                // Update headers without destroying any previously downloaded body.
                db.insertWithOnConflict("messages",null,v,SQLiteDatabase.CONFLICT_IGNORE);
                ContentValues updates=new ContentValues();updates.put("sender",i.from);updates.put("subject",i.subject);updates.put("date_text",i.date);updates.put("seen",i.seen?1:0);
                db.update("messages",updates,"account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity),String.valueOf(i.uid)});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private long validity(String account,String folder){try(Cursor c=getReadableDatabase().rawQuery("SELECT validity FROM folders WHERE account=? AND folder=?",new String[]{account,folder})){return c.moveToFirst()?c.getLong(0):-1;}}
    public synchronized ArrayList<Item> list(String account,String folder){ArrayList<Item> out=new ArrayList<>();long val=validity(account,folder);try(Cursor c=getReadableDatabase().rawQuery("SELECT uid,sender,subject,date_text,seen,body FROM messages WHERE account=? AND folder=? AND validity=? ORDER BY uid DESC LIMIT 100",new String[]{account,folder,String.valueOf(val)})){while(c.moveToNext())out.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0,c.getString(5)));}return out;}
    public synchronized Item get(String account,String folder,long uid){for(Item i:list(account,folder))if(i.uid==uid)return i;return null;}
    public synchronized void saveBody(String account,String folder,long uid,String body){ContentValues v=new ContentValues();v.put("body",body);getWritableDatabase().update("messages",v,"account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity(account,folder)),String.valueOf(uid)});}
}
