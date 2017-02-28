package com.claudiodegio.dbsync.core;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DatabaseCounter extends RecordCounter {
    private Map<String, RecordCounter> mCounterTable;

    public DatabaseCounter() {
        this.mCounterTable = new HashMap<>();
    }

    public int getTableSyncedCount(){
        return mCounterTable.size();
    }

    public Set<String> getTableSynced(){
        return mCounterTable.keySet();
    }

    public RecordCounter findOrCreateTableCounter(final String tableName) {

        if (!mCounterTable.containsKey(tableName)) {
            mCounterTable.put(tableName, new RecordCounter());
        }

        return mCounterTable.get(tableName);
    }
}
