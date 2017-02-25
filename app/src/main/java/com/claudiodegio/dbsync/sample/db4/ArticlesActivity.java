package com.claudiodegio.dbsync.sample.db4;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.core.TableViewerFragment;


public class ArticlesActivity extends BaseActivity implements TableViewerFragment.OnEditListener {

    TableViewerFragment mFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        mFragment = TableViewerFragment.newInstance("db4.db", "article");

        mFragment.setOnItemClicked(this);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.flFragment, mFragment, "TAG").commit();
    }

    @Override
    public void onItemEdit(long id, String[] data) {
        Intent intent = new Intent(this, InsertArticleActivity.class);

        intent.putExtra("ID", id);
        startActivity(intent);
    }

    @Override
    public void onAdd() {
        startActivity(new Intent(this, InsertArticleActivity.class));
    }
}
