package com.claudiodegio.dbsync.sample.db5;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Db5Callback extends SupportSQLiteOpenHelper.Callback {

    static final private Logger log = LoggerFactory.getLogger(Db5Callback.class);

    public static final int DATABASE_VERSION = 1;

    private static final String DDL = "CREATE TABLE category (\n" +
            "    _id          INTEGER PRIMARY KEY,\n" +
            "    NAME         TEXT,\n" +
            "    SEND_TIME INTEGER,\n" +
            "    CLOUD_ID     TEXT    UNIQUE);";

    public Db5Callback() {
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
