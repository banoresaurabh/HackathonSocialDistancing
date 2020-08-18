package com.example.android.bluetoothlegatt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbUtility extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "socialDistancing.db";
    public static final String WHITELISTED_DEVICES = "whitelisted_devices";
    public static final String ID = "id";
    public static final String DEVICE_MAC_ID = "device_mac_id";




    public DbUtility(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + WHITELISTED_DEVICES +"(id INTEGER PRIMARY KEY AUTOINCREMENT, device_mac_id TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+ WHITELISTED_DEVICES);
        onCreate(sqLiteDatabase);
    }

    public boolean insert(String device_mac_id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEVICE_MAC_ID, device_mac_id);
        long res = db.insert(WHITELISTED_DEVICES,null,contentValues);
        if(res == -1)return false;
        return true;
    }

    public Cursor getCred(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+WHITELISTED_DEVICES,null );
        return res;
    }

    public int delete(String device_mac_id){
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from "+ WHITELISTED_DEVICES +"where device_mac_id=" + device_mac_id);
        return db.delete(WHITELISTED_DEVICES,"device_mac_id = ?",new String[]{device_mac_id});
    }
}
