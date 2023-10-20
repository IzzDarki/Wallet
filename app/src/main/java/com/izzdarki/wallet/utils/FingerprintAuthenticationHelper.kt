package com.izzdarki.wallet.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class FingerprintAuthenticationHelper(val activity: FragmentActivity) {
    private var isFingerPrintAuthAvailable = false
    private var reasonForNoFingerPrint = ""

    init {
        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric authentication is available
                isFingerPrintAuthAvailable = true
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // No biometric hardware available
                reasonForNoFingerPrint = "No biometric hardware available"
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware is unavailable
                reasonForNoFingerPrint = "Biometric hardware is unavailable"

            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No biometric data is enrolled
                reasonForNoFingerPrint = "No biometric data is enrolled"

            }
        }

    }

    fun doAuthentication(onAuthResult: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // Biometric authentication succeeded
                    onAuthResult.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Biometric authentication failed
                }

                override fun onAuthenticationFailed() {
                    // Biometric authentication failed
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate using biometric")
            .setSubtitle("Place your finger on the sensor")
            .setDescription("Touch the sensor to authenticate")
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }


}
