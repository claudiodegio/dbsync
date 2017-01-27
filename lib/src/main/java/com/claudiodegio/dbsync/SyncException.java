package com.claudiodegio.dbsync;



public class SyncException extends RuntimeException {

    private Status mStatus;

    public SyncException(@Status.Code int statusCode, String message) {
        super(message);
        this.mStatus = new Status(statusCode, message);
    }

    public Status getStatus() {
        return mStatus;
    }
}
