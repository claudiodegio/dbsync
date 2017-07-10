package com.claudiodegio.dbsync;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Sync status
 */
public class SyncStatus {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Code.OK, Code.ERROR_WRITING_TMP_DB, Code.ERROR_UPLOAD_CLOUD, Code.ERROR_GENERATE_UUID, Code.ERROR_DOWNLOAD_CLOUD, Code.ERROR_SYNC_COULD_DB, Code.ERROR, Code.ERROR_NEW_SCHEMA_VERSION})
    public @interface Code {
        int OK = 0;
        int ERROR = 100;
        int ERROR_WRITING_TMP_DB = 101;
        int ERROR_UPLOAD_CLOUD = 102;
        int ERROR_GENERATE_UUID = 103;
        int ERROR_DOWNLOAD_CLOUD = 104;
        int ERROR_SYNC_COULD_DB = 105;
        int ERROR_NEW_SCHEMA_VERSION = 106;
    }

    private @Code int mStatusCode;
    private String mMessage;

    public SyncStatus(@Code int statusCode, String message) {
        this.mStatusCode = statusCode;
        this.mMessage = message;
    }

    public SyncStatus(@Code int statusCode) {
       this(statusCode, null);
    }

    /**
     * Retrun the status code
     * @return the status code
     */
    @Code
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * Return the error message on error
     * @return
     */
    public String getStatusMessage() {
        return mMessage;
    }

    /**
     * Return if operation is success or not
     * @return true on success otherwise false
     */
    public boolean isSuccess(){
        return mStatusCode == Code.OK;
    }
}
