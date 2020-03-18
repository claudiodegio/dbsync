package com.claudiodegio.dbsync.sample;

import androidx.multidex.MultiDexApplication;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import com.claudiodegio.dbsync.sample.db1.Db1Callback;
import com.claudiodegio.dbsync.sample.db2.Db2Callback;
import com.claudiodegio.dbsync.sample.db3.Db3Callback;
import com.claudiodegio.dbsync.sample.db4.Db4Callback;
import com.claudiodegio.dbsync.sample.db5.Db5Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;


public class DbSyncApplication extends MultiDexApplication {
    static private Logger log;

    public SupportSQLiteOpenHelper db1OpenHelper;
    public SupportSQLiteOpenHelper db2OpenHelper;
    public SupportSQLiteOpenHelper db3OpenHelper;
    public SupportSQLiteOpenHelper db4OpenHelper;
    public SupportSQLiteOpenHelper db5OpenHelper;

    @Override
    public void onCreate() {
        SupportSQLiteOpenHelper.Configuration  configuration;
        super.onCreate();

        StaticLoggerBinder.init(this);
        log = LoggerFactory.getLogger(DbSyncApplication.class);

        log.info("onCreate");


        configuration = SupportSQLiteOpenHelper.Configuration.builder(this)
                .name("db1")
                .callback(new Db1Callback())
                .build();
        db1OpenHelper = new FrameworkSQLiteOpenHelperFactory()
                .create(configuration);
        db1OpenHelper.getReadableDatabase();

        configuration = SupportSQLiteOpenHelper.Configuration.builder(this)
                .name("db2")
                .callback(new Db2Callback())
                .build();
        db2OpenHelper = new FrameworkSQLiteOpenHelperFactory()
                .create(configuration);
        db2OpenHelper.getReadableDatabase();

        configuration = SupportSQLiteOpenHelper.Configuration.builder(this)
                .name("db3")
                .callback(new Db3Callback())
                .build();
        db3OpenHelper = new FrameworkSQLiteOpenHelperFactory()
                .create(configuration);
        db3OpenHelper.getReadableDatabase();

        configuration = SupportSQLiteOpenHelper.Configuration.builder(this)
                .name("db4")
                .callback(new Db4Callback())
                .build();
        db4OpenHelper = new FrameworkSQLiteOpenHelperFactory()
                .create(configuration);
        db4OpenHelper.getReadableDatabase();


        configuration = SupportSQLiteOpenHelper.Configuration.builder(this)
                .name("db5")
                .callback(new Db5Callback())
                .build();
        db5OpenHelper = new FrameworkSQLiteOpenHelperFactory()
                .create(configuration);
        db5OpenHelper.getReadableDatabase();
    }
}
