package com.claudiodegio.dbsync.sample.db1;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.widget.Toast;

import com.claudiodegio.dbsync.CloudProvider;
import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.SyncResult;
import com.claudiodegio.dbsync.GDriveCloudProvider;
import com.claudiodegio.dbsync.TableToSync;
import com.claudiodegio.dbsync.sample.BaseActivity;
import com.claudiodegio.dbsync.sample.R;
import com.claudiodegio.dbsync.sample.tablemanager.TableViewerFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dino.dbinspector.activities.DbInspectorActivity;

public class MainDb1Activity extends BaseActivity implements TableViewerFragment.OnItemClicked, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = "MainDb1Activity";

    TableViewerFragment mFragment;

    GoogleApiClient mGoogleApiClient;

    final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    final int REQUEST_CODE_SELECT_FILE = 200;
    final String DRIVE_ID_FILE = "DRIVE_ID_FILE";

    @BindView(R.id.tvStatus)
    TextView mTvStatus;
    @BindView(R.id.tvStatus2)
    TextView mTvStatus2;
    @BindView(R.id.tvLastTimeStamp)
    TextView mTvLastSyncTimestamp;
    @BindView(R.id.tvCurrentTime)
    TextView mTvCurrentTime;


    @BindView(R.id.btSync)
    Button mBtSync;
    @BindView(R.id.btSelectFileForSync)
    Button mBtSelectFileForSync;
    @BindView(R.id.btResetLastSyncTimestamp)
    Button mBtResetLastSyncTimestamp;

    private DriveId mDriveId;
    private DBSync dbSync;

    private  DateFormat mDateFormat;
    private  Handler mMainHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db1_main);
        ButterKnife.bind(this);

        mFragment = TableViewerFragment.newInstance("db1.db", "name");
        mFragment.setOnItemClicked(this);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.flFragment, mFragment, "TAG").commit();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        mDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));

         mMainHandler = new Handler();

        mMainHandler.post(new UpdateCurrentTime());
    }

    @OnClick(R.id.btToDbManager)
    public void goToDBManager(){
        startActivity(new Intent(this, DbInspectorActivity.class));
    }

    @OnClick(R.id.btInsertName)
    public void goInsertName(){
        startActivity(new Intent(this, InsertNameActivity.class));
    }

    @OnClick(R.id.btSync)
    public void startDbSync(){

        mTvStatus.setText("In Progress");

        CloudProvider gDriveProvider = new GDriveCloudProvider.Builder(this.getBaseContext())
                .setSyncFileByDriveId(mDriveId)
                .setGoogleApiClient(mGoogleApiClient)
                .build();

        dbSync = new DBSync.Builder(this.getBaseContext())
                .setCloudProvider(gDriveProvider)
                .setSQLiteDatabase(app.db1OpenHelper.getWritableDatabase())
                .setDataBaseName(app.db1OpenHelper.getDatabaseName())
                .addTable(new TableToSync.Builder("name").build())
                .build();

        new SyncTask().execute();
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
    protected void onRestart() {
        super.onRestart();
        mFragment.reload();
    }

    @Override
    public void onItemClicked(long id, String [] data) {
        Toast.makeText(this, "" + id, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, InsertNameActivity.class);
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"onConnected");
        mTvStatus2.setText("GDrive Client - Connected");
        mBtSelectFileForSync.setEnabled(true);

        String driveId = getPreferences(Context.MODE_PRIVATE).getString(DRIVE_ID_FILE, null);

        if (driveId != null) {
            mDriveId = DriveId.decodeFromString(driveId);
            mTvStatus2.setText("GDrive Client - Connected - File Selected");
            mBtSync.setEnabled(true);
            mBtResetLastSyncTimestamp.setEnabled(true);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
            case REQUEST_CODE_SELECT_FILE:

                mDriveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                editor.putString(DRIVE_ID_FILE, mDriveId.encodeToString());
                editor.commit();

                System.out.println(mDriveId.encodeToString());

                mTvStatus2.setText("GDrive Client - Connected - File Selected");
                mBtSync.setEnabled(true);
                mBtResetLastSyncTimestamp.setEnabled(true);
                break;
        }
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

    class SyncTask extends AsyncTask<Void, Void, SyncResult> {

        @Override
        protected SyncResult doInBackground(Void... params) {
            return   dbSync.sync();
        }

        @Override
        protected void onPostExecute(SyncResult result) {

            if (result.getStatus().isSuccess()) {
                mTvStatus.setText("OK\nInsert: " +  result.getCounter().getRecordInserted() + "\nUpdate: " +  result.getCounter().getRecordUpdated());
                updateLastSyncTimeStamp();
            } else {
                mTvStatus.setText("Fail: " + result.getStatus().getStatusCode() + "\n" + result.getStatus().getStatusMessage());
                mTvLastSyncTimestamp.setText("");
            }

           // Toast.makeText(app, "Completed", Toast.LENGTH_SHORT).show();
            mFragment.reload();
        }
    }


    private void updateLastSyncTimeStamp(){

        if (dbSync != null) {
            long lastSyncStatus = dbSync.getLastSyncTimestamp();

            Date date = new Date(lastSyncStatus);

            mTvLastSyncTimestamp.setText("Timestamp: " + lastSyncStatus + " - " +mDateFormat.format(date));

        }

    }

    @OnClick(R.id.btResetLastSyncTimestamp)
    public void resetLastSyncTimestamp(){

        if (dbSync != null) {
            dbSync.resetLastSyncTimestamp();
            updateLastSyncTimeStamp();
        }
    }


    class UpdateCurrentTime implements Runnable {

        @Override
        public void run() {
            mTvCurrentTime.setText("CurrentTime: " +mDateFormat.format(new Date()));

            mMainHandler.postDelayed(this, 500);
        }
    }
}
