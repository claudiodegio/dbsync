package com.claudiodegio.dbsync.sample.db1;


import android.app.Application;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.tablemanager.TableViewerFragment;

import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dino.dbinspector.activities.DbInspectorActivity;
import im.dino.dbinspector.fragments.TableFragment;

public class MainDb1Activity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db1_main);
        ButterKnife.bind(this);

        TableViewerFragment fragment = TableViewerFragment.newInstance();

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.flFragment, fragment, "TAG").commit();
    }

    @OnClick(R.id.btToDbManager)
    public void goToDBManager(){
        startActivity(new Intent(this, DbInspectorActivity.class));
    }

    @OnClick(R.id.btInsertName)
    public void goInsertName(){
        startActivity(new Intent(this, InsertNameActivity.class));
    }
}
