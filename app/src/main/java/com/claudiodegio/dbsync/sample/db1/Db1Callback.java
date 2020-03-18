package com.claudiodegio.dbsync.sample.db1;


import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Db1Callback extends SupportSQLiteOpenHelper.Callback {

    static final private Logger log = LoggerFactory.getLogger(Db1Callback.class);

    public static final int DATABASE_VERSION = 1;

    private static final String DDL = "CREATE TABLE name (\n" +
            "    _id          INTEGER PRIMARY KEY,\n" +
            "    NAME         TEXT,\n" +
            "    SEND_TIME INTEGER,\n" +
            "    CLOUD_ID     TEXT    UNIQUE);";

    public Db1Callback() {
        super(DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
        log.info("onCreate");
        db.execSQL(DDL);
    }

    @Override
    public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}
