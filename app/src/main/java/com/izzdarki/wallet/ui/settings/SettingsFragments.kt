package com.izzdarki.wallet.ui.settings

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import izzdarki.wallet.R
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.Navigation
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.izzdarki.wallet.logic.isFingerprintEnabled
import com.izzdarki.wallet.logic.isAppPasswordEnabled
import com.izzdarki.wallet.logic.setFingerprintEnabled
import com.izzdarki.wallet.utils.FingerprintAuthenticationHelper

class SettingsFragment : PreferenceFragmentCompat() {

    private val navController get() = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

    private lateinit var fingerprintAuthenticationHelper: FingerprintAuthenticationHelper

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        fingerprintAuthenticationHelper = FingerprintAuthenticationHelper(requireActivity())

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

        // Authentication warning
        updateAuthenticationWarningVisibility()

        // App password authentication
        val authenticationPreference: SwitchPreferenceCompat = findPreference(getString(R.string.preferences_enable_app_password_key))!!
        authenticationPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true)
                navController.navigate(R.id.action_nav_settings_to_authentication_setup)
            else
                navController.navigate(R.id.action_nav_settings_to_authentication_disable)
            true
        }
        authenticationPreference.isChecked = isAppPasswordEnabled(requireContext()) // Causes no switch animation when set here

        // Fingerprint authentication
        val fingerprintAuthenticationPreference: SwitchPreferenceCompat = findPreference(getString(R.string.preferences_enable_fingerprint_authentication_key))!!
        fingerprintAuthenticationPreference.isChecked = isFingerprintEnabled(requireContext()) // Causes no switch animation when set here
        fingerprintAuthenticationPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue !is Boolean) return@setOnPreferenceChangeListener false // cannot happen, but needed for type safety

            if (newValue == false && !isAppPasswordEnabled(requireContext())) {
                // Warn user if he is about to completely disable authentication
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.disable_authentication_completely)
                    .setMessage(R.string.disable_authentication_completely_warning)
                    .setPositiveButton(R.string.disable_authentication_completely) { dialog, _ ->
                        askForFingerprintAndChangeSetting(fingerprintAuthenticationPreference, newValue)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            else {
                askForFingerprintAndChangeSetting(fingerprintAuthenticationPreference, newValue)
            }

            false // Switch is only updated when authentication succeeds
        }
    }

    override fun onResume() {
        super.onResume()
        // App password authentication
        val authenticationPreference: SwitchPreferenceCompat = findPreference(getString(R.string.preferences_enable_app_password_key))!!
        authenticationPreference.isChecked = isAppPasswordEnabled(requireContext()) // Causes switch animation

        // Fingerprint authentication
        val fingerprintAuthenticationPreference: SwitchPreferenceCompat = findPreference(getString(R.string.preferences_enable_fingerprint_authentication_key))!!
        fingerprintAuthenticationPreference.isChecked = isFingerprintEnabled(requireContext()) // Causes switch animation

        updateAuthenticationWarningVisibility()
    }

    private fun askForFingerprintAndChangeSetting(fingerprintPreference: SwitchPreferenceCompat, newValue: Boolean) {
        // Require fingerprint to change this setting
        fingerprintAuthenticationHelper.doAuthentication(
            requireActivity(),
            promptSubtitle = getString(R.string.fingerprint_needed_to_change_the_setting),
        ) {
            fingerprintPreference.isChecked = newValue // update switch
            setFingerprintEnabled(requireContext(), newValue)
            updateAuthenticationWarningVisibility()
        }
    }

    private fun updateAuthenticationWarningVisibility() {
        val authenticationWarningPreference: Preference? = findPreference(getString(R.string.preferences_authentication_warning_key))
        authenticationWarningPreference?.isVisible =
            !isFingerprintEnabled(requireContext()) && !isAppPasswordEnabled(requireContext())
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
