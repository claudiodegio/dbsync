package com.claudiodegio.dbsync.sample.db1;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.DbSyncApplication;
import com.claudiodegio.dbsync.sample.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class InsertNameActivity extends BaseActivity {

    @BindView(R.id.etName)
    EditText mETName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_name);
        app = (DbSyncApplication) getApplication();
        ButterKnife.bind(this);
    }


    @OnClick(R.id.btInsert)
    public void insertName(){

        SQLiteDatabase db = app.db1OpenHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();

        contentValues.put("NAME", mETName.getEditableText().toString());
        contentValues.put("DATE_CREATED", System.currentTimeMillis());

        db.insert("name", null, contentValues);

        finish();
    }
}
