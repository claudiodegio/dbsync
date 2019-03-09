package com.claudiodegio.dbsync.sample;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.claudiodegio.dbsync.DBSync;
import com.claudiodegio.dbsync.SyncResult;
import com.claudiodegio.dbsync.core.RecordChanged;
import com.claudiodegio.dbsync.core.RecordSyncResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dino.dbinspector.activities.DbInspectorActivity;

public abstract class BaseMainDbActivity extends BaseActivity {

    private final static String TAG = "BaseMainDbActivity";

    protected GoogleSignInClient mGoogleSignInClient;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    final int REQUEST_CODE_SIGN_IN = 101;
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

    protected DBSync dbSync;

    Handler mMainHandler;

    protected String driveId = null;

    protected  com.google.api.services.drive.Drive googleDriveService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);

        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            // Use the last signed in account here since it already have a Drive scope.
            handleSignInONRecnnect();

            successSingIn();
        } else {
            signIn();
        }

        mMainHandler = new Handler();

        mMainHandler.post(new UpdateCurrentTime());

        driveId = getPreferences(Context.MODE_PRIVATE).getString(DRIVE_ID_FILE, null);

        if (driveId != null) {
            mBtSync.setEnabled(true);
            mTvStatus2.setText("GDrive Client - Connected - File Selected");

            mBtResetLastSyncTimestamp.setEnabled(true);
            readMetadata();

            if (dbSync != null) {
                dbSync.dispose();
            }
            onPostSelectFile();
        }
    }

    private void signIn() {
        Log.i(TAG, "Start sign in");
        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Build a Google SignIn client.
     */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .requestScopes(new Scope(DriveScopes.DRIVE_METADATA))
                        .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))
                        .requestScopes(new Scope(DriveScopes.DRIVE_METADATA_READONLY))
                        .requestScopes(new Scope(DriveScopes.DRIVE_PHOTOS_READONLY))
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbSync != null) {
            dbSync.dispose();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode);

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    handleSignInResult(data);

                    successSingIn();
                }
                break;

            case REQUEST_CODE_NEW_FILE:
            case REQUEST_CODE_SELECT_FILE:

                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    Log.d(TAG, "uri: " + uri.toString());

                    // The query, since it only applies to a single document, will only return
                    // one row. There's no need to filter, sort, or select fields, since we want
                    // all fields for one document.
                    Cursor cursor = getContentResolver()
                            .query(uri, null, null, null, null, null);

                    if (cursor != null && cursor.moveToFirst()) {

                        // search the file on drive


                        final String displayName = cursor.getString(
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        Log.i(TAG, "Display Name: " + displayName);


                        Tasks.call(mExecutor, () -> googleDriveService.files()
                                .list()
                                .setQ("name = '" + displayName +"'"  )
                                .setPageSize(10)
                                .execute())

                                .addOnSuccessListener(fileList -> {


                                    if (fileList.getFiles().size() != 1) {
                                        Toast.makeText(BaseMainDbActivity.this, "Unable to select file size != 1 (" + fileList.size() +")", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    for (File file : fileList.getFiles()) {

                                        Log.i(TAG, "Found file: " + file);


                                        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                                        editor.putString(DRIVE_ID_FILE, file.getId());
                                        driveId = file.getId();
                                        editor.commit();
                                        mTvStatus2.setText("GDrive Client - Connected - File Selected");
                                        mBtSync.setEnabled(true);
                                        mBtResetLastSyncTimestamp.setEnabled(true);
                                        readMetadata();

                                        if (dbSync != null) {
                                            dbSync.dispose();
                                        }

                                        onPostSelectFile();

                                    }
                                }).addOnFailureListener(e -> Toast.makeText(BaseMainDbActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());

                    }

                    break;
                }
        }
    }

    @OnClick(R.id.btSelectFileForSync)
    public void actionSelectFileForSync() {
        try {
            Intent pickerIntent = SAFUtils.createSelectFilePickerIntent();

            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_SELECT_FILE);
        } catch (Exception e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }


    @OnClick(R.id.btCreateFileForSync)
    public void actionCreateFileForSync() {

      Tasks.call(mExecutor, () -> {
            File file = new File()
                    .setMimeType("text/json")
                    .setName(this.getClass().getSimpleName() + ".json")
                    .setParents(Collections.singletonList("root"));

            return googleDriveService.files().create(file).execute().getId();
        }) .addOnSuccessListener(id -> {
            Log.d(TAG, "actionCreateFileForSync: " + id);
          SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
          editor.putString(DRIVE_ID_FILE, id);
          driveId = id;
          editor.commit();
          mTvStatus2.setText("GDrive Client - Connected - File Selected");
          mBtSync.setEnabled(true);
          mBtResetLastSyncTimestamp.setEnabled(true);
          readMetadata();

          if (dbSync != null) {
              dbSync.dispose();
          }

          onPostSelectFile();

        }).addOnFailureListener(e -> Log.e(TAG, "actionCreateFileForSync: " + e.getMessage()));
    }

    protected void updateLastSyncTimeStamp() {
        if (dbSync != null) {
            long lastSyncStatus = dbSync.getLastSyncTimestamp();

            Date date = new Date(lastSyncStatus);

            mTvLastSyncTimestamp.setText("Timestamp: " + lastSyncStatus + " - " + Utility.formatDateTimeNoTimeZone(date));
        }
    }

    @OnClick(R.id.btResetLastSyncTimestamp)
    public void resetLastSyncTimestamp() {

        if (dbSync != null) {
            dbSync.resetLastSyncTimestamp();
            updateLastSyncTimeStamp();
        }
    }

    @OnClick(R.id.btToDbManager)
    public void goToDBManager() {
        startActivity(new Intent(this, DbInspectorActivity.class));
    }

    @OnClick(R.id.btSync)
    public void btSync() {
        mTvStatus.setText("In Progress...");
        new SyncTask().execute();
    }

    public abstract void onPostSync();

    public abstract void onPostSelectFile();


    @SuppressLint("SetTextI18n")
    private void readMetadata() {

        Tasks.call(mExecutor, () ->
                googleDriveService.files().get(driveId).execute())
                .addOnSuccessListener(file -> mTvFileName.setText("File name: " + file.getName()))
                .addOnFailureListener(e -> Log.e(TAG, e.getMessage()));

    }

    class UpdateCurrentTime implements Runnable {

        @Override
        public void run() {
            mTvCurrentTime.setText("CurrentTime: " + Utility.formatDateTimeNoTimeZone(new Date()));

            mMainHandler.postDelayed(this, 500);
        }
    }

    class SyncTask extends AsyncTask<Void, Void, SyncResult> {

        @Override
        protected SyncResult doInBackground(Void... params) {
            return dbSync.sync();
        }

        @Override
        protected void onPostExecute(SyncResult result) {

            if (result.getStatus().isSuccess()) {
                StringBuilder sb = new StringBuilder();
                RecordSyncResult syncResult = result.getResult();

                sb.append("OK - Insert: ");
                sb.append(syncResult.getRecordInserted());
                sb.append(" - Update: ");
                sb.append(syncResult.getRecordUpdated());

                for (Map.Entry<String, RecordChanged> entry : syncResult.getTableSynced().entrySet()) {
                    sb.append("\nTable: ");
                    sb.append(entry.getKey());

                    sb.append(" I:");
                    sb.append(Arrays.toString(entry.getValue().getInseredId().toArray()));
                    sb.append(" U:");
                    sb.append(Arrays.toString(entry.getValue().getUpdatedId().toArray()));
                }
                mTvStatus.setText(sb.toString());
                updateLastSyncTimeStamp();
            } else {
                mTvStatus.setText("Fail: " + result.getStatus().getStatusCode() + "\n" + result.getStatus().getStatusMessage());
                mTvLastSyncTimestamp.setText("");
            }

            onPostSync();
        }
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_METADATA, DriveScopes.DRIVE_READONLY,
                                            DriveScopes.DRIVE_METADATA_READONLY, DriveScopes.DRIVE_PHOTOS_READONLY));
                    credential.setSelectedAccount(googleAccount.getAccount());

                    googleDriveService =
                            new com.google.api.services.drive.Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Drive API DBSync")
                                    .build();

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    //mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void handleSignInONRecnnect() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if  (account != null ){
            Log.d(TAG, "Signed in as " + account.getEmail());

            // Use the authenticated account to sign in to the Drive service.
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            googleDriveService =
                    new com.google.api.services.drive.Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Drive API DBSync")
                            .build();
        }
    }

    private void successSingIn() {
        mTvStatus2.setText("GDrive Client - Connected");
        mBtSelectFileForSync.setEnabled(true);
        mBtCreateFileForSync.setEnabled(true);

        if (driveId != null) {
            mBtSync.setEnabled(true);
            mBtResetLastSyncTimestamp.setEnabled(true);
            readMetadata();

            onPostSelectFile();
        }
    }
}
