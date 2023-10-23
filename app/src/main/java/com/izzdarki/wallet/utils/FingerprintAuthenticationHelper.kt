package com.izzdarki.wallet.utils

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import izzdarki.wallet.R

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
                reasonForNoFingerPrint =
                    activity.getString(R.string.no_biometric_hardware_available)
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware is unavailable
                reasonForNoFingerPrint =
                    activity.getString(R.string.biometric_hardware_is_unavailable)

            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No biometric data is enrolled
                reasonForNoFingerPrint =
                    activity.getString(R.string.no_finger_print_enrolled_first_setup_device_finger_print)

            }
        }

    }

    fun doAuthentication(onAuthResult: () -> Unit) {
        if (isFingerPrintAuthAvailable.not()) {
            Toast.makeText(
                activity.applicationContext,
                reasonForNoFingerPrint,
                Toast.LENGTH_LONG
            ).show()
            return
        }
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
                    Toast.makeText(
                        activity.applicationContext,
                        errString.toString(),
                        Toast.LENGTH_LONG
                    ).show()


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
