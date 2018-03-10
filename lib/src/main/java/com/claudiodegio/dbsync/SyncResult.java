package com.claudiodegio.dbsync;

import com.claudiodegio.dbsync.core.RecordSyncResult;

/**
 * Result of sync process
 */
public class SyncResult {

    private SyncStatus mSyncStatus;
    private RecordSyncResult mResult;

    public SyncResult(SyncStatus syncStatus) {
        this.mSyncStatus = syncStatus;
    }

    public SyncResult(SyncStatus syncStatus, RecordSyncResult result) {
        this.mSyncStatus = syncStatus;
        this.mResult = result;
    }

    public SyncStatus getStatus() {
        return mSyncStatus;
    }

    public RecordSyncResult getResult() {
        return mResult;
    }
}
