package com.claudiodegio.dbsync;

import android.content.Intent;

@Deprecated
public class SAFUtils {

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    static public Intent createAddFilePickerIntent(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        intent.setType("text/plain");

        return intent;
    }


    static public Intent createSelectFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }
}
