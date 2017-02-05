package com.claudiodegio.dbsync;


import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.Metadata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;

public class GDriveCloudProvider implements CloudProvider {

    private final static String TAG = "GDriveCloudProvider";

    private GoogleApiClient mGoogleApiClient;
    private DriveId mDriveId;

    private GDriveCloudProvider(final GoogleApiClient googleApiClient, DriveId driveId){
        this.mGoogleApiClient = googleApiClient;
        this.mDriveId = driveId;
    }

    @Override
    public void uploadFile(File tempFile) {
        DriveFile driveFile;
        MetadataResult metadataResult;
        Metadata metadata;
        DriveContents driveContents = null;
        DriveContentsResult driveContentsResult;
        OutputStream outputStream;

        Log.i(TAG, "start upload of DB file temp:" + tempFile.getName());

        try {
            driveFile = mDriveId.asDriveFile();

            // Get metadata
            metadataResult = driveFile.getMetadata(mGoogleApiClient).await();
            checkStatus(metadataResult);

            metadata = metadataResult.getMetadata();

            Log.i(TAG, "try to upload of DB file:" + metadata.getOriginalFilename());

            // Writing file
            driveContentsResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
            checkStatus(driveContentsResult);

            driveContents = driveContentsResult.getDriveContents();

            outputStream  = driveContents.getOutputStream();

            // Copio il file
            FileUtils.copyFile(tempFile, outputStream);
            Log.i(TAG, "try to commit file copy done");

            // Commit del file
            // TODO gestire il conflitto
            driveContents.commit(mGoogleApiClient, null).await();

            Log.i(TAG, "file committed");

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
        DriveContents driveContents = null;
        DriveContentsResult driveContentsResult;
        InputStream inputStream = null;

        Log.i(TAG, "start download of DB file");

        try {
            driveFile = mDriveId.asDriveFile();

            // Get metadata
            metadataResult = driveFile.getMetadata(mGoogleApiClient).await();
            checkStatus(metadataResult);

            // Writing file
            driveContentsResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            checkStatus(driveContentsResult);

            metadata = metadataResult.getMetadata();
            Log.i(TAG, "downloaded DB file:" + metadata.getOriginalFilename() + " modified on: " + metadata.getModifiedDate() + " size:" + metadata.getFileSize() + " bytes");

            driveContents = driveContentsResult.getDriveContents();

            inputStream  = driveContents.getInputStream();

            return inputStream;
        } catch (Exception e) {
            if (driveContents != null) {
                driveContents.discard(mGoogleApiClient);
            }
            throw new SyncException(SyncStatus.ERROR_DOWNLOAD_CLOUD, "Error reading file from GDrive message:" + e.getMessage());
        }
    }

    private void checkStatus(Result result) throws SyncException {
        if (!result.getStatus().isSuccess()) {
            throw new SyncException(SyncStatus.ERROR_UPLOAD_CLOUD, result.getStatus().getStatusMessage());
        }
    }

    public static class Builder {

        private GoogleApiClient mGoogleApiClient;


        private DriveId mDriveId;

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
            return new GDriveCloudProvider(mGoogleApiClient, mDriveId);
        }
    }
}
