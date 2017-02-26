package com.claudiodegio.dbsync.provider;


import android.support.annotation.IntDef;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface CloudProvider {

    int UPLOAD_OK = 0;
    int UPLOAD_CONFLICT = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UPLOAD_OK, UPLOAD_CONFLICT})
    @interface UploadStatus {}


    @UploadStatus int uploadFile(File tempFile);

    InputStream downloadFile();

    void close();
}
