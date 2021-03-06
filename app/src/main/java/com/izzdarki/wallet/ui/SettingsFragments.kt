package com.izzdarki.wallet.ui

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle
import izzdarki.wallet.R
import androidx.appcompat.app.AppCompatDelegate
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference

class SettingsFragment : PreferenceFragmentCompat() {

    private val navController get() = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Sub preferences
        val defaultValuesPref: Preference? = findPreference(getString(R.string.preferences_default_values_key))
        defaultValuesPref?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_nav_settings_to_default_values)
            true
        }

        val creditsPref: Preference? = findPreference(getString(R.string.preferences_credits_key))
        creditsPref?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_nav_settings_to_credits)
            true
        }

        val appInfoPref: Preference? = findPreference(getString(R.string.preferences_app_info_key))
        appInfoPref?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_nav_settings_to_app_info)
            true
        }

        // Theme: if value not set, set the value to what the night mode actually is (AppCompatDelegate)
        val themePreference: ListPreference = findPreference(getString(R.string.preferences_theme_key))!!
        if (themePreference.value == "") {
            val value = themeIntToPreferenceEntryValue(
                requireContext(),
                AppCompatDelegate.getDefaultNightMode()
            )
            if (value != null)
                themePreference.value = value
        }
        themePreference.setOnPreferenceChangeListener { _, newValue ->
            AppCompatDelegate.setDefaultNightMode(
                themePreferenceEntryValueToInt(requireContext(), newValue)
            )
            true
        }

        val appFunctionsPreference: MultiSelectListPreference? = findPreference(getString(R.string.preferences_app_functions_key))
        appFunctionsPreference?.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as Set<*>).size == 0) {
                Toast.makeText(
                    requireContext(),
                    R.string.preferences_app_functions_error_too_few_items,
                    Toast.LENGTH_LONG
                ).show()
                false
            }
            else {
                Toast.makeText(requireContext(),
                    R.string.changing_app_functions_requires_restart,
                    Toast.LENGTH_LONG
                ).show()
                true
            }
        }
    }

    companion object {
        fun themePreferenceEntryValueToInt(context: Context, preferenceValue: Any): Int {
            return when (preferenceValue as String) {
                context.getString(R.string.preferences_theme_entry_values_dark)
                -> AppCompatDelegate.MODE_NIGHT_YES
                context.getString(R.string.preferences_theme_entry_values_light)
                -> AppCompatDelegate.MODE_NIGHT_NO
                else // context.getString(R.string.preferences_theme_entry_values_follow_system)
                -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }

        fun themeIntToPreferenceEntryValue(context: Context, intValue: Int): String? {
            return when (intValue) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> context.getString(R.string.preferences_theme_entry_values_follow_system)
                AppCompatDelegate.MODE_NIGHT_YES -> context.getString(R.string.preferences_theme_entry_values_dark)
                AppCompatDelegate.MODE_NIGHT_NO -> context.getString(R.string.preferences_theme_entry_values_light)
                else -> null
            }
        }
    }
}

class DefaultValuesSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.default_values_preferences, rootKey)
    }
}

class CreditsSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.credits_preferences, rootKey)
    }
}

class AppInfoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_info_preferences, rootKey)
    }
}
