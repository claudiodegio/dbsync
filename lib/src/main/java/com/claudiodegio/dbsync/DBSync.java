package com.claudiodegio.dbsync;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DBSync {

    final static private String TAG = "DBSync";

    final private CloudProvider mCloudProvider;
    final private SQLiteDatabase mDB;
    final private List<Table> mTables;
    final private Context mCtx;
    final private String mDataBaseName;

    private DBSync(final Context ctx, final CloudProvider cloudProvider, final SQLiteDatabase db, final String dataBaseName, final List<Table> tables){
        this.mCtx = ctx;
        this.mCloudProvider = cloudProvider;
        this.mDB = db;
        this.mTables = tables;
        this.mDataBaseName = dataBaseName;
    }

    // TODO fare la versione sincrona e async
    public SyncResult sync() {

        // download

        // writeDateBaseFile

        try {
            writeDateBaseFile();

            // ALL OK
            return new SyncResult(new Status(Status.OK));
        } catch (SyncException e) {
            return new SyncResult(e.getStatus());
        } catch (Exception e) {
            return new SyncResult(new Status(Status.ERROR, e.getMessage()));
        }
    }


    private void writeDateBaseFile() throws SyncException {
        File tempDbFile;
        FileOutputStream outStream;
        DatabaseWriter writer;

        try {
            tempDbFile = File.createTempFile("database", ".json");

            Log.i(TAG, "Create tmp db file: " + tempDbFile.getAbsolutePath());

            // Open temp file
            outStream = new FileOutputStream(tempDbFile);
            writer = new JsonDatabaseWriter(outStream);

            // Write database start
            writer.writeStartDatabase(mDataBaseName, mTables.size());

            // Write tables
            for (Table table : mTables) {
                serializeTable(table, writer);
            }

            // Close database
            writer.writeEndDatabase();
            writer.close();

            Log.i(TAG, "Created DB file with size: " + tempDbFile.length());
        } catch (Exception e) {
            throw new SyncException(Status.ERROR_WRITING_TMP_DB, e.getMessage());
        }
    }

    private void serializeTable(final Table table, final DatabaseWriter writer) throws IOException{
        List<ColumnMetadata> columnsMetadata;
        Cursor cur;
        ColumnValue value = null;
        Record record;

        columnsMetadata = SqlLiteUtility.readTableMetadata(mDB, table.getName());

        cur = mDB.query(table.getName(), null, null, null, null, null, null);

        Log.i(TAG, "Write table:" + table.getName()+ " records:" + cur.getCount());

        writer.writeStartTable(table.getName(), cur.getCount());

        while (cur.moveToNext()) {
            record = new Record();

            for (ColumnMetadata colMeta : columnsMetadata) {
                if (!table.isColumnToIgnore(colMeta.getName())){

                    // valore, nome, typo dato
                    switch (colMeta.getType()) {
                        case ColumnMetadata.TYPE_INTEGER:
                            value = new ColumnValue(SqlLiteUtility.getCursorLong(cur, colMeta.getName()), colMeta);
                            break;

                        case ColumnMetadata.TYPE_TEXT:
                             value = new ColumnValue(SqlLiteUtility.getCursorString(cur, colMeta.getName()), colMeta);
                            break;
                    }

                    record.add(value);
                }
            }

            writer.writeRecord(record);
        }

        cur.close();
        writer.writeEndTable();
    }

    public static class Builder {

        private CloudProvider mCloudProvider;
        private SQLiteDatabase mDB;
        private String mDataBaseName;
        private List<Table> mTables = new ArrayList<>();
        private Context mCtx;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        public Builder setCloudProvider(final CloudProvider cloudProvider) {
            mCloudProvider = cloudProvider;
            return this;
        }

        public Builder setSQLiteDatabase(final SQLiteDatabase db) {
            this.mDB = db;
            return this;
        }

        public Builder addTable(final Table table) {
            this.mTables.add(table);
            return this;
        }


        public Builder setDataBaseName(String dataBaseName) {
            this.mDataBaseName = dataBaseName;
            return this;
        }

        public DBSync build(){
            return new DBSync(mCtx, mCloudProvider, mDB, mDataBaseName, mTables);
        }
    }
}
