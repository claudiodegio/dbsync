package com.claudiodegio.dbsync.provider;


import androidx.annotation.IntDef;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interface for generic cloud provider
 */
public interface CloudProvider {

    int UPLOAD_OK = 0;
    int UPLOAD_CONFLICT = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UPLOAD_OK, UPLOAD_CONFLICT})
    @interface UploadStatus {}

    /**
     * Function to upload of file
     * @param tempFile the temp file where save the file
     * @return UPLOAD_OK, UPLOAD_CONFLICT
     */
    @UploadStatus int uploadFile(File tempFile);

    /**
     * Function to download the file
     * @return the input stream of download file
     */
    InputStream downloadFile();

    /**
     * Close the provider
     */
    void close();
}
