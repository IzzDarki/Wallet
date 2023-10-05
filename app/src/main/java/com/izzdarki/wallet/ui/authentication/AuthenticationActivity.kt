package com.izzdarki.wallet.ui.authentication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.izzdarki.wallet.logic.isPasswordCorrect

import com.izzdarki.wallet.logic.isAuthenticationEnabled
import com.izzdarki.wallet.logic.updateAuthenticationTime
import com.izzdarki.wallet.preferences.authenticationStorage
import izzdarki.wallet.R
import izzdarki.wallet.databinding.ActivityAuthenticationBinding


class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AUTHENTICATION_MESSAGE = "authentication_message"
    }

    private lateinit var binding: ActivityAuthenticationBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // directly finish if authentication is not enabled, activity shouldn't have been started in that case
        if (!isAuthenticationEnabled(this)) {
            finish()
            return
        }

        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent error icon from overlapping with password visibility toggle
        binding.authenticationPasswordLayout.errorIconDrawable = null

        // Authentication message
        binding.authenticationText.text = intent.getStringExtra(EXTRA_AUTHENTICATION_MESSAGE) ?: getString(R.string.authenticate_to_use_app_text)

        // Authenticate button
        binding.authenticationButton.setOnClickListener {
            val enteredPassword = binding.authenticationPasswordInput.text.toString()
            val storedEncodedPassword = authenticationStorage.readEncodedAppPassword(this)!!
            if (isPasswordCorrect(storedEncodedPassword, enteredPassword)) {
                updateAuthenticationTime(this)
                binding.authenticationPasswordLayout.error = null
                finish()
            } else {
                binding.authenticationPasswordLayout.error = getString(R.string.app_password_is_incorrect)
            }
        }

        // Remove error on editing
        binding.authenticationPasswordInput.doOnTextChanged { _, _, _, _ ->
            binding.authenticationPasswordLayout.error = null
        }

    }
}
