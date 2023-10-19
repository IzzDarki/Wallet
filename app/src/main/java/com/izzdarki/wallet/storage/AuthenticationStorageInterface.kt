package com.izzdarki.wallet.storage

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
     * Writes the encoded authentication password to the storage.
     * It contains everything needed to verify a password (e.g. salt)
     * @return `true` if successful
     */
    fun writeEncodedAppPassword(context: Context, encodedPassword: String): Boolean

    /**
     * Remove the encoded password from the storage.
     * Calling [readEncodedAppPassword] will then return `null`
     */
    fun removeEncodedAppPasswort(context: Context): Boolean

    /**
     * Reads the authentication passwordHash from the storage
     * @return the authentication passwordHash or `null` if no password has been set yet
     */
    fun readEncodedAppPassword(context: Context): String?


}