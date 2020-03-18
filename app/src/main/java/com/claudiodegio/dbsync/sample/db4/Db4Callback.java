package com.claudiodegio.dbsync.sample.db4;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Db4Callback extends SupportSQLiteOpenHelper.Callback {

    static final private Logger log = LoggerFactory.getLogger(Db4Callback.class);

    public static final int DATABASE_VERSION = 1;

    private static final String DDL_CATEGORY = "CREATE TABLE category (\n" +
            "    _id       INTEGER PRIMARY KEY,\n" +
            "    NAME      TEXT,\n" +
            "    SEND_TIME INTEGER,\n" +
            "    CLOUD_ID  TEXT UNIQUE);";

    private static final String DDL_ARTICLE = "CREATE TABLE article (\n" +
            "    _id         INTEGER PRIMARY KEY,\n" +
            "    NAME        TEXT,\n" +
            "    SEND_TIME   INTEGER,\n" +
            "    CLOUD_ID    TEXT    UNIQUE,\n" +
            "    CATEGORY_ID INTEGER REFERENCES category (_id));";

    public Db4Callback() {
        super(DATABASE_VERSION);
    }


    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
        log.info("onCreate");
        db.execSQL(DDL_CATEGORY);
        db.execSQL(DDL_ARTICLE);
    }

    @Override
    public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion)  {
        onCreate(db);
    }
}
