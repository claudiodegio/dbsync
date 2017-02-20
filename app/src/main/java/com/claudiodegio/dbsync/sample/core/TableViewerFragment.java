package com.claudiodegio.dbsync.sample.core;

import android.app.Fragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.claudiodegio.dbsync.sample.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.codecrafters.tableview.TableDataAdapter;
import de.codecrafters.tableview.TableHeaderAdapter;
import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.listeners.TableDataClickListener;
import de.codecrafters.tableview.model.TableColumnDpWidthModel;
import de.codecrafters.tableview.toolkit.SimpleTableDataAdapter;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;


public class TableViewerFragment extends Fragment implements TableDataClickListener {

    static final private Logger log = LoggerFactory.getLogger(TableViewerFragment.class);

    final private static int PAGE_SIZE = 5;

    @BindView(R.id.tvTable)
    TableView mTableView;

    @BindView(R.id.tvPage)
    TextView mTVPage;

    String mDbName;
    String mTableName;

    @BindView(R.id.btNext)
    Button btNext;
    @BindView(R.id.btPrev)
    Button btPrev;

    SQLiteDatabase mDB;

    private int mCurrentPage = 0;
    private int mNumPages = 0;

    private OnEditListener mOnItemClicked;

    public static TableViewerFragment newInstance(String dbName, String tableName) {
        
        Bundle args = new Bundle();

        args.putString("db_name", dbName);
        args.putString("table_name", tableName);

        TableViewerFragment fragment = new TableViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbName = getArguments().getString("db_name");
        mTableName = getArguments().getString("table_name");

        openDatabase();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_table_view, container, false);

        ButterKnife.bind(this, view);

        mTableView.addDataClickListener(this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        initHeader();
        countPages();
        mCurrentPage = 1;
        showPage(mCurrentPage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDB != null) {
            mDB.close();
        }
    }

    private void openDatabase(){
        log.info("open database {}", mDbName);

        File dbFile = new File("/data/data/com.claudiodegio.dbsync.sample/databases", mDbName);

        mDB = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private void initHeader(){
        TableHeaderAdapter tableHeaderAdapter;
        Cursor cursor = mDB.query(mTableName, null, " 1 = 0", null, null, null, null);

        tableHeaderAdapter = new SimpleTableHeaderAdapter(getActivity(), cursor.getColumnNames());

        mTableView.setHeaderAdapter(tableHeaderAdapter);
        cursor.close();

        int headerCount = cursor.getColumnNames().length;

        TableColumnDpWidthModel  model = new TableColumnDpWidthModel(getActivity(), headerCount, 200);
        model.setColumnWidth(0, 50);

        mTableView.setColumnModel(model);
    }

    private void showPage(int page){
        log.info("Show Page {}", page);

        String sqlSelect = "SELECT * FROM " + mTableName + " LIMIT " + (page - 1) * PAGE_SIZE + ", " + PAGE_SIZE;

        Cursor selectCursor =  mDB.rawQuery(sqlSelect, null);

        List<String[]> list = new ArrayList<>();

        while (selectCursor.moveToNext()) {
            String [] records = new String[selectCursor.getColumnCount()];

            for (int i = 0; i < selectCursor.getColumnCount(); ++i) {
                if (!selectCursor.isNull(i)) {
                    records[i] = selectCursor.getString(i);
                } else {
                    records[i] = "[NULL]";
                }
            }

            list.add(records);
        }

        TableDataAdapter tableDataAdapter = new SimpleTableDataAdapter(getActivity(), list);
        mTableView.setDataAdapter(tableDataAdapter);

        selectCursor.close();

        updatePage(page);
        updateButton(page);
    }

    private void countPages(){
        String sqlCount = "SELECT COUNT(*) FROM " + mTableName;

        Cursor countCursor =  mDB.rawQuery(sqlCount, null);

        countCursor.moveToFirst();

        int count = countCursor.getInt(0);
        countCursor.close();

        mNumPages = (int) Math.ceil((float)count / (float)PAGE_SIZE);

        log.info("Found {} records, pages {}", count, mNumPages);
    }

    private void updatePage(int page){
        mTVPage.setText( page + "/" + mNumPages);
    }

    private void updateButton(int page){
        if (mNumPages == 1) {
            btNext.setEnabled(false);
            btPrev.setEnabled(false);
        } else {
            if (page == 1) {
                btNext.setEnabled(true);
                btPrev.setEnabled(false);
            } else if (page == mNumPages){
                btNext.setEnabled(false);
                btPrev.setEnabled(true);
            } else {
                btNext.setEnabled(true);
                btPrev.setEnabled(true);
            }
        }
    }

    @OnClick(R.id.btNext)
    public void buttonNext(){
        mCurrentPage++;
        showPage(mCurrentPage);
    }

    @OnClick(R.id.btPrev)
    public void buttonPrev(){
        mCurrentPage--;
        showPage(mCurrentPage);
    }

    @OnClick(R.id.btAdd)
    public void buttonAdd(){
        if (mOnItemClicked != null) {
            mOnItemClicked.onAdd();
        }
    }

    public void reload(){
        countPages();
        mCurrentPage = 1;
        showPage(mCurrentPage);
    }

    @Override
    public void onDataClicked(int rowIndex, Object clickedData) {
        String [] data = (String[]) clickedData;

        long id = Long.parseLong(data[0]);

        if (mOnItemClicked != null) {
            mOnItemClicked.onItemEdit(id, data);
        }
    }

    @OnClick(R.id.btAdd)
    public void onBtAdd(){

    }

    public interface OnEditListener {
        void onItemEdit(long id, String [] data );
        void onAdd();
    }


    public void setOnItemClicked(OnEditListener mOnItemClicked) {
        this.mOnItemClicked = mOnItemClicked;
    }
}
