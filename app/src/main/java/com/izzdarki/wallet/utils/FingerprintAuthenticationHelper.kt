package com.izzdarki.wallet.utils

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import izzdarki.wallet.R

class FingerprintAuthenticationHelper(activity: FragmentActivity) {

    companion object {
        // Don't allow device credentials
        private const val ALLOWED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

    private var isFingerprintAuthAvailable = false
    private var reasonForNoFingerprint = ""

    init {
        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric authentication is available
                isFingerprintAuthAvailable = true
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // No biometric hardware available
                reasonForNoFingerprint =
                    activity.getString(R.string.no_biometric_hardware_available)
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware is unavailable
                reasonForNoFingerprint =
                    activity.getString(R.string.biometric_hardware_is_unavailable)

            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No biometric data is enrolled
                reasonForNoFingerprint =
                    activity.getString(R.string.no_finger_print_enrolled_first_setup_device_finger_print)

            }

            else -> {
                // Biometric authentication is unavailable for unknown reasons
                reasonForNoFingerprint =
                    activity.getString(R.string.biometrics_unavailable_for_unknown_reasons)
            }
        }

    }

    fun doAuthentication(activity: FragmentActivity, promptSubtitle: String, onFailure: () -> Unit = {}, onSuccess: () -> Unit) {
        if (isFingerprintAuthAvailable.not()) {
            Toast.makeText(
                activity.applicationContext,
                reasonForNoFingerprint,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // Biometric authentication succeeded
                    onSuccess.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Biometric authentication failed
                    Toast.makeText(
                        activity.applicationContext,
                        activity.getString(R.string.fingerprint_authentication_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onFailure.invoke()
                }

                override fun onAuthenticationFailed() {
                    // Biometric authentication failed
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.authenticate_using_fingerprint))
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}
