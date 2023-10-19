package com.izzdarki.wallet.logic

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.izzdarki.wallet.storage.authenticationStorage
import com.izzdarki.wallet.ui.authentication.AuthenticationActivity
import java.text.SimpleDateFormat
import java.util.Date

open class AuthenticatedAppCompatActivity : AppCompatActivity() {

    // Possible issue
    // It would be great if the user could interact with AuthenticationActivity while
    // the onCreate of the actual activity happens in the background
    // Would allow for ex. reading from storage to take place in the background
    // But even if AuthenticationActivity is started in the onCreate method in the beginning,
    // the rest of it will be executed before the AuthenticationActivity is actually created

    protected var authenticationMessage: String? = null
    private var wasAuthenticatedOnActivityResume = false

    override fun onResume() {
        if (!isAuthenticated(this)) {
            wasAuthenticatedOnActivityResume = false
            startActivity(
                Intent(this, AuthenticationActivity::class.java).apply {
                    if (authenticationMessage != null)
                        putExtra(AuthenticationActivity.EXTRA_DETAILED_AUTHENTICATION_MESSAGE, authenticationMessage)
                }
            )
        }
        else
            wasAuthenticatedOnActivityResume = true
        super.onResume()
    }

    override fun onPause() {
        // Update authentication time, because the user is considered authenticated as long as the app is in foreground
        // But only if the user was authenticated on activity resume (activity can be started without authentication, see onResume)
        if (wasAuthenticatedOnActivityResume && isAuthenticationEnabled(this))
            updateAuthenticationTime(this)

        // Logging
//        if (wasAuthenticatedOnActivityResume && isAuthenticationEnabled(this)) {
//            val authenticationTime = authenticationStorage.readLastAuthenticationTime(this)
//            val authenticationTimeFormatted = SimpleDateFormat("HH:mm:ss").format(Date(authenticationTime))
//            Log.d("asdf", "Activity ${this.javaClass.simpleName} updated authentication time to $authenticationTimeFormatted")
//        }
        super.onPause()
    }
}
