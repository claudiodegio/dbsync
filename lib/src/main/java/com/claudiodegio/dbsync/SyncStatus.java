package com.claudiodegio.dbsync;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SyncStatus {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OK, ERROR_WRITING_TMP_DB, ERROR_UPLOAD_CLOUD, ERROR_GENERATE_UUID, ERROR_DOWNLOAD_CLOUD, ERROR_SYNC_COULD_DB, ERROR, ERROR_NEW_SCHEMA_VERSION})
    public @interface Code {}
    static final int OK = 0;
    static final public int ERROR = 100;
    static final public int ERROR_WRITING_TMP_DB = 101;
    static final public int ERROR_UPLOAD_CLOUD = 102;
    static final public int ERROR_GENERATE_UUID = 103;
    static final public int ERROR_DOWNLOAD_CLOUD = 104;
    static final public int ERROR_SYNC_COULD_DB = 105;
    static final public int ERROR_NEW_SCHEMA_VERSION = 106;

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

    public boolean isSuccess(){
        return mStatusCode == OK;
    }
}
