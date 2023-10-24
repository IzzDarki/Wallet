package com.izzdarki.wallet.logic

import android.content.Context
import android.util.Log
import com.izzdarki.wallet.storage.authenticationStorage
import org.signal.argon2.Argon2
import org.signal.argon2.MemoryCost
import org.signal.argon2.Type
import org.signal.argon2.Version
import java.security.SecureRandom


// Implementation explanation
// last authentication time (in millis since unix epoch) is stored (encrypted)
// last authentication time is set to 0 on screen off
// last authentication time is updated to current time in onPause of user activities (See [AuthenticatedAppCompatActivity])

/**
 * Check authentication status and update last authentication time if user is authenticated (and authentication is enabled).
 * The user has to enter an authentication password.
 * The authenticated status will hold until
 * - the user turns off the screen or
 * - the user leaves the app for more than [AUTHENTICATION_TIMEOUT_MILLIS] milliseconds
 * @return `true` if the user is authenticated or authentication is disabled
 */
fun isAuthenticated(context: Context): Boolean {
    if (!isAuthenticationEnabled(context))
        return true

    // authentication is enabled

    if (!isAuthenticationExpired(context)) {
        authenticationStorage.writeLastAuthenticationTime(context, System.currentTimeMillis()) // update authentication time
        return true
    }

    return false
}

/**
 * Update the last authentication time (only if authentication is enabled)
 * The update will happen even if the last authentication is expired.
 * Only call this method from a place where the user is authenticated.
 * (For example in onPause of user activities, meaning that the user is considered authenticated as long as the app is in foreground)
 */
fun updateAuthenticationTime(context: Context) {
    if (isAuthenticationEnabled(context)) {
        authenticationStorage.writeLastAuthenticationTime(context, System.currentTimeMillis())
    }
}

/**
 * Remove last authentication time (only if authentication is enabled).
 * Can be used to force the user to authenticate again.
 */
fun removeLastAuthenticationTime(context: Context) {
    if (isAuthenticationEnabled(context))
        authenticationStorage.writeLastAuthenticationTime(context, 0)
}

fun isAuthenticationEnabled(context: Context): Boolean {
    return isAppPasswordEnabled(context) || isFingerprintEnabled(context)
}

fun disableAuthentication(context: Context): Boolean {
    removeLastAuthenticationTime(context) // sets to 0
    return authenticationStorage.removeEncodedAppPassword(context)
}

/**
 * Update the app password.
 * @param password New password in plain text
 * @return `true` if the password was updated successfully
 */
fun setNewAppPassword(context: Context, password: String): Boolean {
    return authenticationStorage.writeEncodedAppPassword(context, encodePassword(password))
}

fun isAppPasswordEnabled(context: Context): Boolean {
    return authenticationStorage.readEncodedAppPassword(context) != null
}

fun setFingerprintEnabled(context: Context, enabled: Boolean): Boolean {
    return authenticationStorage.writeFingerprintEnabled(context, enabled)
}

fun isFingerprintEnabled(context: Context): Boolean {
    return authenticationStorage.readFingerprintEnabled(context)
}


/**
 * Get the encoded password (uses a random salt).
 * It contains everything needed to verify a password.
 * Uses argon2id with 46 MiB memory cost, 2 iterations and 1 parallelism.
 * Recommended on Wikipedia (https://en.wikipedia.org/wiki/Argon2) is 46 MiB memory, 1 iteration, 1 parallelism
 * or other configurations with less memory but more iterations.
 * Changing this only affects passwords set after the change.
 */
fun encodePassword(password: String): String =
    Argon2.Builder(Version.LATEST)
        .type(Type.Argon2id)
        .memoryCost(MemoryCost.MiB(46))
        .parallelism(1)
        .iterations(2)
        .build()
        .hash(password.toByteArray(Charsets.UTF_8), getRandomSalt())
        .encoded

fun isPasswordCorrect(encodedPassword: String, password: String): Boolean =
    Argon2.verify(encodedPassword, password.toByteArray(Charsets.UTF_8))


private fun isAuthenticationExpired(context: Context): Boolean {
    Log.d("asdf", "isAuthenticationExpired: ${(System.currentTimeMillis() - authenticationStorage.readLastAuthenticationTime(context)).toFloat() / 1000f}")
    return System.currentTimeMillis() - authenticationStorage.readLastAuthenticationTime(context) >= AUTHENTICATION_TIMEOUT_MILLIS
}

private fun getRandomSalt(): ByteArray {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return salt
}

private const val AUTHENTICATION_TIMEOUT_MILLIS = 5000 // TODO use this value 1000 * 60 * 5 // 5 minutes
