package com.bennet.wallet;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

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
}
