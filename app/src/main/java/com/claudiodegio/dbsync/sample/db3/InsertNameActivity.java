package com.claudiodegio.dbsync.sample.db3;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.DbSyncApplication;
import com.claudiodegio.dbsync.sample.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_NONE;

public class InsertNameActivity extends BaseActivity {

    @BindView(R.id.etName)
    EditText mETName;

    @BindView(R.id.etFilter)
    EditText mETFilter;

    @BindView(R.id.btUpdate)
    Button btUpdate;

    @BindView(R.id.btInsert)
    Button btInsert;

    @BindView(R.id.cbNull)
    CheckBox mCheckBoxNull;
    private long mId = -1;

    SupportSQLiteDatabase mDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_name_db3);
        app = (DbSyncApplication) getApplication();
        mDB = app.db3OpenHelper.getWritableDatabase();
        ButterKnife.bind(this);

        mId = getIntent().getLongExtra("ID", -1);

    }


    @OnClick(R.id.btInsert)
    public void insertName(){

        ContentValues contentValues = new ContentValues();

        if (mCheckBoxNull.isChecked()) {
            contentValues.put("NAME", (String)null);
        } else {
            contentValues.put("NAME", mETName.getEditableText().toString());
        }
        contentValues.put("FILTER", Integer.parseInt(mETFilter.getEditableText().toString()));
        contentValues.putNull("SEND_TIME");

        mDB.insert("name", CONFLICT_NONE, contentValues);

        finish();
    }

    @OnClick(R.id.btUpdate)
    public void updateName(){

        ContentValues contentValues = new ContentValues();

        if (mCheckBoxNull.isChecked()) {
            contentValues.put("NAME", (String)null);
        } else {
            contentValues.put("NAME", mETName.getEditableText().toString());
        }

        contentValues.put("FILTER", Integer.parseInt(mETFilter.getEditableText().toString()));
        contentValues.putNull("SEND_TIME");

        mDB.update("name", CONFLICT_NONE, contentValues, "_id = ? ", new String[] {Long.toString(mId)});

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadById();
    }

    public void loadById() {
        Cursor cur;

        if (mId != -1) {
            cur = mDB.query("SELECT * FROM name WHERE _id = ?", new String[] {Long.toString(mId)});
            cur.moveToFirst();
            mETName.setText(cur.getString(1));
            cur.close();
            btInsert.setVisibility(View.GONE);
            btUpdate.setVisibility(View.VISIBLE);
        } else {
            btInsert.setVisibility(View.VISIBLE);
            btUpdate.setVisibility(View.GONE);
        }
    }
}
