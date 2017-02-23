package com.claudiodegio.dbsync.sample.db2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Db2OpenHelper extends SQLiteOpenHelper {

    static final private Logger log = LoggerFactory.getLogger(Db2OpenHelper.class);

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "db2.db";

    private static final String DDL_1 = "CREATE TABLE name (\n" +
            "    _id          INTEGER PRIMARY KEY,\n" +
            "    NAME         TEXT,\n" +
            "    SEND_TIME INTEGER,\n" +
            "    CLOUD_ID     TEXT    UNIQUE);";

    private static final String DDL_2 = "CREATE TABLE CATEGORY (\n" +
            "    _id          INTEGER PRIMARY KEY,\n" +
            "    NAME         TEXT,\n" +
            "    SEND_TIME INTEGER,\n" +
            "    CLOUD_ID     TEXT    UNIQUE);";

    public Db2OpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        log.info("onCreate");
        db.execSQL(DDL_1);
        db.execSQL(DDL_2);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public File getDatabaseLocation(){
        return new File("/data/data/com.claudiodegio.dbsync.sample/databases", DATABASE_NAME);
    }
}
