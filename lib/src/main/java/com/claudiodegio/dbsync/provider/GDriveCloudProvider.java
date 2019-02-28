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

    final private DriveResourceClient mDriveResourceClient;

    final private DriveId mDriveId;
    final private Context mCtx;
    private DriveContents mDriveContent;
    private GDriveCompletionReceiver mGDriveCompletionRecevier;

    // New version
    private final Drive mDriveService;
    private final String mNewDriveID;

    private GDriveCloudProvider(final Context ctx,
                                final Drive driveService,
                                final String newDriveId,
                                final DriveResourceClient driveResourceClient,
                                final DriveId driveId){
        this.mDriveId = driveId;
        this.mDriveService = driveService;
        this.mNewDriveID = newDriveId;
        this.mCtx = ctx;
        this.mGDriveCompletionRecevier = new GDriveCompletionReceiver();
        this.mCtx.registerReceiver(mGDriveCompletionRecevier, new IntentFilter(GDriveEventService.CUSTOM_INTENT));
        this.mDriveResourceClient = driveResourceClient;
    }

    @Override
    public int uploadFile(File tempFile) {
        DriveFile driveFile;
        Metadata metadata;
        OutputStream outputStream;
        ExecutionOptions executionOptions;
        int complentionStatus;
        Task<Metadata> taskMetadata;
        Task<DriveContents> taskDriveContent;

        Log.i(TAG, "start upload of DB file temp:" + tempFile.getName());

        try {
            driveFile = mDriveId.asDriveFile();

            // Get metadata
            taskMetadata = mDriveResourceClient.getMetadata(driveFile);
            metadata = Tasks.await(taskMetadata);

            Log.i(TAG, "try to upload of DB file:" + metadata.getOriginalFilename());

            if (!metadata.isPinned()) {
                pinFile(driveFile);
            }

            // Writing file
            taskDriveContent = mDriveResourceClient.reopenContentsForWrite(mDriveContent);
            mDriveContent = Tasks.await(taskDriveContent);

            outputStream  = mDriveContent.getOutputStream();

            // Copio il file
            FileUtils.copyFile(tempFile, outputStream);
            outputStream.close();
            Log.i(TAG, "try to commit file copy done");

            // Commit del file
            executionOptions = new ExecutionOptions.Builder()
                    .setNotifyOnCompletion(true)
                    .setConflictStrategy(ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE)
                    .build();

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setLastViewedByMeDate(new Date())
                    .build();

            mDriveResourceClient.commitContents(mDriveContent, changeSet, executionOptions);

            Log.i(TAG, "file committed - wait for completion");

            // Wait for complention
            complentionStatus = mGDriveCompletionRecevier.waitCompletion();

            Log.i(TAG, "received completion status:" + complentionStatus);

            if (complentionStatus == CompletionEvent.STATUS_CONFLICT) {
                return UPLOAD_CONFLICT;
            } else if (complentionStatus == CompletionEvent.STATUS_FAILURE) {
                throw new SyncException(SyncStatus.Code.ERROR_UPLOAD_CLOUD, "Error writing file to GDrive (FAILURE of commit)");
            }

            return UPLOAD_OK;
        } catch (Exception e) {
            if (mDriveContent != null) {
                mDriveResourceClient.discardContents(mDriveContent);
            }
            Log.e(TAG, "uploadFile: " + e.getMessage());
            e.printStackTrace();
            throw new SyncException(SyncStatus.Code.ERROR_UPLOAD_CLOUD, "Error writing file to GDrive message:" + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile() {
        DriveFile driveFile;
        Metadata metadata;
        InputStream inputStream;
        Task<Metadata> taskMetadata;
        Task<DriveContents> taskDriveContent;

        Log.i(TAG, "start download of DB file");

        try {
            mDriveContent = null;
            driveFile = mDriveId.asDriveFile();

            // Get metadata
            taskMetadata = mDriveResourceClient.getMetadata(driveFile);
            metadata = Tasks.await(taskMetadata);

            // Reading file
            taskDriveContent = mDriveResourceClient.openFile(driveFile,DriveFile.MODE_READ_ONLY);
            mDriveContent = Tasks.await(taskDriveContent);

            Log.i(TAG, "downloaded DB file:" + metadata.getOriginalFilename() + " modified on: " + metadata.getModifiedDate() + " size:" + metadata.getFileSize() + " bytes");

            inputStream  = mDriveContent.getInputStream();

            if (metadata.getFileSize() < 3) {
                inputStream.close();
                return null;
            }

            return inputStream;
        } catch (Exception e) {
            if (mDriveContent != null) {
                mDriveResourceClient.discardContents(mDriveContent);
            }

            Log.e(TAG, "downloadFile: " + e.getMessage());

            throw new SyncException(SyncStatus.Code.ERROR_DOWNLOAD_CLOUD, "Error reading file from GDrive message:" + e.getMessage());
        }
    }

    @Override
    public void close() {
        this.mCtx.unregisterReceiver(mGDriveCompletionRecevier);
    }

    private void pinFile(final DriveFile file) throws Exception {
        MetadataChangeSet changeSet;
        Log.i(TAG, "set file and pinned");

        changeSet = new MetadataChangeSet.Builder()
                .setPinned(true)
                .build();
        Tasks.await(mDriveResourceClient.updateMetadata(file, changeSet));
    }


    private class GDriveCompletionReceiver extends BroadcastReceiver {

        private int result;
        private boolean eventToConsume = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            final DriveId driveId = intent.getParcelableExtra(GDriveEventService.BUNDLE_DRIVEID);

            Log.i(TAG, "onReceive completion event for driveID:" + driveId.encodeToString());

            Log.i(TAG, "startDriveId:" + mDriveId);

            if (driveId.equals(mDriveId)) {
                result =  intent.getIntExtra(GDriveEventService.BUNDLE_SUCCESS, -1);
                synchronized (this) {
                    Log.i(TAG, "onReceive notify result:" + result);
                    eventToConsume = true;
                    notify();
                }
            }
        }

        private int waitCompletion(){

            Log.i(TAG, "waitCompletion wait");

            try {

                if (!eventToConsume) {
                    synchronized (this) {
                        Log.i(TAG, "waitCompletion wait");
                        wait(2000);
                    }
                }

            } catch (InterruptedException ignored) { }

            eventToConsume = false;
            return result;
        }
    }

    /**
     * Builder class for Drive Provider
     */
    public static class Builder {

        private DriveResourceClient mDriveResourceClient;
        private Context mCtx;
        private DriveId mDriveId;

        private Drive mDriveService;
        private String mNewDriveID;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        /**
         * Set the to use to sync
         * @param driveId the drive file
         */
        public Builder setSyncFileByDriveId(DriveId driveId) {
            this.mDriveId = driveId;
            return this;
        }

        /**
         * Set the file to use to sync
         * @param s the drive file as encoded string
         */
        public Builder setSyncFileByString(String s) {
            this.mDriveId = DriveId.decodeFromString(s);
            return this;
        }


        /**
         * Set driver resource client
         * @param mDriveResourceClient the drive resource client
         */
        public Builder setDriveResourceClient(DriveResourceClient mDriveResourceClient) {
            this.mDriveResourceClient = mDriveResourceClient;
            return this;
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

            return new GDriveCloudProvider(mCtx, mDriveService, mNewDriveID, mDriveResourceClient, mDriveId);
        }
    }

}
