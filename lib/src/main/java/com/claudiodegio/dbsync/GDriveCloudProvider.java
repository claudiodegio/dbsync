package com.claudiodegio.dbsync;


import com.google.android.gms.common.api.GoogleApiClient;

public class GDriveCloudProvider implements CloudProvider {

    private final static String TAG = "GDriveCloudProvider";

    private GoogleApiClient mGoogleApiClient;

    private GDriveCloudProvider(final GoogleApiClient googleApiClient){
        this.mGoogleApiClient = googleApiClient;
    }

    public static class Builder {

        private GoogleApiClient mGoogleApiClient;

        public void setGoogleApiClient(final GoogleApiClient googleApiClient) {
            this.mGoogleApiClient = googleApiClient;
        }

        public GDriveCloudProvider build(){
            return new GDriveCloudProvider(mGoogleApiClient);
        }
    }
}
