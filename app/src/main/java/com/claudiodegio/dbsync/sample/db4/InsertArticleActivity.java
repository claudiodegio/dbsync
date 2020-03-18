package com.claudiodegio.dbsync.sample.db4;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.annotation.Nullable;
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

public class InsertArticleActivity extends BaseActivity {

    @BindView(R.id.etName)
    EditText mETName;

    @BindView(R.id.etCategoryId)
    EditText mETCategoryId;

    @BindView(R.id.btUpdate)
    Button btUpdate;

    @BindView(R.id.btInsert)
    Button btInsert;

    @BindView(R.id.cbNull)
    CheckBox mCheckBoxNull;

    @BindView(R.id.cbNullCategory)
    CheckBox mCheckBoxNullCategoryId;
    private long mId = -1;

    SQLiteDatabase mDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_article);
        app = (DbSyncApplication) getApplication();
        mDB = app.db4OpenHelper.getWritableDatabase();

        ButterKnife.bind(this);

        mId = getIntent().getLongExtra("ID", -1);

    }


    @OnClick(R.id.btInsert)
    public void insertName(){

        mDB.insert("article", null, buildContentValue());

        finish();
    }

    @OnClick(R.id.btUpdate)
    public void updateName(){

        mDB.update("article", buildContentValue(), "_id = ? ", new String[] {Long.toString(mId)});

        finish();
    }

    private ContentValues buildContentValue(){

        ContentValues contentValues = new ContentValues();

        if (mCheckBoxNull.isChecked()) {
            contentValues.putNull("NAME");
        } else {
            contentValues.put("NAME", mETName.getEditableText().toString());
        }

        String categoryID = mETCategoryId.getEditableText().toString();

        if (mCheckBoxNullCategoryId.isChecked() || categoryID.isEmpty()) {
            contentValues.putNull("CATEGORY_ID");
        } else {
            contentValues.put("CATEGORY_ID", Integer.parseInt(categoryID));
        }
        contentValues.putNull("SEND_TIME");

        return contentValues;
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadById();
    }

    public void loadById() {
        Cursor cur;

        if (mId != -1) {
            cur = mDB.query("article", null, "_id = ?", new String[] {Long.toString(mId)}, null, null, null);
            cur.moveToFirst();
            mETName.setText(cur.getString(1));

            if (!cur.isNull(4)){
                mETCategoryId.setText(cur.getString(4));
            }

            cur.close();
            btInsert.setVisibility(View.GONE);
            btUpdate.setVisibility(View.VISIBLE);
        } else {
            btInsert.setVisibility(View.VISIBLE);
            btUpdate.setVisibility(View.GONE);
        }
    }
}
