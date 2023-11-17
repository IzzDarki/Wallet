package com.izzdarki.wallet.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import izzdarki.wallet.R

class CreditsSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.credits_preferences, rootKey)
    }
}
