package com.izzdarki.wallet.preferences

import android.content.Context

val authenticationStorage: AuthenticationStorageInterface = AuthenticationPreferences

interface AuthenticationStorageInterface {

    /**
     * Writes the last authentication time to the storage (in milliseconds since unix epoch)
     * @return `true` if successful
     */
    fun writeLastAuthenticationTime(context: Context, lastAuthenticationTime: Long): Boolean

    /**
     * Reads the last authentication time from the storage (in milliseconds since unix epoch)
     * @return the last authentication time or 0 if no authentication has been performed yet or the last authentication has already expired due to screen turned off
     */
    fun readLastAuthenticationTime(context: Context): Long

    /**
     * Writes the authentication passwordHash to the storage
     * @return `true` if successful
     */
    fun writeEncodedAppPassword(context: Context, passwordHash: String): Boolean

    /**
     * Reads the authentication passwordHash from the storage
     * @return the authentication passwordHash or `null` if no password has been set yet
     */
    fun readEncodedAppPassword(context: Context): String?


}