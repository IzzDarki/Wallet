package com.izzdarki.wallet

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.izzdarki.wallet.ui.settings.SettingsFragment
import com.izzdarki.wallet.storage.AppPreferenceManager
import com.izzdarki.wallet.logic.updates.updateToCredentialPreferences
import izzdarki.wallet.BuildConfig

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        runUpdateCode()
        setThemeFromPreferences()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    private fun setThemeFromPreferences() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            val valueString = AppPreferenceManager.getAppDarkMode(this)

            val value =
                if (valueString == null)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else
                    SettingsFragment.themePreferenceEntryValueToInt(this, valueString)

            AppCompatDelegate.setDefaultNightMode(value)
        }
    }

    /*
    /**
     * Will execute the code only once (first time Application gets created after installation or data removal)
     */
    private fun initFirstRun() {
        final String APPLICATION_FIRST_RUN_DONE = "com.izzdarki.wallet.application_first_run_done";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean(APPLICATION_FIRST_RUN_DONE, false)) {

            // at the moment empty

            // set first run preference to false
            sharedPreferences.edit().putBoolean(APPLICATION_FIRST_RUN_DONE, true).apply();
        }
    }
     */

    /**
     * Reads old version number from preferences and eventually runs code if the app has been updated
     */
    private fun runUpdateCode() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val lastVersionNumber = sharedPreferences.getInt(APPLICATION_LAST_VERSION_NUMBER, -1) // -1 means new installation

        if (lastVersionNumber != -1) {
            // Update to 2.2.0-alpha.0 (version code 10)
            if (lastVersionNumber < 10)
                updateToCredentialPreferences(this)
            // Well-tested update, however issues can always arise => Keeping old data
            // In a future update the old preferences should be deleted (removeOldPreferences function does that (manually tested))
        }

        // Write new version code for future updates
        sharedPreferences.edit().putInt(APPLICATION_LAST_VERSION_NUMBER, BuildConfig.VERSION_CODE).apply()
    }

    companion object {
        const val APPLICATION_LAST_VERSION_NUMBER = "com.izzdarki.wallet.application_last_version_number"
    }

}