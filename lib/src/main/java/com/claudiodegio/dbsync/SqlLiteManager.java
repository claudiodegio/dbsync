package com.claudiodegio.dbsync;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.claudiodegio.dbsync.SqlLiteUtility.SqlWithBinding;

/**
 * Classe di gestione delle operazioni su database
 */


public class SqlLiteManager {

    private final static String TAG = "SqlLiteManager";

    final private SQLiteDatabase mDB;
    final private String mDataBaseName;
    @DBSync.ConflictPolicy final private int mConflictPolicy;
    final private int mThresholdSeconds;
    final private int mSchemaVersion;
    final private List<TableToSync> mTableToSync;


    public SqlLiteManager(SQLiteDatabase db,  String dataBaseName, List<TableToSync> tableToSync, int conflictPolicy, int thresholdSeconds, int schemaVersion) {
        this.mDB = db;
        this.mDataBaseName = dataBaseName;
        this.mConflictPolicy = conflictPolicy;
        this.mThresholdSeconds = thresholdSeconds;
        this.mSchemaVersion = schemaVersion;
        this.mTableToSync = tableToSync;
    }


    public void syncDatabase(final DatabaseReader reader, DatabaseCounter counter, long lastSyncTimestamp, long currentSyncTimestamp) throws IOException {
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
            mDB.beginTransaction();

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
                        columns = SqlLiteUtility.readTableMetadataAsMap(mDB, dbCurrentTable.getName());

                        // Find the table to sync definition and rules
                        final String tableName = dbCurrentTable.getName();
                        currentTableToSync = IterableUtils.find(mTableToSync, new Predicate<TableToSync>() {
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
            mDB.setTransactionSuccessful();
        } catch (Exception e) {
            throw e;
        } finally {
            mDB.endTransaction();
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
            cur = mDB.rawQuery(sqlWithBinding.getParsed(), args);

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

        mDB.update(tableToSync.getName(), contentValues, whereClause, new String[]{ Long.toString(dbRecordMatch.getId()) });
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


        mDB.insert(tableToSync.getName(), null, contentValues);
    }

    public void populateUUID(){
        for (TableToSync table : mTableToSync) {
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
            cur = mDB.query(table.getName(), new String[]{table.getIdColumn()}, selection, null, null, null, null);

            rowCount = 0;
            contentValuesUpdate = new ContentValues();

            // TODO check uuid it'unique
            while (cur.moveToNext()) {
                id = cur.getInt(0);
                uuid = UUID.randomUUID().toString();

                Log.d(TAG, "assign uuid:" + uuid + " to id:" + id);
                // Update of cloud id
                contentValuesUpdate.put(table.getCloudIdColumn(), uuid);

                mDB.update(table.getName(), contentValuesUpdate, table.getIdColumn() + " = ?", new String[]{Integer.toString(id)});
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

    public void populateSendTime(long sendTimestamp) {
        for (TableToSync table : mTableToSync) {
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

        rowUpdated = mDB.update(table.getName(), contentValues,  where, null);

        Log.i(TAG, "end populateUUID  updated record:" + rowUpdated);
    }

    public void writeDatabase(final DatabaseWriter writer) throws IOException {
        // Write database start
        writer.writeDatabase(mDataBaseName, mTableToSync.size(), mSchemaVersion);

        // Write tables
        for (TableToSync table : mTableToSync) {
            serializeTable(table, writer);
        }
    }

    private void serializeTable(final TableToSync table, final DatabaseWriter writer) throws IOException{
        List<ColumnMetadata> columnsMetadata;
        Cursor cur = null;
        ColumnValue value = null;
        Record record;
        String sql;
        String sqlJoin;
        String sqlSelection;
        String alias;
        TableToSync refTable;
        List<String> listJoinColumn;

        int aliasCount = 1;

        columnsMetadata = SqlLiteUtility.readTableMetadata(mDB, table.getName());

        sqlSelection = "a.*";
        sqlJoin = "";
        listJoinColumn = new ArrayList<>(table.getJoinTable().size());

        for (JoinTable joinTable : table.getJoinTable()) {
            refTable = joinTable.getReferenceTable();

            alias = "a" + aliasCount;

            // JOIN
            sqlJoin += "LEFT JOIN " + refTable.getName()+ " " + alias;
            sqlJoin += " ON a." + joinTable.getJoinColumn() + " = " + alias + "." + refTable.getIdColumn();

            // Selection
            sqlSelection += ", " + alias + "." + refTable.getCloudIdColumn() + " as CLOUD_" + joinTable.getJoinColumn();

            listJoinColumn.add(joinTable.getJoinColumn());

            aliasCount++;
        }

        try {
            Log.i(TAG, "Write table:" + table.getName() + " with " + table.getJoinTable().size() + " tables");

            sql = "SELECT " + sqlSelection + " FROM " + table.getName() + " a " + sqlJoin;

            if (!TextUtils.isEmpty(table.getFilter())) {
                sql += "WHERE " + table.getFilter();
            }

            Log.d(TAG, "Write sql:" + sql);

            cur = mDB.rawQuery(sql, null);

            writer.writeTable(table.getName(), cur.getCount());

            while (cur.moveToNext()) {
                record = new Record();

                for (ColumnMetadata colMeta : columnsMetadata) {
                    if (!table.isColumnToIgnore(colMeta.getName())
                            && !listJoinColumn.contains(colMeta.getName())) {
                        value = SqlLiteUtility.getCursorColumnValue(cur, colMeta);
                        record.add(value);
                    }
                }

                for (String joinColumn : listJoinColumn) {
                    joinColumn = "CLOUD_" + joinColumn.toUpperCase();
                    value = SqlLiteUtility.getCursorColumnValue(cur,  joinColumn, ColumnMetadata.TYPE_STRING);
                    record.add(value);
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


        /*try {
            cur = mDB.query(table.getName(), null, table.getFilter(), null, null, null, null);

            Log.i(TAG, "Write simple table:" + table.getName() + " records:" + cur.getCount());

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
        }*/
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
