package com.claudiodegio.dbsync;



public class SyncException extends RuntimeException {

    private SyncStatus mSyncStatus;

    public SyncException(@SyncStatus.Code int statusCode, String message) {
        super(message);
        this.mSyncStatus = new SyncStatus(statusCode, message);
    }

    public SyncException(@SyncStatus.Code int statusCode, Exception ex) {
        super(ex);
        this.mSyncStatus = new SyncStatus(statusCode, ex.getMessage());
    }

    public SyncStatus getStatus() {
        return mSyncStatus;
    }
}
