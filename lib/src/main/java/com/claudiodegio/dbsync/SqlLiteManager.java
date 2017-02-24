package com.claudiodegio.dbsync;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.claudiodegio.dbsync.SqlLiteUtility.SqlWithBinding;

/**
 * Classe di gestione delle operazioni su database
 */


public class SqlLiteManager {

    private final static String TAG = "SqlLiteManager";

    final private SQLiteDatabase mDb;
    @DBSync.ConflictPolicy final private int mConflictPolicy;
    final private int mThresholdSeconds;
    final private int mSchemaVersion;


    public SqlLiteManager(SQLiteDatabase db, int conflictPolicy, int thresholdSeconds, int schemaVersion) {
        this.mDb = db;
        this.mConflictPolicy = conflictPolicy;
        this.mThresholdSeconds = thresholdSeconds;
        this.mSchemaVersion = schemaVersion;
    }


    public void syncDatabase(final DatabaseReader reader, final List<TableToSync> tables, DatabaseCounter counter, long lastSyncTimestamp, long currentSyncTimestamp) throws IOException {
        Database dbCurrentDatabase;
        Table dbCurrentTable;
        Record dbCurrentRecord;
        TableToSync currentTableToSync = null;
        Map<String, ColumnMetadata> columns = null;
        int elementType;

        Log.i(TAG, "start syncDatabase");

        // TODO check database name
        try {
            // Open the TX
            mDb.beginTransaction();

            while ((elementType = reader.nextElement()) != JSonDatabaseReader.END) {
                switch (elementType) {
                    case JSonDatabaseReader.START_DB:
                        dbCurrentDatabase = reader.readDatabase();

                        // Check schema version
                        if (dbCurrentDatabase.getSchemaVersion() > mSchemaVersion) {
                            throw new SyncException(SyncStatus.ERROR_NEW_SCHEMA_VERSION, "Find new schema version, need to update (found:" + dbCurrentDatabase.getSchemaVersion() + ", expected:" + mSchemaVersion + ")");
                        }

                        break;
                    case JSonDatabaseReader.START_TABLE:
                        // Read the table and column metadata
                        dbCurrentTable = reader.readTable();
                        columns = SqlLiteUtility.readTableMetadataAsMap(mDb, dbCurrentTable.getName());

                        // Find the table to sync definition and rules
                        final String tableName = dbCurrentTable.getName();
                        currentTableToSync = IterableUtils.find(tables, new Predicate<TableToSync>() {
                            @Override
                            public boolean evaluate(TableToSync object) {
                                return object.getName().equals(tableName);
                            }
                        });

                        if (currentTableToSync == null) {
                            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find table " + tableName + " into table definition");
                        }

                        break;
                    case JSonDatabaseReader.RECORD:
                        dbCurrentRecord = reader.readRecord(columns);
                        // Found record sync single record
                        if (!dbCurrentRecord.isEmpty()) {
                            syncRecord(currentTableToSync, dbCurrentRecord, counter, lastSyncTimestamp, currentSyncTimestamp);
                        }
                        break;
                }
            }
            // Commit the TX
            mDb.setTransactionSuccessful();
        } catch (Exception e) {
            throw e;
        } finally {
            mDb.endTransaction();
        }

        Log.i(TAG, "end syncDatabase tables: " + counter.getTableSyncedCount() + " recUpdated:" + counter.getRecordUpdated() + " recInserted:" + counter.getRecordInserted());
    }

    private void syncRecord(final TableToSync tableToSync, final Record record, final DatabaseCounter counter, long lastSyncTimestamp, long currentSyncTimestamp){

        ColumnValue valueSendTime;
        ColumnValue valueCloudId;

        long sendTime;
        DBRecordMatch dbRecordMatch = null;
        RecordCounter tableCounter;
        int indexMatchRule;

        Log.v(TAG, "syncRecord called: record = " + record + ", tableName = [" + tableToSync.getName() + "], lastSyncTimestamp = [" + lastSyncTimestamp + "]");

        valueSendTime = record.findField(tableToSync.getSendTimeColumn());

        if (valueSendTime == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getSendTimeColumn() + " into cloud DB record");
        }

        valueCloudId = record.findField(tableToSync.getCloudIdColumn());

        if (valueCloudId == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getCloudIdColumn() + " into cloud DB record");
        }

        sendTime = valueSendTime.getValueLong();

        // Create table counter if non defined
        tableCounter = counter.findOrCreateTableCounter(tableToSync.getName());

        // Check if record it new or not
        if (sendTime > lastSyncTimestamp - mThresholdSeconds * 1000) {
            // New Record, find match rule to detect is to insert or update
            for (indexMatchRule = 0 ; indexMatchRule < tableToSync.getMatchRules().size(); ++indexMatchRule) {
                String rule = tableToSync.getMatchRules().get(indexMatchRule);

                dbRecordMatch = findDatabaseIdByMatchRule(rule, tableToSync, record);

                // If found a match i can break the loop
                if (dbRecordMatch != null) {
                    break;
                }
            }

            if (dbRecordMatch == null) {
                // Insert no more check no conflict
                Log.v(TAG, "syncRecord: insert new record with cloudId:" + valueCloudId.getValueString());

                insertRecordIntoDatabase(tableToSync, record);
                tableCounter.incrementRecordInserted();
                counter.incrementRecordInserted();
            } else {
                // Update

                // Check for possible conflict
                if ((dbRecordMatch.getSendTime() == null
                        || dbRecordMatch.getSendTime() == currentSyncTimestamp)
                        && sendTime > lastSyncTimestamp) {
                    // Conflict of data
                    if (mConflictPolicy == DBSync.SERVER) {
                        // perform update only if server version wins
                        Log.v(TAG, "syncRecord: update conflict record with cloudId:" + valueCloudId.getValueString() + " match with id:" + dbRecordMatch.getId() + " (match rule" + (indexMatchRule+1) + ")");

                        updateRecordIntoDatabase(dbRecordMatch, tableToSync, record);
                        tableCounter.incrementRecordUpdated();
                        counter.incrementRecordUpdated();
                    } else {
                        Log.v(TAG, "syncRecord: update ignored for conflict policy win server version");
                    }
                } else {
                    // No conflict perform update
                    Log.v(TAG, "syncRecord: update new record with cloudId:" + valueCloudId.getValueString() + " match with id:" + dbRecordMatch + " (match rule" + (indexMatchRule+1) + ")");

                    updateRecordIntoDatabase(dbRecordMatch, tableToSync, record);
                    tableCounter.incrementRecordUpdated();
                    counter.incrementRecordUpdated();
                }
            }
        } else {
            // Old Record ignore it
            Log.v(TAG, "syncRecord: ignored for timestamp too old");
        }
    }


    @Nullable
    private DBRecordMatch findDatabaseIdByMatchRule(final String rule, final TableToSync tableToSync, final Record record){
        String sql;
        String cloudId;
        Cursor cur = null;
        SqlWithBinding sqlWithBinding;
        String [] args;
        Long id, sendTime;

        // Build the sql
        sql = "SELECT " + tableToSync.getIdColumn()  + ","
                +  tableToSync.getSendTimeColumn() +
                " FROM " + tableToSync.getName() + " WHERE " + rule;

        // Extract the binding
        sqlWithBinding = SqlLiteUtility.sqlWithMapToSqlWithBinding(sql);

        args = new String[sqlWithBinding.getArgs().length];

        // Extract selection args for record
        for (int i = 0;  i < sqlWithBinding.getArgs().length; ++i) {
            String fieldToBind = sqlWithBinding.getArgs()[i];
            ColumnValue value = record.findField(fieldToBind);
            args[i] = value.toSelectionArg();
        }

        try {

            // Find the id
            cur = mDb.rawQuery(sqlWithBinding.getParsed(), args);

            if (cur.getCount() > 1) {
                cloudId = record.findField(tableToSync.getCloudIdColumn()).getValueString();
                throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Found more match with record with cloudId: "  + cloudId + " and match rule: " + rule);
            }

            if (cur.getCount() == 0) {
                return null;
            }

            cur.moveToNext();

            id = cur.getLong(0);

            sendTime = cur.getLong(1);

            return new DBRecordMatch(id, sendTime);
        } catch (Exception e) {
           throw e;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    private void updateRecordIntoDatabase(final DBRecordMatch dbRecordMatch, final TableToSync tableToSync, final Record record){
        ContentValues contentValues;
        String whereClause;

        contentValues = new ContentValues();

        for (ColumnValue value : record) {
            // Ignore id columns
            String fieldName = value.getMetadata().getName();

            if (!fieldName.equals(tableToSync.getIdColumn())) {
                SqlLiteUtility.columnValueToContentValues(value, contentValues);
            }
        }

        whereClause = tableToSync.getIdColumn() + " = ?";

        mDb.update(tableToSync.getName(), contentValues, whereClause, new String[]{ Long.toString(dbRecordMatch.getId()) });
    }


    private void insertRecordIntoDatabase(final TableToSync tableToSync, final Record record){
        ContentValues contentValues;

        contentValues = new ContentValues();

        for (ColumnValue value : record) {
            // Ignore id columns
            String fieldName = value.getMetadata().getName();

            if (!fieldName.equals(tableToSync.getIdColumn())) {
                SqlLiteUtility.columnValueToContentValues(value, contentValues);
            }
        }


        mDb.insert(tableToSync.getName(), null, contentValues);
    }

    public void populateUUID(final List<TableToSync> tables){
        for (TableToSync table : tables) {
            populateUUID(table);
        }
    }

    private void populateUUID(TableToSync table) {
        String selection;
        Cursor cur = null;
        String uuid;
        int id;
        int rowCount;
        ContentValues contentValuesUpdate;
        Log.i(TAG, "start populateUUID for table:" + table.getName() + " idColumn:" + table.getIdColumn() + " CloudIdColumn:" + table.getCloudIdColumn());

        selection = "( " + table.getCloudIdColumn() + " IS NULL OR " + table.getCloudIdColumn() + " = \"\")";

        if (!TextUtils.isEmpty(table.getFilter())) {
            selection += " AND " + table.getFilter();
        }

        try {
            cur = mDb.query(table.getName(), new String[]{table.getIdColumn()}, selection, null, null, null, null);

            rowCount = 0;
            contentValuesUpdate = new ContentValues();

            // TODO check uuid it'unique
            while (cur.moveToNext()) {
                id = cur.getInt(0);
                uuid = UUID.randomUUID().toString();

                Log.d(TAG, "assign uuid:" + uuid + " to id:" + id);
                // Update of cloud id
                contentValuesUpdate.put(table.getCloudIdColumn(), uuid);

                mDb.update(table.getName(), contentValuesUpdate, table.getIdColumn() + " = ?", new String[]{Integer.toString(id)});
                rowCount++;
            }

            Log.i(TAG, "end populateUUID rows updated:" + rowCount);
        } catch (Exception e) {
            throw e;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public void populateSendTime(long sendTimestamp, final List<TableToSync> tables) {
        for (TableToSync table : tables) {
            populateSendTime(sendTimestamp, table);
        }
    }

    private void populateSendTime(long sendTimestamp, final TableToSync table) {
        int rowUpdated;
        String where;
        ContentValues contentValues;

        Log.i(TAG, "start populateSendTime for table:" + table.getName() + " sendTable:" + table.getSendTimeColumn());

        where = table.getSendTimeColumn() + " IS NULL";

        if (!TextUtils.isEmpty(table.getFilter())) {
            where += " AND "+ table.getFilter();
        }

        contentValues = new ContentValues();
        contentValues.put(table.getSendTimeColumn(), sendTimestamp);

        rowUpdated = mDb.update(table.getName(), contentValues,  where, null);

        Log.i(TAG, "end populateUUID  updated record:" + rowUpdated);
    }

    class DBRecordMatch {
        private Long mId;
        private Long mSendTime;

        public DBRecordMatch(Long id,  Long sendTime) {
            this.mId = id;
            this.mSendTime = sendTime;
        }

        public Long getSendTime() {
            return mSendTime;
        }

        public Long getId() {
            return mId;
        }
    }
}
