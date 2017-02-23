package com.claudiodegio.dbsync.sample.db2;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.claudiodegio.dbsync.CloudProvider;
import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.GDriveCloudProvider;
import com.claudiodegio.dbsync.TableToSync;
import com.claudiodegio.dbsync.sample.BaseMainDbActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.core.TableViewerFragment;
import com.claudiodegio.dbsync.sample.db1.InsertNameActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import butterknife.OnClick;

public class MainDb2Activity extends BaseMainDbActivity  {

    private final static String TAG = "MainDb2Activity";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_db2);
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        CloudProvider gDriveProvider = new GDriveCloudProvider.Builder(this.getBaseContext())
                .setSyncFileByDriveId(mDriveId)
                .setGoogleApiClient(mGoogleApiClient)
                .build();

        dbSync = new DBSync.Builder(this.getBaseContext())
                .setCloudProvider(gDriveProvider)
                .setSQLiteDatabase(app.db2OpenHelper.getWritableDatabase())
                .setDataBaseName(app.db2OpenHelper.getDatabaseName())
                .addTable(new TableToSync.Builder("name").build())
                .addTable(new TableToSync.Builder("category").build())
                .setSchemaVersion(1)
                .build();
   }

    @Override
    public void onPostSync() {
    }


    @OnClick(R.id.btName)
    public void goToNames(){
        startActivity(new Intent(this, NamesActivity.class));
    }


    @OnClick(R.id.btCat)
    public void goToCats(){
        startActivity(new Intent(this, CategoriesActivity.class));
    }
}
