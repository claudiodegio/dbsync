package com.claudiodegio.dbsync;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Status {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OK, ERROR_WRITING_TMP_DB, ERROR})
    public @interface Code {}
    static final int OK = 0;
    static final int ERROR = 100;
    static final int ERROR_WRITING_TMP_DB = 101;

    private @Code int mStatusCode;
    private String mMessage;

    public Status(@Code int statusCode, String message) {
        this.mStatusCode = statusCode;
        this.mMessage = message;
    }

    public Status(int statusCode) {
       this(statusCode, null);
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getStatusMessage() {
        return mMessage;
    }
}
