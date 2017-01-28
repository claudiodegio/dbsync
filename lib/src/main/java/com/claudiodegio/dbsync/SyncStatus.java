package com.claudiodegio.dbsync;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SyncStatus {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OK, ERROR_WRITING_TMP_DB, ERROR_UPLOAD_CLOUD, ERROR})
    public @interface Code {}
    static final int OK = 0;
    static final int ERROR = 100;
    static final int ERROR_WRITING_TMP_DB = 101;
    static final int ERROR_UPLOAD_CLOUD = 102;


    private @Code int mStatusCode;
    private String mMessage;

    public SyncStatus(@Code int statusCode, String message) {
        this.mStatusCode = statusCode;
        this.mMessage = message;
    }

    public SyncStatus(int statusCode) {
       this(statusCode, null);
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getStatusMessage() {
        return mMessage;
    }
}