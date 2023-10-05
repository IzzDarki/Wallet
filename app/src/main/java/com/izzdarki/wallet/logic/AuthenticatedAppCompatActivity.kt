package com.izzdarki.wallet.logic

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.izzdarki.wallet.preferences.authenticationStorage
import com.izzdarki.wallet.ui.authentication.AuthenticationActivity
import java.text.SimpleDateFormat
import java.util.Date

open class AuthenticatedAppCompatActivity : AppCompatActivity() {

    protected var authenticationMessage: String? = null
    private var wasAuthenticatedOnActivityResume = false

    override fun onResume() {
        if (!isAuthenticated(this)) {
            wasAuthenticatedOnActivityResume = false
            startActivity(
                Intent(this, AuthenticationActivity::class.java).apply {
                    if (authenticationMessage != null)
                        putExtra(AuthenticationActivity.EXTRA_AUTHENTICATION_MESSAGE, authenticationMessage)
                }
            )
        }
        else
            wasAuthenticatedOnActivityResume = true
        super.onResume()
    }

    override fun onPause() {
        // update authentication time, because the user is considered authenticated as long as the app is in foreground
        if (wasAuthenticatedOnActivityResume) // Only do it if the user was authenticated on activity resume (activity can be started without authentication, see onResume)
            updateAuthenticationTime(this)

        if (wasAuthenticatedOnActivityResume) {
            val authenticationTime = authenticationStorage.readLastAuthenticationTime(this)
            val authenticationTimeFormatted = SimpleDateFormat("HH:mm:ss").format(Date(authenticationTime))
            Log.d("asdf", "Activity ${this.javaClass.simpleName} updated authentication time to $authenticationTimeFormatted")
        }
        super.onPause()
    }

}