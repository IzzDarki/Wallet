package com.izzdarki.wallet.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import izzdarki.wallet.R
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.Navigation
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.izzdarki.wallet.logic.authentication.disableAppPassword
import com.izzdarki.wallet.logic.authentication.isFingerprintEnabled
import com.izzdarki.wallet.logic.authentication.isAppPasswordEnabled
import com.izzdarki.wallet.logic.authentication.setFingerprintEnabled
import com.izzdarki.wallet.ui.authentication.AuthenticationActivity
import com.izzdarki.wallet.utils.FingerprintAuthenticationHelper

class SettingsFragment : PreferenceFragmentCompat() {

    private val navController get() = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

    private lateinit var appPasswordPreference: SwitchPreferenceCompat
    private lateinit var fingerprintPreference: SwitchPreferenceCompat

    private lateinit var fingerprintAuthenticationHelper: FingerprintAuthenticationHelper
    private val authenticationLauncherToDisableFingerprint = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            // Authentication succeeded
            changeFingerprintSetting(false)
        }
    }

    private val authenticationLauncherToDisableAppPassword = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            // Authentication succeeded
            changeAppPasswordSettingToDisabled()
        }
    }

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
        appPasswordPreference = findPreference(getString(R.string.preferences_enable_app_password_key))!!
        appPasswordPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true)
                navController.navigate(R.id.action_nav_settings_to_authentication_setup)
            else {
                if (!isFingerprintEnabled(requireContext())) {
                    showDisableAuthenticationCompletelyDialog {
                        authenticateToDisableAppPasswordSetting()
                    }
                }
                else {
                    authenticateToDisableAppPasswordSetting()
                }
            }
            true // Update switch state (will be reset if canceled)
        }
        appPasswordPreference.isChecked = isAppPasswordEnabled(requireContext()) // Causes no switch animation when set here

        // Fingerprint authentication
        fingerprintPreference = findPreference(getString(R.string.preferences_enable_fingerprint_authentication_key))!!
        fingerprintPreference.isChecked = isFingerprintEnabled(requireContext()) // Causes no switch animation when set here
        fingerprintPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue !is Boolean) return@setOnPreferenceChangeListener false // cannot happen, but needed for type safety

            if (newValue == false && !isAppPasswordEnabled(requireContext())) {
                showDisableAuthenticationCompletelyDialog {
                    authenticateToDisableFingerprintSetting()
                }
            }
            else if (!newValue)
                authenticateToDisableFingerprintSetting()
            else
                askForFingerprintToEnableFingerprint()

            true // Update switch state (will be reset if canceled)
        }
    }

    override fun onResume() {
        super.onResume()
        // App password authentication
        appPasswordPreference.isChecked = isAppPasswordEnabled(requireContext()) // Causes switch animation

        // Fingerprint authentication
        fingerprintPreference.isChecked = isFingerprintEnabled(requireContext()) // Causes switch animation

        updateAuthenticationWarningVisibility()
    }

    private fun showDisableAuthenticationCompletelyDialog(onDisableCompletely: () -> Unit) {
        // Warn user if he is about to completely disable authentication
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.disable_authentication_completely)
            .setMessage(R.string.disable_authentication_completely_warning)
            .setPositiveButton(R.string.disable_authentication_completely) { dialog, _ ->
                onDisableCompletely()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                // Reset switches
                appPasswordPreference.isChecked = isAppPasswordEnabled(requireContext()) // Causes switch animation
                fingerprintPreference.isChecked = isFingerprintEnabled(requireContext()) // Causes switch animation
            }
            .show()
    }

    private fun askForFingerprintToEnableFingerprint() {
        // Require fingerprint to change this setting
        fingerprintAuthenticationHelper.doAuthentication(
            requireActivity(),
            promptSubtitle = getString(R.string.fingerprint_needed_to_change_the_setting),
            onSuccess = { changeFingerprintSetting(true) },
            onFailure = { fingerprintPreference.isChecked = isFingerprintEnabled(requireContext()) }
        )
    }

    private fun authenticateToDisableFingerprintSetting() {
        val authenticationIntent = Intent(requireContext(), AuthenticationActivity::class.java).apply {
            putExtra(
                AuthenticationActivity.EXTRA_DETAILED_AUTHENTICATION_MESSAGE,
                getString(R.string.authenticate_to_disable_fingerprint_authentication)
            )
        }
        authenticationLauncherToDisableFingerprint.launch(authenticationIntent)
    }

    private fun authenticateToDisableAppPasswordSetting() {
        val authenticationIntent = Intent(requireContext(), AuthenticationActivity::class.java).apply {
            putExtra(
                AuthenticationActivity.EXTRA_DETAILED_AUTHENTICATION_MESSAGE,
                getString(R.string.authenticate_to_disable_app_password_authentication)
            )
        }
        authenticationLauncherToDisableAppPassword.launch(authenticationIntent)
    }

    private fun changeFingerprintSetting(newValue: Boolean) {
        fingerprintPreference.isChecked = newValue // update switch
        setFingerprintEnabled(requireContext(), newValue)
        updateAuthenticationWarningVisibility()
    }

    private fun changeAppPasswordSettingToDisabled() {
        appPasswordPreference.isChecked = false // update switch
        disableAppPassword(requireContext())
        updateAuthenticationWarningVisibility()
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
