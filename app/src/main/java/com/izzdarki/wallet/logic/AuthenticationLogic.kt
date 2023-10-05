package com.izzdarki.wallet.logic

import android.content.Context
import android.util.Log
import com.izzdarki.wallet.preferences.authenticationStorage
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

fun isAuthenticationEnabled(context: Context): Boolean {
    return authenticationStorage.readEncodedAppPassword(context) != null
}

fun hashPassword(password: String): String =
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
