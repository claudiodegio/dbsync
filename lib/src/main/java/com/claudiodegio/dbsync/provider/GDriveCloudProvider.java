package com.claudiodegio.dbsync.provider;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.claudiodegio.dbsync.exception.SyncBuildException;
import com.claudiodegio.dbsync.exception.SyncException;
import com.claudiodegio.dbsync.SyncStatus;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Cloud provider implementation for Driver
 */
public class GDriveCloudProvider implements CloudProvider {

    private final static String TAG = "GDriveCloudProvider";


    final private Context mCtx;

    // New version
    private final Drive mDriveService;
    private final String mNewDriveID;
    private com.google.api.services.drive.model.File metadata;

    private GDriveCloudProvider(final Context ctx,
                                final Drive driveService,
                                final String newDriveId){
        this.mDriveService = driveService;
        this.mNewDriveID = newDriveId;
        this.mCtx = ctx;
    }

    @Override
    public int uploadFile(File tempFile) {


        com.google.api.services.drive.model.File fileMeta;

        try {
            Log.i(TAG, "start upload of DB file temp:" + tempFile.getName());

            mDriveService
                    .files()
                    .update(mNewDriveID, null, new FileContent("text/json", tempFile))
                    .execute();

            Log.i(TAG, "file uploaded ");

            return UPLOAD_OK;

        } catch (Exception e) {
            Log.e(TAG, "uploadFile: " + e.getMessage());
            e.printStackTrace();
            throw new SyncException(SyncStatus.Code.ERROR_UPLOAD_CLOUD, "Error writing file to GDrive message:" + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile() {
        InputStream inputStream;
        Log.i(TAG, "start download of DB file:" + mNewDriveID);

        try {

            // Get metadata
            metadata = mDriveService.files()
                    .get(mNewDriveID)
                    .setFields("version,size,name,mimeType,kind,modifiedTime")
                    .execute();

            Log.i(TAG, "downloaded DB file:" + metadata.getName() + " modified on: " + metadata.getModifiedTime() + " size:" + metadata.getSize() + " bytes version:" + metadata.getVersion());

            if (metadata.getSize() < 3) {
                return null;
            }

            inputStream = mDriveService.files()
                    .get(mNewDriveID)
                    .executeMediaAsInputStream();

            return inputStream;
        }  catch (Exception e) {

            Log.e(TAG, "downloadFile: " + e.getMessage());

            throw new SyncException(SyncStatus.Code.ERROR_DOWNLOAD_CLOUD, "Error reading file from Drive message:" + e.getMessage());

        }
    }

    @Override
    public void close() {
    }


    /**
     * Builder class for Drive Provider
     */
    public static class Builder {

        private Context mCtx;

        private Drive mDriveService;
        private String mNewDriveID;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        public Builder setDriveService(Drive mDriveService) {
            this.mDriveService = mDriveService;
            return this;
        }

        public Builder setNewDriveID(String mNewDriveID) {
            this.mNewDriveID = mNewDriveID;
            return this;
        }

        /**
         * Build a new GDriveCloudProvider
         * @return the new created cloud provider
         */
        public GDriveCloudProvider build(){

            return new GDriveCloudProvider(mCtx, mDriveService, mNewDriveID);
        }
    }

}
