package com.claudiodegio.dbsync.core;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RecordSyncResult {

    // Count of records Updated/Inserted and relative id
    private Map<String, RecordChanged> mRecordChanged;

    public RecordSyncResult() {
        this.mRecordChanged = new HashMap<>();
    }

    public int getTableSyncedCount(){
        return mRecordChanged.size();
    }

    public Set<String> getTableSynced(){
        return mRecordChanged.keySet();
    }

    RecordChanged findOrCreateTableCounter(final String tableName) {

        if (!mRecordChanged.containsKey(tableName)) {
            mRecordChanged.put(tableName, new RecordChanged());
        }

        return mRecordChanged.get(tableName);
    }

    public int getRecordInserted() {
        // TODO da implementare
        return 0;
    }

    public int getRecordUpdated() {
        // TODO da implementare
        return 0;
    }

}
