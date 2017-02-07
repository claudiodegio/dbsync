package com.claudiodegio.dbsync;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DBSync {

    final static private String TAG = "DBSync";

    final private CloudProvider mCloudProvider;
    final private SQLiteDatabase mDB;
    final private SqlLiteManager mManager;
    final private List<TableToSync> mTables;
    final private Context mCtx;
    final private String mDataBaseName;

    private DBSync(final Context ctx, final CloudProvider cloudProvider, final SQLiteDatabase db, final String dataBaseName, final List<TableToSync> tables){
        this.mCtx = ctx;
        this.mCloudProvider = cloudProvider;
        this.mDB = db;
        this.mTables = tables;
        this.mDataBaseName = dataBaseName;
        mManager = new SqlLiteManager(mDB);
    }

    // TODO fare la versione sincrona e async
    public SyncResult sync() {
        File tempFbFile = null;
        InputStream inputStream = null;
        DatabaseCounter counter;

        try {
            // Download the file from cloud
            inputStream = mCloudProvider.downloadFile();

            // Sync the database
            counter = syncDatabase(inputStream);

            /*// populateUUID
            populateUUID();

            // Write the database file
            tempFbFile = writeDateBaseFile();

            // Upload file to cloud
            mCloudProvider.uploadFile(tempFbFile);
*/
             // ALL OK
            return new SyncResult(new SyncStatus(SyncStatus.OK), counter);
        } catch (SyncException e) {
            return new SyncResult(e.getStatus());
        } catch (Exception e) {
            return new SyncResult(new SyncStatus(SyncStatus.ERROR, e.getMessage()));
        } finally {
            if (tempFbFile != null && tempFbFile.exists()) {
                Log.d(TAG, "delete db temp file:" + tempFbFile.getName());
            }
        }
    }


    private File writeDateBaseFile() throws SyncException {
        File tempDbFile;
        FileOutputStream outStream;
        DatabaseWriter writer;

        try {
            tempDbFile = File.createTempFile("database", ".json");

            Log.i(TAG, "Create tmp db file: " + tempDbFile.getAbsolutePath());

            // Open temp file
            outStream = new FileOutputStream(tempDbFile);
            writer = new JSonDatabaseWriter(outStream);

            // Write database start
            writer.writeDatabase(mDataBaseName, mTables.size());

            // Write tables
            for (TableToSync table : mTables) {
                serializeTable(table, writer);
            }

            // Close database
             writer.close();

            Log.i(TAG, "Created DB file with size: " + tempDbFile.length());
            return tempDbFile;
        } catch (Exception e) {
            throw new SyncException(SyncStatus.ERROR_WRITING_TMP_DB, e.getMessage());
        }
    }

    private void serializeTable(final TableToSync table, final DatabaseWriter writer) throws IOException{
        List<ColumnMetadata> columnsMetadata;
        Cursor cur = null;
        ColumnValue value = null;
        Record record;

        columnsMetadata = SqlLiteUtility.readTableMetadata(mDB, table.getName());

        try {
            cur = mDB.query(table.getName(), null, null, null, null, null, null);

            Log.i(TAG, "Write table:" + table.getName() + " records:" + cur.getCount());

            writer.writeTable(table.getName(), cur.getCount());

            while (cur.moveToNext()) {
                record = new Record();

                for (ColumnMetadata colMeta : columnsMetadata) {
                    if (!table.isColumnToIgnore(colMeta.getName())) {
                        value = SqlLiteUtility.getCursorColumnValue(cur, colMeta);
                        record.add(value);
                    }
                }

                writer.writeRecord(record);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    private void populateUUID(){

        // populate UUID TableToSync
        for (TableToSync table : mTables) {
            populateUUID(table);
        }
    }

    private void populateUUID(TableToSync table){
        String selection;
        Cursor cur = null;
        String uuid;
        int id;
        int rowCount;
        ContentValues contentValuesUpdate;
        Log.i(TAG, "start populateUUID for table:" +table.getName()+ " idColumn:" + table.getIdColumn() + " CloudIdColumn:" + table.getCloudIdColumn());

        selection = table.getCloudIdColumn() + " IS NULL OR " + table.getCloudIdColumn() + " = \"\"";

        try {
            cur = mDB.query(table.getName(), new String[]{table.getIdColumn()}, selection, null, null, null, null);

            rowCount = 0;
            contentValuesUpdate = new ContentValues();

            // TODO check uuid it'unique
            while(cur.moveToNext()) {
                id = cur.getInt(0);
                uuid = UUID.randomUUID().toString();

                Log.d(TAG, "assing uuid:" + uuid + " to id:" + id);
                // Update of cloud id
                contentValuesUpdate.put(table.getCloudIdColumn(), uuid);

                mDB.update(table.getName(), contentValuesUpdate, table.getIdColumn() + " = ?", new String[]{Integer.toString(id)});
                rowCount++;
            }

            Log.i(TAG, "end populateUUID rows updated:" + rowCount);
        } catch(Exception e) {
            throw new SyncException(SyncStatus.ERROR_GENERATE_UUID, "Error generating UUID message:" + e.getMessage());
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }


    /**
     * Funzion to start sync of cloud database ad local
     * @param inputStream
     */
    private DatabaseCounter syncDatabase(final InputStream inputStream) {
        JSonDatabaseReader reader = null;
        DatabaseCounter counter = null;

        try {
            // Create the new database reader
            reader = new JSonDatabaseReader(inputStream);

            // Start sync procedure with a the last timestamp
            counter = mManager.syncDatabase(reader, mTables, 0);
        } catch (IOException e) {
            // if the sync procedure generate an IOException i convert it
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return counter;
    }
    public static class Builder {

        private CloudProvider mCloudProvider;
        private SQLiteDatabase mDB;
        private String mDataBaseName;
        private List<TableToSync> mTables = new ArrayList<>();
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

        public Builder addTable(final TableToSync table) {
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
