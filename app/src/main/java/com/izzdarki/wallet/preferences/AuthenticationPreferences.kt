package com.izzdarki.wallet.preferences

import android.content.Context
import android.content.SharedPreferences
import com.izzdarki.wallet.utils.Utility

object AuthenticationPreferences : AuthenticationStorageInterface {
    override fun writeLastAuthenticationTime(context: Context, lastAuthenticationTime: Long): Boolean {
        return getPreferences(context).edit().putLong(LAST_AUTHENTICATION_TIME_KEY, lastAuthenticationTime).commit()
    }

    override fun readLastAuthenticationTime(context: Context): Long {
        return getPreferences(context).getLong(LAST_AUTHENTICATION_TIME_KEY, 0)
    }

    override fun writeEncodedAppPassword(context: Context, encodedPassword: String): Boolean {
        return getPreferences(context).edit().putString(ENCODED_AUTHENTICATION_PASSWORD, encodedPassword).commit()
    }

    override fun removeEncodedAppPasswort(context: Context): Boolean {
        return getPreferences(context).edit().remove(ENCODED_AUTHENTICATION_PASSWORD).commit()
    }

    override fun readEncodedAppPassword(context: Context): String? {
        return getPreferences(context).getString(ENCODED_AUTHENTICATION_PASSWORD, null)
    }

    override fun enableFingerprint(context: Context, enable:Boolean): Boolean {
        return  getPreferences(context).edit().putBoolean(USE_FINGERPRINT_AUTH, enable).commit()
    }



    override fun isFingerPrintEnable(context: Context): Boolean {
        return  getPreferences(context).getBoolean(USE_FINGERPRINT_AUTH, false)
    }

    @Synchronized private fun getPreferences(context: Context): SharedPreferences {
        if (preferences == null)
            // preferences = context.getSharedPreferences("authentication-test", MODE_PRIVATE) // not encrypted for testing
            preferences = Utility.openEncryptedPreferences(
                context,
                AUTHENTICATION_PREFERENCES_NAME
            )
        return preferences!!
    }

    private var preferences: SharedPreferences? = null
    private const val AUTHENTICATION_PREFERENCES_NAME = "authentication_preferences"
    private const val LAST_AUTHENTICATION_TIME_KEY = "last_authentication_time"
    private const val ENCODED_AUTHENTICATION_PASSWORD = "authentication_password_hash"
    private const val USE_FINGERPRINT_AUTH = "authentication_finger_print"
}