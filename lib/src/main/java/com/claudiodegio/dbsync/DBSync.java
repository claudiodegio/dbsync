package com.claudiodegio.dbsync;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;

import com.claudiodegio.dbsync.core.JoinTable;
import com.claudiodegio.dbsync.core.Table;
import com.claudiodegio.dbsync.core.Utility;
import com.claudiodegio.dbsync.exception.SyncBuildException;
import com.claudiodegio.dbsync.provider.CloudProvider;
import com.claudiodegio.dbsync.core.DatabaseCounter;
import com.claudiodegio.dbsync.core.DatabaseWriter;
import com.claudiodegio.dbsync.exception.SyncException;
import com.claudiodegio.dbsync.json.JSonDatabaseReader;
import com.claudiodegio.dbsync.json.JSonDatabaseWriter;
import com.claudiodegio.dbsync.core.SqlLiteManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for sync datase process
 */
public class DBSync {

    final static private String TAG = "DBSync";

    final private CloudProvider mCloudProvider;
    final private SQLiteDatabase mDB;
    final private SqlLiteManager mManager;
    final private List<TableToSync> mTables;
    final private Context mCtx;
    final private String mDataBaseName;
    final private String mLocalPrefName;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ConflictPolicy.SERVER, ConflictPolicy.CLIENT})
    public @interface ConflictPolicy {
        int SERVER = 1;
        int CLIENT = 2;
    }

    private DBSync(final Context ctx, final CloudProvider cloudProvider, final SQLiteDatabase db, final String dataBaseName, final List<TableToSync> tables, final String localPrefName, @ConflictPolicy int conflictPolicy, int thresholdSeconds, int schemaVersion){
        this.mCtx = ctx;
        this.mCloudProvider = cloudProvider;
        this.mDB = db;
        this.mTables = tables;
        this.mDataBaseName = dataBaseName;
        this.mManager = new SqlLiteManager(mDB, dataBaseName, tables, conflictPolicy, thresholdSeconds, schemaVersion);
        this.mLocalPrefName = localPrefName;
    }

    /**
     * Start the sync process
     * @return result of sync
     */
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
                if (!Utility.isConnectionUp(mCtx)) {
                    throw new SyncException(SyncStatus.Code.ERROR_NO_CONNECTION, "Error no connection");
                }
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

                if (!Utility.isConnectionUp(mCtx)) {
                    throw new SyncException(SyncStatus.Code.ERROR_NO_CONNECTION, "Error no connection");
                }

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
                break;
            }

            // Save Time
            saveLastSyncTimestamp(currentTimestamp);

            Log.i(TAG, "sync completed");

            // ALL OK
            return new SyncResult(new SyncStatus(SyncStatus.Code.OK), counter);
        } catch (SyncException e) {
            return new SyncResult(e.getStatus());
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            return new SyncResult(new SyncStatus(SyncStatus.Code.ERROR, e.getMessage()));
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
        File tempDbFile = null;
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
            FileUtils.deleteQuietly(tempDbFile);
            throw new SyncException(SyncStatus.Code.ERROR_WRITING_TMP_DB, e.getMessage());
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
            throw new SyncException(SyncStatus.Code.ERROR_SYNC_COULD_DB, e);
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
        return "com.claudiodegio.dbsync." + mLocalPrefName + ".STORAGE";
    }

    /**
     * Builder class sync object
     */
    public static class Builder {

        private CloudProvider mCloudProvider;
        private SQLiteDatabase mDB;
        private String mDataBaseName;
        private String mLocalPrefName;
        private List<TableToSync> mTables = new ArrayList<>();
        private Context mCtx;
        @ConflictPolicy private int mConflictPolicy = ConflictPolicy.CLIENT;
        private int mToleranceSeconds = 60;
        private int mSchemaVersion = 1;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        /**
         * Set the cloud provider
         * @param cloudProvider the cloud provider
         */
        public Builder setCloudProvider(final CloudProvider cloudProvider) {
            mCloudProvider = cloudProvider;
            return this;
        }

        /**
         * Set the connection to sqlite database
         * @param db the db
         */
        public Builder setSQLiteDatabase(final SQLiteDatabase db) {
            this.mDB = db;
            return this;
        }

        /**
         * Add a table to sync
         * @param table the table to sync
         */
        public Builder addTable(final TableToSync table) {
            this.mTables.add(table);
            return this;
        }

        /**
         * Set the database name
         * @param dataBaseName the db name
         */
        public Builder setDataBaseName(String dataBaseName) {
            this.mDataBaseName = dataBaseName;
            return this;
        }


        /**
         * Set the local preference file name used to save last sync time a runtime informazione
         * used locally if non defined use the database name
         * @param localPrefName the db name
         */
        public Builder setLocalPrefName(String localPrefName) {
            this.mLocalPrefName = localPrefName;
            return this;
        }


        /**
         * Set the conflict policy
         * @param conflictPolicy the conflict policy to set 
         */
        public Builder setConflictPolicy(@ConflictPolicy int conflictPolicy){
            this.mConflictPolicy = conflictPolicy;
            return this;
        }

        /**
         * Set tolerance of send time
         * @param toleranceSeconds the tolerance in seconds
         */
        public Builder setToleranceSeconds(int toleranceSeconds) {
            this.mToleranceSeconds = toleranceSeconds;
            return this;
        }

        /**
         * Set the database schema version
         * @param schemaVersion the schema version
ì         */
        public Builder setSchemaVersion(int schemaVersion) {
            this.mSchemaVersion = schemaVersion;
            return this;
        }

        /**
         * Build a new syn object
         */
        public DBSync build(){
            List<String> listTable;
            if (mCloudProvider == null) {
                throw new SyncBuildException("Missing the CloudProvider");
            }

            if (mDB == null) {
                throw new SyncBuildException("Missing the DB");
            }

            if (TextUtils.isEmpty(mDataBaseName)) {
                throw new SyncBuildException("Missing the database name");
            }

            if (mTables.isEmpty()) {
                throw new SyncBuildException("No table to sync");
            }

           listTable = new ArrayList<>(mTables.size());
            for (TableToSync tableToSync : mTables) {
                listTable.add(tableToSync.getName());
                for (JoinTable joinTable : tableToSync.getJoinTable()) {
                    if (!listTable.contains(joinTable.getReferenceTable().getName())) {
                        throw new SyncBuildException("Wrong table order, the table " + joinTable.getReferenceTable().getName() + " must be declared before " + tableToSync.getName());
                    }
                }
            }

            if (TextUtils.isEmpty(mLocalPrefName)) {
                mLocalPrefName = mDataBaseName;
            }

            return new DBSync(mCtx, mCloudProvider, mDB, mDataBaseName, mTables, mLocalPrefName, mConflictPolicy, mToleranceSeconds, mSchemaVersion);
        }
    }
}
