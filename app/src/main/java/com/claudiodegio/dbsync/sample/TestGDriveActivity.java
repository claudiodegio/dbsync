package com.claudiodegio.dbsync.sample;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class TestGDriveActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;

    final static String TAG = "TestGDriveActivity";

    final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    final int REQUEST_CODE_CREATOR = 200;

    final static String RESOURCE_ID = "0B7R4nsyQgnmnOUtVaFFiWE9UcE0";

    @BindView(R.id.tvTimestamp)
    TextView mTvTimestamp;

    @BindView(R.id.tvDatetime)
    TextView mTvDatetime;

    @BindView(R.id.tvDatetimeTimeZone)
    TextView mTvDatetimeZone;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_gdrive_activity);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        ButterKnife.bind(this);

        init();
    }

    private void init(){
        long timestamp = System.currentTimeMillis();

        mTvTimestamp.setText(Long.toString(timestamp));

        Date date = new Date();

        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());

        mTvDatetime.setText(dateFormat.format(date));


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm z");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mTvDatetimeZone.setText(simpleDateFormat.format(date));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @OnClick(R.id.btGDrive)
    public void connectGDrive(){
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;

            case 120:

                DriveId driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);


                new ReadWriteAsync(driveId).execute();

                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed:" + connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();

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


    @OnClick(R.id.btGen)
    public void generateJson() {


        /*MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                .setMimeType("application/json")
                .build();
        IntentSender intentSender = Drive.DriveApi.newCreateFileActivityBuilder()
                .setActivityTitle("Crea")
                .setInitialMetadata(metadataChangeSet)
                .build(mGoogleApiClient);

        try {
            startIntentSenderForResult(intentSender, 1, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            // Handle the exception
            Log.e(TAG, "generateJson: ", e);
        }*/
    }

    public void readText() {
        Drive.DriveApi.fetchDriveId(mGoogleApiClient, RESOURCE_ID)
                .setResultCallback(mFetchResultCallBack);
    }

    private ResultCallback<DriveApi.DriveIdResult> mFetchResultCallBack = new ResultCallback<DriveApi.DriveIdResult>() {


        @Override
        public void onResult(@NonNull DriveApi.DriveIdResult driveIdResult) {

        }
    };


    @OnClick(R.id.btReadWrite)
    public void readAndWrite() {

        Log.e(TAG,"btReadWrite");

        try {
            IntentSender intentSender = Drive.DriveApi.newOpenFileActivityBuilder()
                    .setActivityTitle("Select file for test")
                    .build(mGoogleApiClient);

            startIntentSenderForResult(intentSender, 120, null, 0, 0, 0);
        } catch (Exception e) {
            Log.w(TAG, "Unable to send intent", e);
        }




    }

    class ReadWriteAsync extends AsyncTask {

        private DriveId mDriveId;

        public ReadWriteAsync(DriveId driveId) {
            this.mDriveId = driveId;
        }

        @Override
        protected Object doInBackground(Object[] params) {

            Log.e(TAG,"doInBackground");

            DriveFile driveFile;
            DriveContents driveContentsInput = null;
            DriveContents driveContentsOutput;

            List<String> lines = null;
            int lastValue = 0;

            try {

                driveFile = mDriveId.asDriveFile();

                driveContentsInput = driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await().getDriveContents();

                lines = IOUtils.readLines(driveContentsInput.getInputStream(), Charset.defaultCharset());

                System.out.println(lines.toString());

                lastValue = Integer.parseInt(lines.get(lines.size() -1));

               // driveContentsInput.discard(mGoogleApiClient);
            } catch (IOException e) {
                e.printStackTrace();

                Log.e(TAG, "eccezione read", e);
            }

            // Scrittura

            try {

                driveFile = mDriveId.asDriveFile();

             //   driveContentsOutput = driveFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await().getDriveContents();

                driveContentsOutput = driveContentsInput.reopenForWrite(mGoogleApiClient).await().getDriveContents();

                BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(driveContentsOutput.getOutputStream()));

                if (lines != null) {
                    for (String line : lines) {

                        outputStream.write(line);
                        outputStream.newLine();

                    }
                }

                outputStream.write(Integer.toString(lastValue + 1));
                outputStream.newLine();
                outputStream.close();

                ExecutionOptions executionOptions = new ExecutionOptions.Builder()
                        .setNotifyOnCompletion(true)
                        .setConflictStrategy(ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE)
                        .build();
                com.google.android.gms.common.api.Status status = driveContentsOutput.commit(mGoogleApiClient, null, executionOptions).await();


                System.out.println(status.isSuccess());

            } catch (IOException e) {
                e.printStackTrace();

                Log.e(TAG, "eccezione write", e);
            }



            return null;
        }
    }
}
