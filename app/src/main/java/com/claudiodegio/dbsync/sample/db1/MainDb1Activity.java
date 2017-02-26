package com.claudiodegio.dbsync.sample.db1;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.claudiodegio.dbsync.CloudProvider;
import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.GDriveCloudProvider;
import com.claudiodegio.dbsync.TableToSync;
import com.claudiodegio.dbsync.sample.BaseMainDbActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.core.TableViewerFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

public class MainDb1Activity extends BaseMainDbActivity implements TableViewerFragment.OnEditListener {

    private final static String TAG = "MainDb1Activity";

    TableViewerFragment mFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_db1);
        super.onCreate(savedInstanceState);


        mFragment = TableViewerFragment.newInstance("db1.db", "name");
        mFragment.setOnItemClicked(this);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.flFragment, mFragment, "TAG").commit();
    }

    @Override
    public void onItemEdit(long id, String[] data) {
        Intent intent = new Intent(this, InsertNameActivity.class);
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    @Override
    public void onAdd() {
        startActivity(new Intent(this, InsertNameActivity.class));
    }

    @Override
    public void onPostSync() {
        mFragment.reload();
    }

    @Override
    public void onPostSelectFile() {
        Log.d(TAG, "onPostSelectFile");
        CloudProvider gDriveProvider = new GDriveCloudProvider.Builder(this.getBaseContext())
                .setSyncFileByDriveId(mDriveId)
                .setGoogleApiClient(mGoogleApiClient)
                .build();

        dbSync = new DBSync.Builder(this.getBaseContext())
                .setCloudProvider(gDriveProvider)
                .setSQLiteDatabase(app.db1OpenHelper.getWritableDatabase())
                .setDataBaseName(app.db1OpenHelper.getDatabaseName())
                .addTable(new TableToSync.Builder("name").build())
                .setSchemaVersion(2)
                .build();
    }
}
