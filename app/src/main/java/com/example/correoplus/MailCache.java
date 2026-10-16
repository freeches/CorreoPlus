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
        public final int attachmentState;
        public Item(long uid,String from,String subject,String date,boolean seen,String body){this.uid=uid;this.from=from;this.subject=subject;this.date=date;this.seen=seen;this.body=body;this.attachmentState=-1;}
        public Item(long uid,String from,String subject,String date,boolean seen,String body,int attachmentState){this.uid=uid;this.from=from;this.subject=subject;this.date=date;this.seen=seen;this.body=body;this.attachmentState=attachmentState;}
    }
    public MailCache(Context context){super(context,"mail_cache.db",null,2);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE messages(account TEXT NOT NULL, folder TEXT NOT NULL, validity INTEGER NOT NULL, uid INTEGER NOT NULL, sender TEXT, subject TEXT, date_text TEXT, seen INTEGER, body TEXT, has_attachments INTEGER NOT NULL DEFAULT -1, PRIMARY KEY(account,folder,validity,uid))");
        db.execSQL("CREATE TABLE folders(account TEXT NOT NULL, folder TEXT NOT NULL, validity INTEGER NOT NULL, PRIMARY KEY(account,folder))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2)db.execSQL("ALTER TABLE messages ADD COLUMN has_attachments INTEGER NOT NULL DEFAULT -1");}
    public synchronized void saveHeaders(String account,String folder,long validity,ArrayList<Item> items){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            long previous=-1;
            try(Cursor c=db.rawQuery("SELECT validity FROM folders WHERE account=? AND folder=?",new String[]{account,folder})){if(c.moveToFirst())previous=c.getLong(0);}
            if(previous!=-1&&previous!=validity)db.delete("messages","account=? AND folder=?",new String[]{account,folder});
            ContentValues fv=new ContentValues();fv.put("account",account);fv.put("folder",folder);fv.put("validity",validity);db.insertWithOnConflict("folders",null,fv,SQLiteDatabase.CONFLICT_REPLACE);
            for(Item i:items){ContentValues v=new ContentValues();v.put("account",account);v.put("folder",folder);v.put("validity",validity);v.put("uid",i.uid);v.put("sender",i.from);v.put("subject",i.subject);v.put("date_text",i.date);v.put("seen",i.seen?1:0);v.put("has_attachments",i.attachmentState);
                // Update headers without destroying any previously downloaded body.
                db.insertWithOnConflict("messages",null,v,SQLiteDatabase.CONFLICT_IGNORE);
                ContentValues updates=new ContentValues();updates.put("sender",i.from);updates.put("subject",i.subject);updates.put("date_text",i.date);updates.put("seen",i.seen?1:0);
                db.update("messages",updates,"account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity),String.valueOf(i.uid)});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private long validity(String account,String folder){try(Cursor c=getReadableDatabase().rawQuery("SELECT validity FROM folders WHERE account=? AND folder=?",new String[]{account,folder})){return c.moveToFirst()?c.getLong(0):-1;}}
    public synchronized ArrayList<Item> list(String account,String folder){ArrayList<Item> out=new ArrayList<>();long val=validity(account,folder);try(Cursor c=getReadableDatabase().rawQuery("SELECT uid,sender,subject,date_text,seen,body,has_attachments FROM messages WHERE account=? AND folder=? AND validity=? ORDER BY uid DESC LIMIT 100 OFFSET 0",new String[]{account,folder,String.valueOf(val)})){while(c.moveToNext())out.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0,c.getString(5),c.getInt(6)));}return out;}
    public synchronized int count(String account,String folder){
        long val=validity(account,folder);
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM messages WHERE account=? AND folder=? AND validity=?",new String[]{account,folder,String.valueOf(val)})){return c.moveToFirst()?c.getInt(0):0;}
    }
    public synchronized ArrayList<Item> page(String account,String folder,int offset,int limit){
        ArrayList<Item> out=new ArrayList<>();long val=validity(account,folder);
        try(Cursor c=getReadableDatabase().rawQuery("SELECT uid,sender,subject,date_text,seen,body,has_attachments FROM messages WHERE account=? AND folder=? AND validity=? ORDER BY uid DESC LIMIT ? OFFSET ?",new String[]{account,folder,String.valueOf(val),String.valueOf(Math.max(1,Math.min(limit,100))),String.valueOf(Math.max(0,offset))})){
            while(c.moveToNext())out.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0,c.getString(5),c.getInt(6)));
        }return out;
    }
    public synchronized ArrayList<Item> advancedSearch(String account,String folder,String sender,String subject,String bodyTerm,String after,boolean attachmentsOnly,boolean unreadOnly,int offset){
        ArrayList<Item> out=new ArrayList<>();long val=validity(account,folder);
        StringBuilder sql=new StringBuilder("SELECT uid,sender,subject,date_text,seen,body,has_attachments FROM messages WHERE account=? AND folder=? AND validity=?");
        ArrayList<String> args=new ArrayList<>();args.add(account);args.add(folder);args.add(String.valueOf(val));
        if(!sender.isEmpty()){sql.append(" AND sender LIKE ? ESCAPE '\\'");args.add("%"+escapeLike(sender)+"%");}
        if(!subject.isEmpty()){sql.append(" AND subject LIKE ? ESCAPE '\\'");args.add("%"+escapeLike(subject)+"%");}
        if(!bodyTerm.isEmpty()){sql.append(" AND body LIKE ? ESCAPE '\\'");args.add("%"+escapeLike(bodyTerm)+"%");}
        if(attachmentsOnly)sql.append(" AND has_attachments=1");
        if(unreadOnly)sql.append(" AND seen=0");
        sql.append(" ORDER BY uid DESC LIMIT 100 OFFSET ?");args.add(String.valueOf(Math.max(0,offset)));
        try(Cursor c=getReadableDatabase().rawQuery(sql.toString(),args.toArray(new String[0]))){while(c.moveToNext()){
            Item item=new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0,c.getString(5),c.getInt(6));
            if(!after.isEmpty()){try{java.util.Date date=new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",java.util.Locale.ENGLISH).parse(item.date);if(date==null||date.before(new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).parse(after)))continue;}catch(Exception ignored){continue;}}
            out.add(item);
        }}return out;
    }
    private String escapeLike(String s){return s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
    public synchronized Item get(String account,String folder,long uid){
        long val=validity(account,folder);
        try(Cursor c=getReadableDatabase().rawQuery("SELECT uid,sender,subject,date_text,seen,body,has_attachments FROM messages WHERE account=? AND folder=? AND validity=? AND uid=? LIMIT 1",new String[]{account,folder,String.valueOf(val),String.valueOf(uid)})){
            return c.moveToFirst()?new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0,c.getString(5),c.getInt(6)):null;
        }
    }
    public synchronized void remove(String account,String folder,long uid){
        getWritableDatabase().delete("messages","account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity(account,folder)),String.valueOf(uid)});
    }
    public synchronized void saveBody(String account,String folder,long uid,String body){ContentValues v=new ContentValues();v.put("body",body);getWritableDatabase().update("messages",v,"account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity(account,folder)),String.valueOf(uid)});}
    public synchronized void updateAttachments(String account,String folder,long uid,boolean hasAttachments){ContentValues v=new ContentValues();v.put("has_attachments",hasAttachments?1:0);getWritableDatabase().update("messages",v,"account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity(account,folder)),String.valueOf(uid)});}
    public synchronized long maxUid(String account,String folder,long validity){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT MAX(uid) FROM messages WHERE account=? AND folder=? AND validity=?",new String[]{account,folder,String.valueOf(validity)})){
            return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;
        }
    }
    public synchronized void updateSeen(String account,String folder,long validity,long uid,boolean seen){
        ContentValues values=new ContentValues();values.put("seen",seen?1:0);
        getWritableDatabase().update("messages",values,"account=? AND folder=? AND validity=? AND uid=?",new String[]{account,folder,String.valueOf(validity),String.valueOf(uid)});
    }
    public synchronized ArrayList<Item> search(String account,String folder,String query){
        ArrayList<Item> out=new ArrayList<>();long val=validity(account,folder);
        String term="%"+query.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        try(Cursor c=getReadableDatabase().rawQuery("SELECT uid,sender,subject,date_text,seen,body,has_attachments FROM messages WHERE account=? AND folder=? AND validity=? AND (sender LIKE ? ESCAPE '\\' OR subject LIKE ? ESCAPE '\\' OR body LIKE ? ESCAPE '\\') ORDER BY uid DESC LIMIT 100",new String[]{account,folder,String.valueOf(val),term,term,term})){
            while(c.moveToNext())out.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0,c.getString(5),c.getInt(6)));
        }
        return out;
    }
}
