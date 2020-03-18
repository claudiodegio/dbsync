package com.claudiodegio.dbsync.sample.db2;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.core.TableViewerFragment;

public class CategoriesActivity extends BaseActivity implements TableViewerFragment.OnEditListener {

    TableViewerFragment mFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        mFragment = TableViewerFragment.newInstance("db2.db", "CATEGORY");
        mFragment.setOnItemClicked(this);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.flFragment, mFragment, "TAG").commit();
    }

    @Override
    public void onItemEdit(long id, String[] data) {
        Intent intent = new Intent(this, InsertCategoryActivity.class);
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    @Override
    public void onAdd() {
        startActivity(new Intent(this, InsertCategoryActivity.class));
    }

}
