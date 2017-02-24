package com.claudiodegio.dbsync;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.CompletionEvent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GDriveCloudProvider implements CloudProvider {

    private final static String TAG = "GDriveCloudProvider";

    final private GoogleApiClient mGoogleApiClient;
    final private DriveId mDriveId;
    final private Context mCtx;
    private DriveContents mDriveContent;
    private GDriveCompletionRecevier mGDriveCompletionRecevier;

    private GDriveCloudProvider(final Context ctx, final GoogleApiClient googleApiClient, final DriveId driveId){
        this.mGoogleApiClient = googleApiClient;
        this.mDriveId = driveId;
        this.mCtx = ctx;
        this.mGDriveCompletionRecevier = new GDriveCompletionRecevier();
        this.mCtx.registerReceiver(mGDriveCompletionRecevier, new IntentFilter(GDriveEventService.CUSTOM_INTENT));
    }

    @Override
    public int uploadFile(File tempFile) {
        DriveFile driveFile;
        MetadataResult metadataResult;
        Metadata metadata;
        DriveContents driveContents = null;
        DriveContentsResult driveContentsResult;
        OutputStream outputStream;
        ExecutionOptions executionOptions;
        int complentionStatus;

        Log.i(TAG, "start upload of DB file temp:" + tempFile.getName());

        try {
            driveFile = mDriveId.asDriveFile();

            // Get metadata
            metadataResult = driveFile.getMetadata(mGoogleApiClient).await();
            checkStatus(metadataResult);

            metadata = metadataResult.getMetadata();

            Log.i(TAG, "try to upload of DB file:" + metadata.getOriginalFilename());

            if (!metadata.isPinned()) {
                pinFile(driveFile);
            }

            // Writing file
            driveContentsResult = mDriveContent.reopenForWrite(mGoogleApiClient).await();
            checkStatus(driveContentsResult);

            driveContents = driveContentsResult.getDriveContents();

            outputStream  = driveContents.getOutputStream();

            // Copio il file
            FileUtils.copyFile(tempFile, outputStream);
            Log.i(TAG, "try to commit file copy done");

            // Commit del file
            // TODO gestire il conflitto
            executionOptions = new ExecutionOptions.Builder()
                    .setNotifyOnCompletion(true)
                    .setConflictStrategy(ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE)
                    .build();

            driveContents.commit(mGoogleApiClient, null, executionOptions);

            Log.i(TAG, "file committed - wait for complention");

            // Wait for complention
            complentionStatus = mGDriveCompletionRecevier.waitCompletion();

            Log.i(TAG, "received complention status:" + complentionStatus);

            if (complentionStatus == CompletionEvent.STATUS_CONFLICT) {
                return CloudProvider.UPLOAD_CONFLICT;
            } else if (complentionStatus == CompletionEvent.STATUS_FAILURE) {
                throw new SyncException(SyncStatus.ERROR_UPLOAD_CLOUD, "Error writing file to GDrive (FAILURE of commit)");
            }

            return CloudProvider.UPLOAD_OK;
        } catch (IOException e) {
            if (driveContents != null) {
                driveContents.discard(mGoogleApiClient);
            }
            throw new SyncException(SyncStatus.ERROR_UPLOAD_CLOUD, "Error writing file to GDrive message:" + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile() {
        DriveFile driveFile;
        MetadataResult metadataResult;
        Metadata metadata;
        DriveContentsResult driveContentsResult;
        InputStream inputStream = null;

        Log.i(TAG, "start download of DB file");

        try {
            mDriveContent = null;
            driveFile = mDriveId.asDriveFile();

            // Get metadata
            metadataResult = driveFile.getMetadata(mGoogleApiClient).await();
            checkStatus(metadataResult);

            // Writing file
            driveContentsResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            checkStatus(driveContentsResult);

            metadata = metadataResult.getMetadata();

            Log.i(TAG, "downloaded DB file:" + metadata.getOriginalFilename() + " modified on: " + metadata.getModifiedDate() + " size:" + metadata.getFileSize() + " bytes");

            mDriveContent = driveContentsResult.getDriveContents();

            inputStream  = mDriveContent.getInputStream();

            if (metadata.getFileSize() < 3) {
                inputStream.close();
                return null;
            }

            return inputStream;
        } catch (Exception e) {
            if (mDriveContent != null) {
                mDriveContent.discard(mGoogleApiClient);
            }
            throw new SyncException(SyncStatus.ERROR_DOWNLOAD_CLOUD, "Error reading file from GDrive message:" + e.getMessage());
        }
    }

    @Override
    public void close() {
        this.mCtx.unregisterReceiver(mGDriveCompletionRecevier);
    }

    private void checkStatus(Result result) throws SyncException {
        if (!result.getStatus().isSuccess()) {
            throw new SyncException(SyncStatus.ERROR_UPLOAD_CLOUD, result.getStatus().getStatusMessage());
        }
    }


    private void pinFile(final DriveFile file){
        MetadataChangeSet changeSet;
        MetadataResult metadataResult;


        Log.i(TAG, "set file and pinned");

        changeSet = new MetadataChangeSet.Builder()
                .setPinned(true)
                .build();

        metadataResult = file.updateMetadata(mGoogleApiClient, changeSet).await();

        checkStatus(metadataResult);
    }


    private class GDriveCompletionRecevier extends BroadcastReceiver {

        private int result;
        @Override
        public void onReceive(Context context, Intent intent) {
            final DriveId driveId = intent.getParcelableExtra(GDriveEventService.BUNDLE_DRIVEID);

            Log.i(TAG, "onReceive complention event for driveID:" + driveId.encodeToString());

            if (driveId.equals(mDriveId)) {
                result =  intent.getIntExtra(GDriveEventService.BUNDLE_SUCCESS, -1);
                synchronized (this) {
                    notify();
                }
            }
        }

        private int waitCompletion(){

            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException ignored) { }

            return result;
        }
    }

    public static class Builder {

        private GoogleApiClient mGoogleApiClient;
        private Context mCtx;

        private DriveId mDriveId;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        public Builder setGoogleApiClient(final GoogleApiClient googleApiClient) {
            this.mGoogleApiClient = googleApiClient;
            return this;
        }

        public Builder setSyncFileByDriveId(DriveId driveId) {
            this.mDriveId = driveId;
            return this;
        }

        public Builder setSyncFileByString(String s) {
            this.mDriveId = DriveId.decodeFromString(s);
            return this;
        }

        public GDriveCloudProvider build(){
            // TODO check
            return new GDriveCloudProvider(mCtx, mGoogleApiClient, mDriveId);
        }
    }

}
