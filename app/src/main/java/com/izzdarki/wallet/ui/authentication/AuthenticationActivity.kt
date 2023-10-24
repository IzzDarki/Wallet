package com.izzdarki.wallet.ui.authentication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.izzdarki.wallet.logic.isAppPasswordEnabled
import com.izzdarki.wallet.logic.isPasswordCorrect

import com.izzdarki.wallet.logic.isAuthenticationEnabled
import com.izzdarki.wallet.logic.isFingerprintEnabled
import com.izzdarki.wallet.logic.updateAuthenticationTime
import com.izzdarki.wallet.storage.authenticationStorage
import com.izzdarki.wallet.utils.FingerprintAuthenticationHelper
import izzdarki.wallet.R
import izzdarki.wallet.databinding.ActivityAuthenticationBinding


class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DETAILED_AUTHENTICATION_MESSAGE = "authentication_message"
    }

    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var fingerprintAuthenticationHelper: FingerprintAuthenticationHelper
    private var fingerprintDone: Boolean = false
    private var appPasswordDone: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fingerprintAuthenticationHelper = FingerprintAuthenticationHelper(this)

        // directly finish if authentication is not enabled, activity shouldn't have been started in that case
        if (!isAuthenticationEnabled(this)) {
            finish()
            return
        }

        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // State
        appPasswordDone = !isAppPasswordEnabled(this)
        fingerprintDone = !isFingerprintEnabled(this)

        // Prevent error icon from overlapping with password visibility toggle
        binding.authenticationPasswordLayout.errorIconDrawable = null

        // Authentication message
        binding.authenticationText.text = getString(
                if (isAppPasswordEnabled(this) && isFingerprintEnabled(this))
                    R.string.authenticate_using_fingerprint_and_app_password
                else if (isFingerprintEnabled(this))
                    R.string.authenticate_using_fingerprint
                else
                    R.string.enter_your_app_password
        )

        // Detailed authentication message
        binding.authenticationDetailedText.text = intent.getStringExtra(EXTRA_DETAILED_AUTHENTICATION_MESSAGE) ?: ""

        // Hide password stuff if it is not enabled
        if (!isAppPasswordEnabled(this)) {
            binding.authenticationPasswordLayout.visibility = View.INVISIBLE
            binding.forgotPasswordText.visibility = View.INVISIBLE
        }

        // Authenticate button
        binding.authenticationButton.setOnClickListener {
            if (!fingerprintDone && isFingerprintEnabled(this)) { // Fingerprint authentication
                promptFingerprint()
            }
            if (isAppPasswordEnabled(this)) { // App password authentication
                val enteredPassword = binding.authenticationPasswordInput.text.toString()
                val storedEncodedPassword = authenticationStorage.readEncodedAppPassword(this)!!
                if (isPasswordCorrect(storedEncodedPassword, enteredPassword)) {
                    binding.authenticationPasswordLayout.error = null
                    appPasswordDone = true
                    finishIfAuthenticated()
                } else {
                    binding.authenticationPasswordLayout.error = getString(R.string.app_password_is_incorrect)
                }
            }
        }

        // Editing listener
        binding.authenticationPasswordInput.doOnTextChanged { _, _, _, _ ->
            appPasswordDone = false
            binding.authenticationPasswordLayout.error = null
        }

        // Directly show fingerprint prompt if fingerprint is enabled
        if (isFingerprintEnabled(this))
            promptFingerprint()
    }

    override fun onPause() {
        super.onPause()
        // When activity goes to background, reset authentication state
        appPasswordDone = !isAppPasswordEnabled(this)
        fingerprintDone = !isFingerprintEnabled(this)
    }

    override fun onResume() {
        super.onResume()
        if (isAppPasswordEnabled(this) && !isFingerprintEnabled(this))
            binding.authenticationPasswordInput.requestFocus() // focus password input on activity resume
    }

    private fun promptFingerprint() {
        fingerprintAuthenticationHelper.doAuthentication(
            activity = this,
            promptSubtitle = getString(R.string.fingerprint_needed_to_open_the_app),
        ) {
            fingerprintDone = true
            finishIfAuthenticated()
        }
    }

    private fun finishIfAuthenticated() {
        if (fingerprintDone && appPasswordDone) {
            updateAuthenticationTime(this)
            finish()
        } else if (fingerprintDone) { // fingerprint done but app password still missing
            binding.authenticationPasswordLayout.requestFocus() // app password is next, focus it
        }
    }
}
