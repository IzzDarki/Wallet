package com.izzdarki.wallet.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import izzdarki.wallet.R

class DefaultValuesSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.default_values_preferences, rootKey)
    }
}
