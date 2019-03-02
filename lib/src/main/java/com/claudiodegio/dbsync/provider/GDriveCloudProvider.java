package com.claudiodegio.dbsync.provider;


import android.content.Context;
import android.util.Log;

import com.claudiodegio.dbsync.exception.SyncException;
import com.claudiodegio.dbsync.SyncStatus;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;

import java.io.File;
import java.io.InputStream;

/**
 * Cloud provider implementation for Driver
 */
public class GDriveCloudProvider implements CloudProvider {

    private final static String TAG = "GDriveCloudProvider";


    final private Context mCtx;

    // New version
    private final Drive mDriveService;
    private final String mNewDriveID;
    private String md5Sum;

    private GDriveCloudProvider(final Context ctx,
                                final Drive driveService,
                                final String newDriveId){
        this.mDriveService = driveService;
        this.mNewDriveID = newDriveId;
        this.mCtx = ctx;
    }

    @Override
    public int uploadFile(File tempFile) {


        com.google.api.services.drive.model.File fileMetadata;
        String md5sumCurrent;

        try {
            Log.i(TAG, "start upload of DB file temp:" + tempFile.getName());

            // Check collision
            fileMetadata = mDriveService.files()
                    .get(mNewDriveID)
                    .setFields("md5Checksum")
                    .execute();


            md5sumCurrent = fileMetadata.getMd5Checksum();

            Log.i(TAG, "md5Checksum to check " + md5sumCurrent);

            if (!md5sumCurrent.equals(md5Sum)) {
                return UPLOAD_CONFLICT;
            }

            mDriveService
                    .files()
                    .update(mNewDriveID, null, new FileContent("text/json", tempFile))
                    .execute();

            Log.i(TAG, "file uploaded ");

            return UPLOAD_OK;

        } catch (Exception e) {
            Log.e(TAG, "uploadFile: " + e.getMessage());
            throw new SyncException(SyncStatus.Code.ERROR_UPLOAD_CLOUD, "Error writing file to GDrive message:" + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile() {
        InputStream inputStream;
        com.google.api.services.drive.model.File metadata;
        Log.i(TAG, "start download of DB file:" + mNewDriveID);

        try {

            // Get metadata
            metadata = mDriveService.files()
                    .get(mNewDriveID)
                    .setFields("version,size,name,mimeType,kind,modifiedTime,md5Checksum")
                    .execute();

            Log.i(TAG, "downloaded DB file:" + metadata.getName() + " modified on: " + metadata.getModifiedTime() + " size:" + metadata.getSize() + " bytes version:" + metadata.getVersion());

            md5Sum = metadata.getMd5Checksum();

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
        private String mDriveID;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        public Builder setDriveService(Drive mDriveService) {
            this.mDriveService = mDriveService;
            return this;
        }

        public Builder setDriveID(String mNewDriveID) {
            this.mDriveID = mNewDriveID;
            return this;
        }

        /**
         * Build a new GDriveCloudProvider
         * @return the new created cloud provider
         */
        public GDriveCloudProvider build(){

            return new GDriveCloudProvider(mCtx, mDriveService, mDriveID);
        }
    }

}
