package com.claudiodegio.dbsync;

import android.util.Log;

import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;



public class GDriveEventService extends DriveEventService {

    final private String TAG = "GDriveEventService";

    @Override
    public void onCompletion(CompletionEvent event) {
        Log.d(TAG, "Action completed with status: " + event.getStatus());

        // handle completion event here.

        event.dismiss();
    }
}
