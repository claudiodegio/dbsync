package com.claudiodegio.dbsync.core;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.collections4.IterableUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.SyncStatus;
import com.claudiodegio.dbsync.TableToSync;
import com.claudiodegio.dbsync.exception.SyncException;
import com.claudiodegio.dbsync.json.JSonDatabaseReader;

/**
 * Classe di gestione delle operazioni su database
 */


public class SqlLiteManager {

    private final static String TAG = "SqlLiteManager";

    private final static String JOIN_COLUMN_PREFIX = "FK_CLOUD_";

    final private SQLiteDatabase mDB;
    final private String mDataBaseName;
    @DBSync.ConflictPolicy
    final private int mConflictPolicy;
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


    public void syncDatabase(final DatabaseReader reader, RecordSyncResult result, long lastSyncTimestamp, long currentSyncTimestamp) throws IOException {
        Database dbCurrentDatabase;
        Table dbCurrentTable;
        Record dbCurrentRecord;
        TableToSync currentTableToSync = null;
        Map<String, ValueMetadata> columns = null;
        String joinColumnName;
        ValueMetadata joinColumnMetadata;
        int elementType;

        Log.i(TAG, "start syncDatabase");

        // TODO check database name
        try {
            // Open the TX
            mDB.beginTransaction();

            while ((elementType = reader.nextElement()) != JSonDatabaseReader.END)
                switch (elementType) {
                    case JSonDatabaseReader.START_DB:
                        dbCurrentDatabase = reader.readDatabase();

                        // Check schema version
                        if (dbCurrentDatabase.getSchemaVersion() > mSchemaVersion) {
                            throw new SyncException(SyncStatus.Code.ERROR_NEW_SCHEMA_VERSION, "Find new schema version, need to update (found:" + dbCurrentDatabase.getSchemaVersion() + ", expected:" + mSchemaVersion + ")");
                        }

                        break;
                    case JSonDatabaseReader.START_TABLE:
                        // Read the table and column metadata
                        dbCurrentTable = reader.readTable();

                        // Find the table to sync definition and rules
                        final String tableName = dbCurrentTable.getName();
                        currentTableToSync = IterableUtils.find(mTableToSync, object -> object.getName().equals(tableName));

                        if (currentTableToSync == null) {
                            throw new SyncException(SyncStatus.Code.ERROR_SYNC_COULD_DB, "Unable to find table " + tableName + " into table definition");
                        }

                        columns = SqlLiteUtility.readTableMetadataAsMap(mDB, dbCurrentTable.getName());

                        // Add the records columns for join tables
                        for (JoinTable joinTable : currentTableToSync.getJoinTable()) {
                            joinColumnName = JOIN_COLUMN_PREFIX + joinTable.getJoinColumn();
                            joinColumnMetadata = new ValueMetadata(joinColumnName.toUpperCase(), ValueMetadata.TYPE_STRING);
                            columns.put(joinColumnName, joinColumnMetadata);
                        }

                        break;
                    case JSonDatabaseReader.RECORD:
                        dbCurrentRecord = reader.readRecord(columns);
                        // Found record sync single record
                        if (!dbCurrentRecord.isEmpty()) {
                            syncRecord(currentTableToSync, dbCurrentRecord, result, lastSyncTimestamp, currentSyncTimestamp);
                        }
                        break;
                }
            // Commit the TX
            mDB.setTransactionSuccessful();
        } catch (Exception e) {
            throw e;
        } finally {
            mDB.endTransaction();
        }

        Log.i(TAG, "end syncDatabase tables: " + result.getTableSyncedCount() + " recUpdated:" + result.getRecordUpdated() + " recInserted:" + result.getRecordInserted());
    }

    private void syncRecord(final TableToSync tableToSync, final Record record, final RecordSyncResult result, long lastSyncTimestamp, long currentSyncTimestamp){

        Value valueSendTime;
        Value valueCloudId;

        long sendTime;
        DBRecordMatch dbRecordMatch = null;
        RecordChanged tableRecordChanged;
        int indexMatchRule;

        Log.v(TAG, "syncRecord called: record = " + record + ", tableName = [" + tableToSync.getName() + "], lastSyncTimestamp = [" + lastSyncTimestamp + "]");

        valueSendTime = record.findField(tableToSync.getSendTimeColumn());

        if (valueSendTime == null) {
            throw new SyncException(SyncStatus.Code.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getSendTimeColumn() + " into cloud DB record");
        }

        valueCloudId = record.findField(tableToSync.getCloudIdColumn());

        if (valueCloudId == null) {
            throw new SyncException(SyncStatus.Code.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getCloudIdColumn() + " into cloud DB record");
        }

        sendTime = valueSendTime.getValueLong();

        // Create table counter if non defined
        tableRecordChanged = result.findOrCreateTableCounter(tableToSync.getName());

        // Check if record is new or not
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

                long id = insertRecordIntoDatabase(tableToSync, record);
                tableRecordChanged.addInseredId(id);
            } else {
                // Update
                if (dbRecordMatch.getSendTime() == sendTime) {
                    Log.v(TAG, "syncRecord: ignored for send time same of current record");
                    return;
                }

                // Check for possible conflict
                if ((dbRecordMatch.getSendTime() == null
                        || dbRecordMatch.getSendTime() == currentSyncTimestamp)
                        && sendTime > lastSyncTimestamp) {
                    // Conflict of data
                    if (mConflictPolicy == DBSync.ConflictPolicy.SERVER) {
                        // perform update only if server version wins
                        Log.v(TAG, "syncRecord: update conflict record with cloudId:" + valueCloudId.getValueString() + " match with id:" + dbRecordMatch.getId() + " (match rule" + (indexMatchRule+1) + ")");

                        updateRecordIntoDatabase(dbRecordMatch, tableToSync, record);
                        tableRecordChanged.addUpdatedId(dbRecordMatch.getId());
                    } else {
                        Log.v(TAG, "syncRecord: update ignored for conflict policy win client version");
                    }
                } else {
                    // No conflict perform update
                    Log.v(TAG, "syncRecord: update new record with cloudId:" + valueCloudId.getValueString() + " match with id:" + dbRecordMatch + " (match rule" + (indexMatchRule+1) + ")");

                    updateRecordIntoDatabase(dbRecordMatch, tableToSync, record);
                    tableRecordChanged.addUpdatedId(dbRecordMatch.getId());
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
        SqlLiteUtility.SqlWithBinding sqlWithBinding;
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
            Value value = record.findField(fieldToBind);
            args[i] = value.toSelectionArg();
        }

        try {

            // Find the id
            cur = mDB.rawQuery(sqlWithBinding.getParsed(), args);

            if (cur.getCount() > 1) {
                cloudId = record.findField(tableToSync.getCloudIdColumn()).getValueString();
                throw new SyncException(SyncStatus.Code.ERROR_SYNC_COULD_DB, "Found more match with record with cloudId: "  + cloudId + " and match rule: " + rule);
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

    @Nullable
    private Long findDatabaseIdByCloudId(final String cloudId, final TableToSync table) {

        String whereCause;
        Cursor cur = null;

        try {

            whereCause = table.getCloudIdColumn() + " = ?";

            cur = mDB.query(table.getName(), new String[] { table.getIdColumn()}, whereCause, new String[] { cloudId }, null, null, null, null);

            if (cur.getCount() == 0) {
                return null;
            }

            cur.moveToFirst();

            return cur.getLong(0);
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

        contentValues = buildContentValues(tableToSync, record);

        whereClause = tableToSync.getIdColumn() + " = ?";

        mDB.update(tableToSync.getName(), contentValues, whereClause, new String[]{ Long.toString(dbRecordMatch.getId()) });
    }

    private long insertRecordIntoDatabase(final TableToSync tableToSync, final Record record){
        ContentValues contentValues;

        contentValues = buildContentValues(tableToSync, record);

       return mDB.insertOrThrow(tableToSync.getName(), null, contentValues);
    }

    private ContentValues buildContentValues(final TableToSync tableToSync, final Record record){
        ContentValues contentValues;
        JoinTable joinTable;
        Long joinId;

        contentValues = new ContentValues();

        // Work on join columns
        for (Value value : record) {
            final String fieldName = value.getMetadata().getName();

            // Ignore id columns, and join columns
            if (!fieldName.equals(tableToSync.getIdColumn())
                    && ! fieldName.startsWith(JOIN_COLUMN_PREFIX)) {
                SqlLiteUtility.columnValueToContentValues(value, contentValues);
            }

            // Found join columns
            if (fieldName.startsWith(JOIN_COLUMN_PREFIX)) {

                joinTable = IterableUtils.find(tableToSync.getJoinTable(), object -> fieldName.toUpperCase().endsWith(object.getJoinColumn()));

                if (joinTable == null) {
                    throw new SyncException(SyncStatus.Code.ERROR_SYNC_COULD_DB, "Unable to find join table for field " + fieldName + " into definition");
                }


                if (!value.isNull()) {
                    // The value is non null try to find the local id
                    joinId = findDatabaseIdByCloudId(value.getValueString(), joinTable.getReferenceTable());

                    if (joinId != null) {
                        Log.d(TAG, "Map join column: " + joinTable.getJoinColumn() + " (table: "+ joinTable.getReferenceTable().getName() + ") with id:" + joinId);
                        contentValues.put(joinTable.getJoinColumn(), joinId);
                    } else {
                        Log.d(TAG, "Map join column: " + joinTable.getJoinColumn() + " (table: "+ joinTable.getReferenceTable().getName() + ") with null");
                        contentValues.putNull(joinTable.getJoinColumn());
                    }
                } else {
                    // The value is null
                    Log.d(TAG, "Map join column: " + joinTable.getJoinColumn() + " (table: "+ joinTable.getReferenceTable().getName() + ") with null");
                    contentValues.putNull(joinTable.getJoinColumn());
                }
            }
        }

        return contentValues;
    }
    public void populateUUID(){
        for (TableToSync table : mTableToSync) {
            populateUUID(table);
        }
    }

    private void populateUUID(TableToSync table) {
        String sql;
        Cursor cur = null;
        String uuid;
        int id;
        int rowCount;
        ContentValues contentValuesUpdate;
        Log.i(TAG, "start populateUUID for table:" + table.getName() + " idColumn:" + table.getIdColumn() + " CloudIdColumn:" + table.getCloudIdColumn());

        sql = "SELECT " + table.getIdColumn() + " FROM " + table.getName() + " a WHERE ( " + table.getCloudIdColumn() + " IS NULL OR " + table.getCloudIdColumn() + " = \"\")";

        if (!TextUtils.isEmpty(table.getFilter())) {
            sql += " AND " + table.getFilter();
        }

        try {
            cur = mDB.rawQuery(sql, null);

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

        where = table.getIdColumn() + " IN (SELECT a." + table.getIdColumn() + " FROM " +table.getName() +
                " a WHERE " + table.getSendTimeColumn() + " IS NULL";
        if (!TextUtils.isEmpty(table.getFilter())) {
            where += " AND "+ table.getFilter();
        }

        where += ")";

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
        List<ValueMetadata> columnsMetadata;
        Cursor cur = null;
        Value value = null;
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
            sqlJoin += " LEFT JOIN " + refTable.getName()+ " " + alias;
            sqlJoin += " ON a." + joinTable.getJoinColumn() + " = " + alias + "." + refTable.getIdColumn();

            // Selection
            sqlSelection += ", " + alias + "." + refTable.getCloudIdColumn() + " as " + JOIN_COLUMN_PREFIX + joinTable.getJoinColumn();

            listJoinColumn.add(joinTable.getJoinColumn());

            aliasCount++;
        }

        try {
            Log.i(TAG, "Write table:" + table.getName() + " with " + table.getJoinTable().size() + " tables");

            sql = "SELECT " + sqlSelection + " FROM " + table.getName() + " a " + sqlJoin;

            if (!TextUtils.isEmpty(table.getFilter())) {
                sql += " WHERE " + table.getFilter();
            }

            Log.d(TAG, "Write sql:" + sql);

            cur = mDB.rawQuery(sql, null);

            writer.writeTable(table.getName(), cur.getCount());

            while (cur.moveToNext()) {
                record = new Record();

                for (ValueMetadata colMeta : columnsMetadata) {
                    if (!table.isColumnToIgnore(colMeta.getName())
                            && !listJoinColumn.contains(colMeta.getName())) {
                        value = SqlLiteUtility.getCursorColumnValue(cur, colMeta);
                        record.add(value);
                    }
                }

                for (String joinColumn : listJoinColumn) {
                    joinColumn = JOIN_COLUMN_PREFIX + joinColumn;
                    value = SqlLiteUtility.getCursorColumnValue(cur,  joinColumn.toUpperCase(), ValueMetadata.TYPE_STRING);
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
    }


    private class DBRecordMatch {
        private Long mId;
        private Long mSendTime;

        DBRecordMatch(Long id,  Long sendTime) {
            this.mId = id;
            this.mSendTime = sendTime;
        }

        Long getSendTime() {
            return mSendTime;
        }

        public Long getId() {
            return mId;
        }
    }
}
