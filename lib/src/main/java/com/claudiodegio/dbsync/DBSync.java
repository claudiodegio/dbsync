package com.claudiodegio.dbsync;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.IntDef;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class DBSync {

    final static private String TAG = "DBSync";

    final private CloudProvider mCloudProvider;
    final private SQLiteDatabase mDB;
    final private SqlLiteManager mManager;
    final private List<TableToSync> mTables;
    final private Context mCtx;
    final private String mDataBaseName;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SERVER, CLIENT})
    public @interface ConflictPolicy {}
    static final int SERVER = 1;
    static final int CLIENT = 2;

    private DBSync(final Context ctx, final CloudProvider cloudProvider, final SQLiteDatabase db, final String dataBaseName, final List<TableToSync> tables, @ConflictPolicy int conflictPolicy, int thresholdSeconds, int schemaVersion){
        this.mCtx = ctx;
        this.mCloudProvider = cloudProvider;
        this.mDB = db;
        this.mTables = tables;
        this.mDataBaseName = dataBaseName;
        this.mManager = new SqlLiteManager(mDB, dataBaseName, tables, conflictPolicy, thresholdSeconds, schemaVersion);
    }

    // TODO fare la versione sincrona e async
    public SyncResult sync() {
        File tempFbFile = null;
        InputStream inputStream = null;
        DatabaseCounter counter;
        long lastSyncTimestamp;
        long currentTimestamp;
        int uploadStatus;
        int attempt;

        try {
            // Read the last time stamp
            lastSyncTimestamp = getLastSyncTimestamp();
            // Generate the new sync timestamp
            currentTimestamp = System.currentTimeMillis();
            counter = new DatabaseCounter();

            attempt = 0;
            while (true) {
                attempt++;
                Log.i(TAG, "start sync try " + attempt);
                // Download the file from cloud
                inputStream = mCloudProvider.downloadFile();

                // Sync the database
                if (inputStream != null) {
                    syncDatabase(inputStream, counter, lastSyncTimestamp, currentTimestamp);
                }
                // populateUUID
                mManager.populateUUID();

                // Generate the new sync timestamp
                mManager.populateSendTime(currentTimestamp);

                // Write the database file
                tempFbFile = writeDateBaseFile();

                // Upload file to cloud
                uploadStatus = mCloudProvider.uploadFile(tempFbFile);

                if (uploadStatus == CloudProvider.UPLOAD_OK) {
                    break;
                }

                // Delete the temp file
                if (tempFbFile != null && tempFbFile.exists()) {
                    Log.d(TAG, "delete db temp file:" + tempFbFile.getName());
                    tempFbFile = null;
                }
                Log.i(TAG, "conflict retry into sync try in 100 ms");
                Thread.sleep(100);
            }

            // Save Time
            saveLastSyncTimestamp(currentTimestamp);

            Log.i(TAG, "sync completed");

            // ALL OK
            return new SyncResult(new SyncStatus(SyncStatus.OK), counter);
        } catch (SyncException e) {
            return new SyncResult(e.getStatus());
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            return new SyncResult(new SyncStatus(SyncStatus.ERROR, e.getMessage()));
        } finally {
            if (tempFbFile != null && tempFbFile.exists()) {
                Log.d(TAG, "delete db temp file:" + tempFbFile.getName());
            }
        }
    }


    public void dispose(){
        this.mCloudProvider.close();
    }

    /**
     * Write the database on json file
     * @return
     * @throws SyncException
     */
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
            mManager.writeDatabase(writer);

            // Close database
             writer.close();

            Log.i(TAG, "Created DB file with size: " + tempDbFile.length());
            return tempDbFile;
        } catch (Exception e) {
            throw new SyncException(SyncStatus.ERROR_WRITING_TMP_DB, e.getMessage());
        }
    }

    /**
     * Function to start sync of cloud database ad local
     * @param inputStream
     * @param lastSyncTimestamp
     */
    private void syncDatabase(final InputStream inputStream, DatabaseCounter counter, long lastSyncTimestamp, long currentTimestamp) {
        JSonDatabaseReader reader = null;

        try {
            // Create the new database reader
            reader = new JSonDatabaseReader(inputStream);

            // Start sync procedure with a the last timestamp
            mManager.syncDatabase(reader, counter, lastSyncTimestamp, currentTimestamp);
        } catch (IOException e) {
            // if the sync procedure generate an IOException i convert it
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void saveLastSyncTimestamp(long timestampToSave) {
        String sharedFileName;
        SharedPreferences prefs;

        sharedFileName = buildPreferenceFileName();
        prefs = mCtx.getSharedPreferences(sharedFileName, Context.MODE_PRIVATE);

        prefs.edit()
                .putLong("lastSyncTimeStamp", timestampToSave)
                .commit();

        Log.i(TAG, "Save the last sync timestamp " + timestampToSave);
    }

    public long getLastSyncTimestamp(){

        String sharedFileName;
        SharedPreferences prefs;
        long currentTimestamp;

        sharedFileName = buildPreferenceFileName();
        prefs = mCtx.getSharedPreferences(sharedFileName, Context.MODE_PRIVATE);

        currentTimestamp = prefs.getLong("lastSyncTimeStamp", 0);

        Log.i(TAG, "Read last sync timestamp " + currentTimestamp);

        return currentTimestamp;
    }

    public void resetLastSyncTimestamp(){
        saveLastSyncTimestamp(0);
    }

    private String buildPreferenceFileName(){
        return "com.claudiodegio.dbsync." + mDataBaseName + ".STORAGE";
    }
    public static class Builder {

        private CloudProvider mCloudProvider;
        private SQLiteDatabase mDB;
        private String mDataBaseName;
        private List<TableToSync> mTables = new ArrayList<>();
        private Context mCtx;
        @ConflictPolicy private int mConflictPolicy = CLIENT;
        private int mThresholdSeconds = 300;
        private int mSchemaVersion = 1;

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

        public Builder setConflictPolicy(@ConflictPolicy int conflictPolicy){
            this.mConflictPolicy = conflictPolicy;
            return this;
        }

        public Builder setThresholdSeconds(int thresholdSeconds) {
            this.mThresholdSeconds = thresholdSeconds;
            return this;
        }

        public Builder setSchemaVersion(int schemaVersion) {
            this.mSchemaVersion = schemaVersion;
            return this;
        }

        public DBSync build(){
            return new DBSync(mCtx, mCloudProvider, mDB, mDataBaseName, mTables, mConflictPolicy, mThresholdSeconds, mSchemaVersion);
        }
    }
}
