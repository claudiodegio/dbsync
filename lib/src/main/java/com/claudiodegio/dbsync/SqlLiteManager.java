package com.claudiodegio.dbsync;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.functors.ForClosure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.claudiodegio.dbsync.SqlLiteUtility.SqlWithBinding;

/**
 * Classe di gestione delle operazioni su database
 */

// TODO mettere populate UUID
// TODO mettere logica generazione record

public class SqlLiteManager {

    private final static String TAG = "SqlLiteManager";

    final private SQLiteDatabase mDb;

    public SqlLiteManager(SQLiteDatabase db) {
        this.mDb = db;
    }


    public DatabaseCounter syncDatabase(final DatabaseReader reader, final List<TableToSync> tables, long lastSyncTimestamp) throws IOException {
        DatabaseCounter counter;
        Database dbCurrentDatabase;
        Table dbCurrentTable;
        Record dbCurrentRecord;
        TableToSync currentTableToSync = null;
        Map<String, ColumnMetadata> columns = null;
        int elementType;

        counter = new DatabaseCounter();

        Log.i(TAG, "start syncDatabase");

        // TODO check database name
        // TODO open TX
        try {
            // Open the TX
            mDb.beginTransaction();

            while ((elementType = reader.nextElement()) != JSonDatabaseReader.END) {
                switch (elementType) {
                    case JSonDatabaseReader.START_DB:
                        dbCurrentDatabase = reader.readDatabase();
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
                        syncRecord(currentTableToSync, dbCurrentRecord, counter, lastSyncTimestamp);
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
        return counter;
    }

    private void syncRecord(final TableToSync tableToSync, final Record record, final DatabaseCounter counter, long lastSyncTimestamp){

        ColumnValue valueDateCreated;
        ColumnValue valueLastUpdated;
        ColumnValue valueCloudId;

        long dateCreated;
        long lastUpdated;
        Long dbId = null;
        RecordCounter tableCounter;
        int indexMatchRule;

        Log.v(TAG, "syncRecord called: record = " + record + ", tableName = [" + tableToSync.getName() + "], lastSyncTimestamp = [" + lastSyncTimestamp + "]");

        valueDateCreated = record.findField(tableToSync.getDateCreatedColumn());

        if (valueDateCreated == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getDateCreatedColumn() + " into cloud DB record");
        }
        dateCreated = valueDateCreated.getValueLong();

        valueLastUpdated = record.findField(tableToSync.getLastUpdatedColumn());

        if (valueLastUpdated == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getLastUpdatedColumn() + " into cloud DB record");
        }

        valueCloudId = record.findField(tableToSync.getCloudIdColumn());

        if (valueCloudId == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableToSync.getCloudIdColumn() + " into cloud DB record");
        }

        lastUpdated = valueLastUpdated.getValueLong();

        // Create table counter if non defined
        tableCounter = counter.findOrCreateTableCounter(tableToSync.getName());

        // Check if record it to update
        if (dateCreated > lastSyncTimestamp || lastUpdated > lastSyncTimestamp) {

            for (indexMatchRule = 0 ; indexMatchRule < tableToSync.getMatchRules().size(); ++indexMatchRule) {
                String rule = tableToSync.getMatchRules().get(indexMatchRule);

                dbId = findDatabaseIdByMatchRule(rule, tableToSync, record);

                // If found a match i can break the loop
                if (dbId != null) {
                    break;
                }
            }

            if (dbId == null) {
                // Insert
                Log.v(TAG, "syncRecord: insert new record with cloudId:" + valueCloudId.getValueString());

                insertRecordIntoDatabase(tableToSync, record);
                tableCounter.incrementRecordInserted();
                counter.incrementRecordInserted();
            } else {
                // Update
                Log.v(TAG, "syncRecord: update new record with cloudId:" + valueCloudId.getValueString() + " match with id:" + dbId + " (match rule" + (indexMatchRule+1) + ")");

                updateRecordIntoDatabase(dbId, tableToSync, record);
                tableCounter.incrementRecordUpdated();
                counter.incrementRecordUpdated();
            }


        } else {
            Log.v(TAG, "syncRecord: ignored for timestamp too old");
        }
    }


    @Nullable
    private Long findDatabaseIdByMatchRule(final String rule, final TableToSync tableToSync, final Record record){
        String sql;
        String cloudId;
        Cursor cur = null;
        SqlWithBinding sqlWithBinding;
        String [] args;

        // Build the sql
        sql = "SELECT " + tableToSync.getIdColumn() + " FROM " + tableToSync.getName() + " WHERE " + rule;

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
            cloudId = record.findField(tableToSync.getCloudIdColumn()).getValueString();

            // Find the id
            cur = mDb.rawQuery(sqlWithBinding.getParsed(), args);

            if (cur.getCount() > 1) {
                throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Found more match with record with cloudId: "  + cloudId + " and match rule: " + rule);
            }

            if (cur.getCount() == 0) {
                return null;
            }

            cur.moveToNext();
            return cur.getLong(0);
        } catch (Exception e) {
           throw e;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    private void updateRecordIntoDatabase(final Long id, final TableToSync tableToSync, final Record record){
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

        mDb.update(tableToSync.getName(), contentValues, whereClause, new String[]{ Long.toString(id) });
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
}
