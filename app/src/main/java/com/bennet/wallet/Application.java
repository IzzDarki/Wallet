package com.bennet.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import com.bennet.wallet.activities.SettingsActivity;
import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.utils.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Map;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        runUpdateCode();

        setThemeFromPreferences();
    }

    protected void setThemeFromPreferences() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            String valueString = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.preferences_theme_key), null);
            int value;
            if (valueString == null)
                value = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            else
                value = SettingsActivity.SettingsFragment.themePreferenceEntryValueToInt(this, valueString);
            AppCompatDelegate.setDefaultNightMode(value);
        }
    }

    /**
     * Will execute the code only once (first time Application gets created after installation or data removal)
     */
    private void initFirstRun() {
        /*
        final String APPLICATION_FIRST_RUN_DONE = "com.bennet.wallet.application_first_run_done";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean(APPLICATION_FIRST_RUN_DONE, false)) {

            // at the moment empty

            // set first run preference to false
            sharedPreferences.edit().putBoolean(APPLICATION_FIRST_RUN_DONE, true).apply();
        }
         */
    }

    /**
     * Reads old version number from preferences and eventually runs code if the app has been updated
     */
    protected void runUpdateCode() {
        final String APPLICATION_LAST_VERSION_NUMBER = "com.bennet.wallet.application_last_version_number";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int lastVersionNumber = sharedPreferences.getInt(APPLICATION_LAST_VERSION_NUMBER, -1);

        // -1 means either new install or last version is prior to 5 (1.4.0-alpha)

        // encrypt old image files and preferences
        if (lastVersionNumber < 5) { // < 1.4.0-alpha (or fresh install)
            encryptAllOldImageFiles(); // this code is also run on every fresh install, but in that case there are no images to encrypt anyway
            encryptOldPreferences();
        }

        sharedPreferences.edit().putInt(APPLICATION_LAST_VERSION_NUMBER, BuildConfig.VERSION_CODE).apply();
    }

    // Migration to 5 (1.4.0-alpha) - Encryption
    /**
     * If old unencrypted preferences are there, this copies all old preferences to new encrypted ones and deletes old preferences afterwards.
     */
    private void encryptOldPreferences() {
        SharedPreferences oldPreferences = getSharedPreferences(CardPreferenceManager.CARDS_PREFERENCES_NAME_OLD, Context.MODE_PRIVATE);
        SharedPreferences preferences = CardPreferenceManager.getPreferences(this);

        Map<String, ?> prefContent = oldPreferences.getAll();
        if (prefContent.size() > 0) {

            SharedPreferences.Editor editor = preferences.edit();

            for (Map.Entry<String, ?> entry : prefContent.entrySet()) {
                if (entry.getValue().getClass() == String.class)
                    editor.putString(entry.getKey(), (String) entry.getValue());

                else if (entry.getValue().getClass() == Integer.class)
                    editor.putInt(entry.getKey(), (Integer) entry.getValue());

                else if (entry.getValue().getClass() == Boolean.class)
                    editor.putBoolean(entry.getKey(), (Boolean) entry.getValue());

                // These are all the types that existed before version 5

                else { // TODO remove or comment debug log
                    if (BuildConfig.DEBUG) {
                        Log.e("encryptOldPref", "Could not encrypt " + entry.getKey() + " (" + entry.getValue().getClass().getName() + ")" + ": " + entry.getValue().toString());
                    }
                }
            }
            editor.apply();

            oldPreferences.edit().clear().apply();
        }

        // deleting the preferences file doesn't really work that well
    }

    /**
     * Encrypts all images in cards images folder in files, except for example card images
     */
    private void encryptAllOldImageFiles() {
        File directory = new File(getFilesDir() + "/" + getString(R.string.cards_images_folder_name));
        File[] files = directory.listFiles();

        if (files == null)
            throw new RuntimeException("Could not list files in: " + directory.getAbsolutePath());

        for (File file : files) {
            if (!file.getName().equals(getString(R.string.example_card_front_image_file_name))
                    && !file.getName().equals(getString(R.string.example_card_back_image_file_name))
                    && !file.getName().contains(getString(R.string.mahler_card_front_image_file_name))) { // These files don't need to be encrypted
                encryptOldImageFile(file);
            }
        }
    }


    /**
     * Copies old file to new encrypted file and deletes the old one
     * Method should only be called if the {@code imageFile} is not encrypted and in files directory
     */
    private void encryptOldImageFile(File imageFile) {

        // rename old file
        String encryptedImageFilePath = imageFile.getAbsolutePath();
        File oldImageFile = new File (imageFile.getAbsolutePath() + ".old");
        imageFile.renameTo(oldImageFile);

        OutputStream outputStream = null;
        try {
            // open encrypted file
            MasterKey mainKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedFile encryptedImageFile = new EncryptedFile.Builder(this,
                    new File(encryptedImageFilePath),
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            // open output stream
            outputStream = encryptedImageFile.openFileOutput();

            // copy
            InputStream inputStream = new FileInputStream(oldImageFile.getAbsolutePath());
            Utility.copyFile(inputStream, outputStream);

            // delete old unencrypted image file
            boolean success = oldImageFile.delete();
            if (BuildConfig.DEBUG && !success) { // TODO Remove debug code
                Log.e("EncryptOldFile", "Could not delete old image file after replacing it with an encrypted file");
            }

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            // flush and close the stream
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
