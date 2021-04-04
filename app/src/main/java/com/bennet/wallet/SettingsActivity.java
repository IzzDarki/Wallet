package com.bennet.wallet;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    static public class SettingsFragment extends PreferenceFragmentCompat {

        protected ListPreference themePreference;
        protected MultiSelectListPreference appFunctionsPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            themePreference = findPreference(getString(R.string.preferences_theme_key));
            appFunctionsPreference = findPreference(getString(R.string.preferences_app_functions_key));

            // if value not set, set the value to what the night mode actually is (AppCompatDelegate)
            if (themePreference.getValue().equals("")) {
                String value = themeIntToPreferenceEntryValue(requireContext(), AppCompatDelegate.getDefaultNightMode());
                if (value != null)
                    themePreference.setValue(value);
            }
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                AppCompatDelegate.setDefaultNightMode(themePreferenceEntryValueToInt(requireContext(), newValue));
                return true;
            });

            appFunctionsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                if (((Set<String>)(newValue)).size() == 0) {
                    Toast.makeText(requireContext(), R.string.preferences_app_functions_error_too_few_items, Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            });
        }

        // helpers
        static public int themePreferenceEntryValueToInt(Context context, Object preferenceValue) {
            String themeValue = (String) preferenceValue;
            if (themeValue.equals(context.getString(R.string.preferences_theme_entry_values_follow_system)))
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            else if (themeValue.equals(context.getString(R.string.preferences_theme_entry_values_dark)))
                 return AppCompatDelegate.MODE_NIGHT_YES;

            else if (themeValue.equals(context.getString(R.string.preferences_theme_entry_values_light)))
                return AppCompatDelegate.MODE_NIGHT_NO;

            else
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

        static public String themeIntToPreferenceEntryValue(Context context, int intValue) {
            switch (intValue) {
                case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                    return context.getString(R.string.preferences_theme_entry_values_follow_system);

                case AppCompatDelegate.MODE_NIGHT_YES:
                    return context.getString(R.string.preferences_theme_entry_values_dark);

                case AppCompatDelegate.MODE_NIGHT_NO:
                    return context.getString(R.string.preferences_theme_entry_values_light);

                default:
                    return null;
            }
        }
    }


    static public class DefaultValuesSettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.default_values_preferences, rootKey);
        }
    }


    static public class CreditsSettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.credits_preferences, rootKey);
        }
    }


    protected MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // hooks
        toolbar = findViewById(R.id.toolbar);

        // toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);

        // see https://developer.android.com/guide/topics/ui/settings
        // settings fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_settings_container, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // see https://developer.android.com/guide/topics/ui/settings/organize-your-settings
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_settings_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
