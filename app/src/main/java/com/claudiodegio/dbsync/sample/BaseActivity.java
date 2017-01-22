package com.claudiodegio.dbsync.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;


public abstract class BaseActivity extends AppCompatActivity {

    protected DbSyncApplication app;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (DbSyncApplication) getApplication();
    }
}
