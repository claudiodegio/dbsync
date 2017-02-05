package com.claudiodegio.dbsync;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

        Log.i(TAG, "end syncDatabase tables: " + counter.getTableSyncedCount() + " recUpdated:" + counter.getRecordUpdated() + " recInserted:" + counter.getRecordInserted());
        return counter;
    }


    private void syncRecord(final TableToSync tableDefinition, final Record record, final DatabaseCounter counter, long lastSyncTimestamp){
        Log.v(TAG, "syncRecord called: record = " + record + ", tableName = [" + tableDefinition.getName() + "], lastSyncTimestamp = [" + lastSyncTimestamp + "]");

        ColumnValue valueDateCreated;
        ColumnValue valueLastUpdated;
        long dateCreated;
        long lastUpdated;
        Long dbId;

        valueDateCreated = record.findField(tableDefinition.getDateCreatedColumn());

        if (valueDateCreated == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableDefinition.getDateCreatedColumn() + " into cloud DB record");
        }
        dateCreated = valueDateCreated.getValueLong();

        valueLastUpdated = record.findField(tableDefinition.getLastUpdatedColumn());

        if (valueLastUpdated == null) {
            throw new SyncException(SyncStatus.ERROR_SYNC_COULD_DB, "Unable to find column " + tableDefinition.getLastUpdatedColumn() + " into cloud DB record");
        }
        lastUpdated = valueLastUpdated.getValueLong();


        // Check if record it to update
        if (dateCreated > lastSyncTimestamp || lastUpdated > lastSyncTimestamp) {
            // The record it's to update, find the match rules

            for (String rule : tableDefinition.getMatchRules()) {
                dbId = findDatabaseIdByMatchRule(rule, tableDefinition.getIdColumn(), record);

                // If found a match i can break the loop
                if (dbId != null) {
                    break;
                }
            }



        } else {
            Log.v(TAG, "syncRecord: ignored");
        }
    }


    private Long findDatabaseIdByMatchRule(final String rule, final String columnId, final Record record){
        String sql;
        Cursor cur;
        SqlWithBinding sqlWithBinding;

        // Build the sql

        sql = "SELECT " + columnId + " FROM WHERE " + rule;

        // System.out.println(sql);

        // mDb.rawQuery()

        sqlWithBinding = SqlLiteUtility.sqlWithMapToSqlWithBinding(sql);


        System.out.println(sqlWithBinding);


        return null;
    }
}
