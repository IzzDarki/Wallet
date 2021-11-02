package com.bennet.wallet

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.bennet.wallet.activities.SettingsActivity
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.CardPreferenceManager
import com.bennet.wallet.utils.Utility
import java.io.*
import java.security.GeneralSecurityException

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        runUpdateCode()
        setThemeFromPreferences()
    }

    private fun setThemeFromPreferences() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            val valueString = AppPreferenceManager.getAppDarkMode(this)

            val value =
                if (valueString == null)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else
                    SettingsActivity.SettingsFragment.themePreferenceEntryValueToInt(this, valueString)

            AppCompatDelegate.setDefaultNightMode(value)
        }
    }

    /**
     * Will execute the code only once (first time Application gets created after installation or data removal)
     */
    private fun initFirstRun() {
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
    private fun runUpdateCode() {
        val APPLICATION_LAST_VERSION_NUMBER = "com.bennet.wallet.application_last_version_number"

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val lastVersionNumber = sharedPreferences.getInt(APPLICATION_LAST_VERSION_NUMBER, -1)

        // -1 means either new install or last version is prior to 5 (1.4.0-alpha)

        // encrypt old image files and preferences
        if (lastVersionNumber < 5) { // < 1.4.0-alpha (or fresh install)
            encryptAllOldImageFiles() // this code is also run on every fresh install, but in that case there are no images to encrypt anyway
            encryptOldPreferences()
        }
        if (lastVersionNumber < 6) { // 1.4.1-alpha
            changeCardIdsToProperty()
        }

        sharedPreferences.edit().putInt(APPLICATION_LAST_VERSION_NUMBER, BuildConfig.VERSION_CODE).apply()
    }


    // Migration to 5 (1.4.0-alpha) - Encryption
    /**
     * If old unencrypted preferences are there, this copies all old preferences to new encrypted ones and deletes old preferences afterwards.
     */
    private fun encryptOldPreferences() {
        val oldPreferences =
            getSharedPreferences(CardPreferenceManager.CARDS_PREFERENCES_NAME_OLD, MODE_PRIVATE)
        val preferences = CardPreferenceManager.getPreferences(this)
        val prefContent = oldPreferences.all
        if (prefContent.isNotEmpty()) {
            val editor = preferences.edit()
            for ((key, value) in prefContent) {
                when (value?.javaClass) {
                    // These are all the types that existed before version 5
                    String::class.java -> editor.putString(key, value as String)
                    Int::class.java -> editor.putInt(key, value as Int)
                    Boolean::class.java -> editor.putBoolean(key, value as Boolean)

                    // else -> if (BuildConfig.DEBUG) Log.e("encryptOldPref", "Could not encrypt $key (${value!!::class.qualifiedName}): $value")
                }
            }
            editor.apply()
            oldPreferences.edit().clear().apply()
        }

        // deleting the preferences file doesn't really work that well
    }

    /**
     * Encrypts all images in cards images folder in files, except for example card images
     */
    private fun encryptAllOldImageFiles() {
        val directory =
            File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.name != getString(R.string.example_card_front_image_file_name)
                && file.name != getString(R.string.example_card_back_image_file_name)
                && !Utility.isMahlerFile(this, file)
            ) { // These files don't need to be encrypted
                encryptOldImageFile(file)
            }
        }
    }

    /**
     * Copies old file to new encrypted file and deletes the old one
     * Method should only be called if the `imageFile` is not encrypted and in files directory
     */
    private fun encryptOldImageFile(imageFile: File) {
        // rename old file
        val encryptedImageFilePath = imageFile.absolutePath
        val oldImageFile = File(imageFile.absolutePath + ".old")
        imageFile.renameTo(oldImageFile)
        var outputStream: OutputStream? = null
        try {
            // open encrypted file
            val mainKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedImageFile = EncryptedFile.Builder(
                this,
                File(encryptedImageFilePath),
                mainKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            // open output stream
            outputStream = encryptedImageFile.openFileOutput()

            // copy
            val inputStream: InputStream = FileInputStream(oldImageFile.absolutePath)
            Utility.copyFile(inputStream, outputStream)

            // delete old unencrypted image file
            val success = oldImageFile.delete()
            /*
            if (BuildConfig.DEBUG && !success) {
                Log.e("EncryptOldFile", "Could not delete old image file after replacing it with an encrypted file");
            } */
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            // flush and close the stream
            if (outputStream != null) {
                try {
                    outputStream.flush()
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Since version 6 card id is not an explicit property of each card anymore, but instead a normal property.
     * Removes all card ids and instead creates a property for the card id
     */
    private fun changeCardIdsToProperty() {
        for (cardID in CardPreferenceManager.readAllIDs(this)) {

            // read id value
            @Suppress("DEPRECATION")
            val idValue = CardPreferenceManager.readID(this, cardID)

            if (idValue != null && idValue != "") {

                // create new property
                val cardPropertyIDs = CardPreferenceManager.readPropertyIds(this, cardID)
                val propertyID = Utility.IDGenerator(cardPropertyIDs).generateID()
                cardPropertyIDs.add(propertyID)

                CardPreferenceManager.writePropertyIds(this, cardID, cardPropertyIDs)
                CardPreferenceManager.writePropertyName(this, cardID, propertyID, getString(R.string.card_id))
                CardPreferenceManager.writePropertyValue(this, cardID, propertyID, idValue)
                CardPreferenceManager.writePropertySecret(this, cardID, propertyID, false)

                // remove old id
                @Suppress("DEPRECATION")
                CardPreferenceManager.removeID(this, cardID)
            }
        }
    }

}