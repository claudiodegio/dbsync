package com.claudiodegio.dbsync;

public class SyncResult {

    private SyncStatus mSyncStatus;
    private DatabaseCounter mCounter;

    public SyncResult(SyncStatus syncStatus) {
        this.mSyncStatus = syncStatus;
    }

    public SyncResult(SyncStatus syncStatus, DatabaseCounter counter) {
        this.mSyncStatus = syncStatus;
        this.mCounter = counter;
    }

    public SyncStatus getStatus() {
        return mSyncStatus;
    }

    public DatabaseCounter getCounter() {
        return mCounter;
    }
}
