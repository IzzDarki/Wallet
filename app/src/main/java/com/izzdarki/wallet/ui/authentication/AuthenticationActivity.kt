package com.izzdarki.wallet.ui.authentication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.izzdarki.wallet.logic.isPasswordCorrect

import com.izzdarki.wallet.logic.isAuthenticationEnabled
import com.izzdarki.wallet.logic.updateAuthenticationTime
import com.izzdarki.wallet.preferences.authenticationStorage
import com.izzdarki.wallet.utils.FingerprintAuthenticationHelper
import izzdarki.wallet.R
import izzdarki.wallet.databinding.ActivityAuthenticationBinding


class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DETAILED_AUTHENTICATION_MESSAGE = "authentication_message"
    }

    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var fingerprintAuthenticationHelper: FingerprintAuthenticationHelper


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

        // Prevent error icon from overlapping with password visibility toggle
        binding.authenticationPasswordLayout.errorIconDrawable = null

        // Detailed authentication message
        binding.authenticationDetailedText.text = intent.getStringExtra(EXTRA_DETAILED_AUTHENTICATION_MESSAGE) ?: ""

        // Authenticate button
        binding.authenticationButton.setOnClickListener {
            // Show FIngerPrint if Enabled
            if (authenticationStorage.isFingerPrintEnable(this)) {
                fingerprintAuthenticationHelper.doAuthentication {
                    updateAuthenticationTime(this)
                    finish()
                }
            } else {
                val enteredPassword = binding.authenticationPasswordInput.text.toString()
                val storedEncodedPassword = authenticationStorage.readEncodedAppPassword(this)!!
                if (isPasswordCorrect(storedEncodedPassword, enteredPassword)) {
                    updateAuthenticationTime(this)
                    binding.authenticationPasswordLayout.error = null
                    finish()
                } else {
                    binding.authenticationPasswordLayout.error =
                        getString(R.string.app_password_is_incorrect)
                }
            }
        }
        // FingerPrint Prompt
        if (authenticationStorage.isFingerPrintEnable(this)) {
            binding.authenticationText.setText(getString(R.string.authentication_using_finger_print))
            binding.authenticationPasswordLayout.visibility = View.INVISIBLE
            binding.forgotPasswordText.visibility = View.INVISIBLE
            fingerprintAuthenticationHelper.doAuthentication {
                updateAuthenticationTime(this)
                finish()
            }
        }
        // Remove error on editing
        binding.authenticationPasswordInput.doOnTextChanged { _, _, _, _ ->
            binding.authenticationPasswordLayout.error = null
        }

    }
}
