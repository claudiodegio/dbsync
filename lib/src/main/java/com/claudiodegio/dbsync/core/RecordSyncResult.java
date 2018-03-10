package com.claudiodegio.dbsync.core;


import org.apache.commons.collections4.IterableUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class RecordSyncResult {

    // Count of records Updated/Inserted and relative id
    private Map<String, RecordChanged> mRecordChanged;

    public RecordSyncResult() {
        this.mRecordChanged = new HashMap<>();
    }

    public int getTableSyncedCount(){
        return mRecordChanged.size();
    }

    public Map<String, RecordChanged> getTableSynced(){
        return mRecordChanged;
    }

    RecordChanged findOrCreateTableCounter(final String tableName) {

        if (!mRecordChanged.containsKey(tableName)) {
            mRecordChanged.put(tableName, new RecordChanged());
        }

        return mRecordChanged.get(tableName);
    }

    public int getRecordInserted() {
        int total = 0;

        for (RecordChanged recordChanged : mRecordChanged.values()) {
            total += recordChanged.getRecordInserted();
        }

        return total;
    }

    public int getRecordUpdated() {
        int total = 0;

        for (RecordChanged recordChanged : mRecordChanged.values()) {
            total += recordChanged.getRecordUpdated();
        }

        return total;
    }

}
