package com.claudiodegio.dbsync.sample.db3;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Db3OpenHelper extends SQLiteOpenHelper {

    static final private Logger log = LoggerFactory.getLogger(Db3OpenHelper.class);

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "db3.db";

    private static final String DDL = "CREATE TABLE name (\n" +
            "    _id          INTEGER PRIMARY KEY,\n" +
            "    NAME         TEXT,\n" +
            "    SEND_TIME INTEGER,\n" +
            "    FILTER INTEGER,\n" +
            "    CLOUD_ID     TEXT    UNIQUE);";

    public Db3OpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        log.info("onCreate");
        db.execSQL(DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public File getDatabaseLocation(){
        return new File("/data/data/com.claudiodegio.dbsync.sample/databases", DATABASE_NAME);
    }
}
