package com.claudiodegio.dbsync;

public class SyncResult {

    private SyncStatus mSyncStatus;

    public SyncResult(SyncStatus syncStatus) {
        this.mSyncStatus = syncStatus;
    }

    public SyncStatus getStatus() {
        return mSyncStatus;
    }
}
