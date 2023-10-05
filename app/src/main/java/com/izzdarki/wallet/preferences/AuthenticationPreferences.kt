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

    override fun writeEncodedAppPassword(context: Context, passwordHash: String): Boolean {
        return getPreferences(context).edit().putString(AUTHENTICATION_PASSWORD_HASH_KEY, passwordHash).commit()
    }

    override fun readEncodedAppPassword(context: Context): String? {
        return getPreferences(context).getString(AUTHENTICATION_PASSWORD_HASH_KEY, null)
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
    private const val AUTHENTICATION_PASSWORD_HASH_KEY = "authentication_password_hash"
}