package com.izzdarki.wallet.ui.settings

import android.os.Bundle
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import izzdarki.wallet.R

class AppInfoSettingsFragment : PreferenceFragmentCompat() {

    private val navController get() = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_info_preferences, rootKey)

        // Set text of version preference
        val versionPreference = findPreference<Preference>(getString(R.string.preferences_version_key))
        versionPreference?.summary = getString(R.string.version_name) + "\n" + getString(R.string.preferences_version_update_info)

        // Update log
        val updateLogPreference = findPreference<Preference>(getString(R.string.preferences_update_log_key))
        updateLogPreference?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_nav_app_info_to_update_log)
            true
        }
    }
}
