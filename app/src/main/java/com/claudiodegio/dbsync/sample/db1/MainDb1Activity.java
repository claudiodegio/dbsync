package com.claudiodegio.dbsync.sample.db1;


import android.app.Application;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.tablemanager.TableViewerFragment;

import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dino.dbinspector.activities.DbInspectorActivity;

public class MainDb1Activity extends BaseActivity implements TableViewerFragment.OnItemClicked {

    TableViewerFragment mFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db1_main);
        ButterKnife.bind(this);

        mFragment = TableViewerFragment.newInstance("db1.db", "name");
        mFragment.setOnItemClicked(this);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.flFragment, mFragment, "TAG").commit();
    }

    @OnClick(R.id.btToDbManager)
    public void goToDBManager(){
        startActivity(new Intent(this, DbInspectorActivity.class));
    }

    @OnClick(R.id.btInsertName)
    public void goInsertName(){
        startActivity(new Intent(this, InsertNameActivity.class));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mFragment.reload();
    }

    @Override
    public void onItemClicked(long id, String [] data) {
        Toast.makeText(this, "" + id, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, InsertNameActivity.class);
        intent.putExtra("ID", id);
        startActivity(intent);
    }
}
