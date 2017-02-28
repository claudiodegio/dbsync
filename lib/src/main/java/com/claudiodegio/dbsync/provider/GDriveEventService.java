package com.claudiodegio.dbsync.provider;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;


/**
 * Gdrive service to handle file complention
 */
public class GDriveEventService extends DriveEventService {

    final private String TAG = "GDriveEventService";
    final static public String BUNDLE_SUCCESS = "success";
    final static public String BUNDLE_DRIVEID = "driveId";

    public static final String CUSTOM_INTENT = "com.claudiodefio.dbsync.intent.action.COMPLETE";

    @Override
    public void onCompletion(CompletionEvent event) {
        Log.d(TAG, "Action completed with status: " + event.getStatus());

        Intent intent = new Intent();
        intent.setAction(CUSTOM_INTENT)
                .putExtra(BUNDLE_SUCCESS, event.getStatus())
                .putExtra(BUNDLE_DRIVEID, event.getDriveId());
        sendBroadcast(intent);

        event.dismiss();
    }
}
