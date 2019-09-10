package com.example.android.rttsurvey;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;


public class RttDataBaseHelper extends SQLiteOpenHelper {

    public RttDataBaseHelper(Context context) {
        super(context, RttDatabaseContract.DATABASE_NAME , null, RttDatabaseContract.DATABASE_VERSION);
        //this.context = context;
    }

    @Override
    public void onCreate (SQLiteDatabase db){
        db.execSQL(RttDatabaseContract.Table_AP.CREATE_TABLE);
        //db.execSQL(RttDatabaseContract.Table_Data.CREATE_TABLE_PREFIX + "1" + RttDatabaseContract.Table_Data.CREATE_TABLE_SUFFIX);
    }

    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(RttDatabaseContract.Table_Data.DELETE_TABLE);
        onCreate(db);
    }

    @Override
    public void onOpen (SQLiteDatabase db) {
        int table_index = getTableIndex(db) + 1;
        db.execSQL(RttDatabaseContract.Table_Data.CREATE_TABLE_PREFIX + table_index + RttDatabaseContract.Table_Data.CREATE_TABLE_SUFFIX);
    }

    public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db,oldVersion,newVersion);
    }

    public static int checkAP (SQLiteDatabase db, String accessPoint) {
        Cursor cursor = db.rawQuery(RttDatabaseContract.Table_AP.CHECK_AP + '"' + accessPoint + "\")", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public static int getTableIndex (SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master AS tables WHERE TYPE = 'table'", null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getCount() - 2;
        } else {
            return 0;
        }
    }
}
