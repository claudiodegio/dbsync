package com.claudiodegio.dbsync;



public class SyncException extends RuntimeException {

    private SyncStatus mSyncStatus;

    public SyncException(@SyncStatus.Code int statusCode, String message) {
        super(message);
        this.mSyncStatus = new SyncStatus(statusCode, message);
    }

    public SyncStatus getStatus() {
        return mSyncStatus;
    }
}
