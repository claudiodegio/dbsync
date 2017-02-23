package com.claudiodegio.dbsync.sample;


import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.SyncResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.util.Date;
import java.text.DateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dino.dbinspector.activities.DbInspectorActivity;

public abstract class BaseMainDbActivity extends BaseActivity implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = "BaseMainDbActivity";

    protected GoogleApiClient mGoogleApiClient;

    final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    final int REQUEST_CODE_SELECT_FILE = 200;
    final int REQUEST_CODE_NEW_FILE = 300;

    final String DRIVE_ID_FILE = "DRIVE_ID_FILE";

    @BindView(R.id.tvStatus)
    TextView mTvStatus;
    @BindView(R.id.tvStatus2)
    TextView mTvStatus2;
    @BindView(R.id.tvLastTimeStamp)
    TextView mTvLastSyncTimestamp;
    @BindView(R.id.tvCurrentTime)
    TextView mTvCurrentTime;

    @BindView(R.id.tvFileName)
    TextView mTvFileName;

    @BindView(R.id.btSync)
    Button mBtSync;
    @BindView(R.id.btSelectFileForSync)
    Button mBtSelectFileForSync;
    @BindView(R.id.btCreateFileForSync)
    Button mBtCreateFileForSync;
    @BindView(R.id.btResetLastSyncTimestamp)
    Button mBtResetLastSyncTimestamp;

    protected DriveId mDriveId;
    protected DBSync dbSync;

    Handler mMainHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);

        mMainHandler = new Handler();

        mMainHandler.post(new UpdateCurrentTime());

        String driveId = getPreferences(Context.MODE_PRIVATE).getString(DRIVE_ID_FILE, null);

        if (driveId != null) {
            mDriveId = DriveId.decodeFromString(driveId);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"onConnected");
        mTvStatus2.setText("GDrive Client - Connected");
        mBtSelectFileForSync.setEnabled(true);
        mBtCreateFileForSync.setEnabled(true);

        if (mDriveId != null) {
            mTvStatus2.setText("GDrive Client - Connected - File Selected");
            mBtSync.setEnabled(true);
            mBtResetLastSyncTimestamp.setEnabled(true);
            readMetadata();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed:" + connectionResult.getErrorMessage());

        // Viene chiamata nel caso la connect fallisca ad esempio
        // non è ancora stata data autorizzaiozne alla applicazione corrente
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
            case REQUEST_CODE_SELECT_FILE:
            case REQUEST_CODE_NEW_FILE:
                if (resultCode == RESULT_OK) {

                    mDriveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                    Log.d(TAG, "driveId: " + mDriveId.encodeToString());

                    SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                    editor.putString(DRIVE_ID_FILE, mDriveId.encodeToString());
                    editor.commit();

                    mTvStatus2.setText("GDrive Client - Connected - File Selected");
                    mBtSync.setEnabled(true);
                    mBtResetLastSyncTimestamp.setEnabled(true);
                    readMetadata();
                }
                break;
        }
    }

    @OnClick(R.id.btSelectFileForSync)
    public void actionSelectFileForSync() {
        try {
            IntentSender intentSender = Drive.DriveApi.newOpenFileActivityBuilder()
                    .setActivityTitle("Select file for sync")
                    .build(mGoogleApiClient);

            startIntentSenderForResult(intentSender, REQUEST_CODE_SELECT_FILE, null, 0, 0, 0);
        } catch (Exception e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }


    @OnClick(R.id.btCreateFileForSync)
    public void actionCreateFileForSync() {
        try {
            MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                    .setMimeType("text/plain").build();
            IntentSender intentSender = Drive.DriveApi.newCreateFileActivityBuilder()
                    .setInitialMetadata(metadataChangeSet)
                    .setInitialDriveContents(null)
                    .setActivityTitle("Create file for sync")
                    .build(mGoogleApiClient);

            startIntentSenderForResult(intentSender, REQUEST_CODE_NEW_FILE, null, 0, 0, 0);
        } catch (Exception e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }
    protected void updateLastSyncTimeStamp(){
        if (dbSync != null) {
            long lastSyncStatus = dbSync.getLastSyncTimestamp();

            Date date = new Date(lastSyncStatus);

            mTvLastSyncTimestamp.setText("Timestamp: " + lastSyncStatus + " - " +Utility.formatDateTimeNoTimeZone(date));
        }
    }

    @OnClick(R.id.btResetLastSyncTimestamp)
    public void resetLastSyncTimestamp(){

        if (dbSync != null) {
            dbSync.resetLastSyncTimestamp();
            updateLastSyncTimeStamp();
        }
    }

    @OnClick(R.id.btToDbManager)
    public void goToDBManager(){
        startActivity(new Intent(this, DbInspectorActivity.class));
    }

    @OnClick(R.id.btSync)
    public void btSync(){
        mTvStatus.setText("In Progress...");
        new SyncTask().execute();
    }

    public abstract void onPostSync();

    private void readMetadata(){

       mDriveId.asDriveFile().getMetadata(mGoogleApiClient)
               .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                   @Override
                   public void onResult(@NonNull DriveResource.MetadataResult metadataResult) {

                       Metadata metadata = metadataResult.getMetadata();

                       mTvFileName.setText("File name: " + metadata.getOriginalFilename());
                   }
               });

    }

    class UpdateCurrentTime implements Runnable {

        @Override
        public void run() {
            mTvCurrentTime.setText("CurrentTime: " +Utility.formatDateTimeNoTimeZone(new Date()));

            mMainHandler.postDelayed(this, 500);
        }
    }

    class SyncTask extends AsyncTask<Void, Void, SyncResult> {

        @Override
        protected SyncResult doInBackground(Void... params) {
            return  dbSync.sync();
        }

        @Override
        protected void onPostExecute(SyncResult result) {

            if (result.getStatus().isSuccess()) {
                mTvStatus.setText("OK - Insert: " +  result.getCounter().getRecordInserted() + " - Update: " +  result.getCounter().getRecordUpdated());
                updateLastSyncTimeStamp();
            } else {
                mTvStatus.setText("Fail: " + result.getStatus().getStatusCode() + "\n" + result.getStatus().getStatusMessage());
                mTvLastSyncTimestamp.setText("");
            }
        }
    }
}
