package com.izzdarki.wallet.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.io.File;

/**
 * <h><b>Service to delete all files in the specified directory</b></h>
 *
 * <p>EXTRA_DIRECTORY_NAME name of the directory to be cleared (if not set the service does nothing)</p>
 */
public class ClearDirectoryService extends JobIntentService {
    public static final String EXTRA_DIRECTORY_NAME = "com.izzdarki.wallet.clear_directory_service.extra_directory_name";
    private static final int JOB_ID = 1042;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, ClearDirectoryService.class, JOB_ID, intent);
    }

    public static void enqueueWork(Context context, String directoryName) {
        Intent intent = new Intent(context, ClearDirectoryService.class);
        intent.putExtra(ClearDirectoryService.EXTRA_DIRECTORY_NAME, directoryName);
        enqueueWork(context, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String directoryName = intent.getStringExtra(EXTRA_DIRECTORY_NAME);

        if (directoryName == null) return;

        File directory = new File(directoryName);
        File[] files = directory.listFiles();

        if (files == null) return;
        /*
        if (BuildConfig.DEBUG && files.length == 0)
            Log.d("ClearCachedCardImgS", directory.getName() + " is empty");
         */

        // Log.d("asd", "Clearing " + directory.getAbsolutePath() + ", which is " + (files.length > 0 ? "not " : "") + "empty");

        for (File file : files) {
            file.delete();
        }
    }

}
