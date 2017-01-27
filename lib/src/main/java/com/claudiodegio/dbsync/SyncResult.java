package com.claudiodegio.dbsync;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SyncResult {

    private Status mStatus;

    public SyncResult(Status status) {
        this.mStatus = status;
    }

    public Status getStatus() {
        return mStatus;
    }
}
