package com.claudiodegio.dbsync.sample.db4;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.claudiodegio.dbsync.provider.CloudProvider;
import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.provider.GDriveCloudProvider;
import com.claudiodegio.dbsync.TableToSync;
import com.claudiodegio.dbsync.sample.BaseMainDbActivity;
import com.claudiodegio.dbsync.sample.R;

import butterknife.OnClick;

public class MainDb4Activity extends BaseMainDbActivity  {

    private final static String TAG = "MainDb4Activity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_db4);
        super.onCreate(savedInstanceState);
   }

    @Override
    public void onPostSync() {
    }

    @Override
    public void onPostSelectFile() {

        /*CloudProvider gDriveProvider = new GDriveCloudProvider.Builder(this.getBaseContext())
                .setSyncFileByDriveId(mDriveId)
                .setDriveResourceClient(mDriveResourceClient)
                .build();


        TableToSync tableCategory = new TableToSync.Builder("CATEGORY")
                .build();

        TableToSync tableArticle = new TableToSync.Builder("ARTICLE")
                .addJoinTable(tableCategory, "CATEGORY_ID")
                .build();

        dbSync = new DBSync.Builder(this.getBaseContext())
                .setCloudProvider(gDriveProvider)
                .setSQLiteDatabase(app.db4OpenHelper.getWritableDatabase())
                .setDataBaseName(app.db4OpenHelper.getDatabaseName())
                .addTable(tableCategory)
                .addTable(tableArticle)
                .setSchemaVersion(1)
                .build();
*/

    }


    @OnClick(R.id.btArticle)
    public void goToNames(){
        startActivity(new Intent(this, ArticlesActivity.class));
    }


    @OnClick(R.id.btCat)
    public void goToCats(){
        startActivity(new Intent(this, CategoriesActivity.class));
    }
}
