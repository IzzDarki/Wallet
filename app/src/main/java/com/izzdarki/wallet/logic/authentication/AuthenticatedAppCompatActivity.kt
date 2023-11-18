package com.izzdarki.wallet.logic.authentication

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.izzdarki.wallet.ui.authentication.AuthenticationActivity

open class AuthenticatedAppCompatActivity : AppCompatActivity() {

    // Possible issue
    // It would be great if the user could interact with AuthenticationActivity while
    // the onCreate of the actual activity happens in the background
    // Would allow for ex. reading from storage to take place in the background
    // But even if AuthenticationActivity is started in the onCreate method in the beginning,
    // the rest of it will be executed before the AuthenticationActivity is actually created

    protected var authenticationMessage: String? = null
    private var wasAuthenticatedOnActivityResume = false
    private var screenOffReceiver: ScreenOffBroadcastReceiver? = null
    private val authenticationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result?.resultCode != RESULT_OK) {
            // If authentication failed (ex. user cancelled it), finish this activity
            // Otherwise the user would be instantly redirected to AuthenticationActivity again
            finish()
        }
    }

    companion object {
        private var screenOffReceiverRegistered = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register screen off receiver
        if (!screenOffReceiverRegistered) { // Prevents multiple instances of this activity base class from registering the receiver each on their own
            screenOffReceiverRegistered = true
            screenOffReceiver = ScreenOffBroadcastReceiver()
            registerReceiver(
                screenOffReceiver,
                IntentFilter(ScreenOffBroadcastReceiver.INTENT_FILTER_ACTION)
            )
        }
    }

    override fun onResume() {
        if (!isAuthenticated(this)) { // also checks if authentication is enabled
            wasAuthenticatedOnActivityResume = false
            val authenticationIntent = Intent(this, AuthenticationActivity::class.java).apply {
                if (authenticationMessage != null)
                    putExtra(AuthenticationActivity.EXTRA_DETAILED_AUTHENTICATION_MESSAGE, authenticationMessage)
            }
            authenticationLauncher.launch(authenticationIntent)
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

    override fun onDestroy() {
        // Unregister screen off receiver
        // When the app is closed this will unregister the receiver, therefore the user will stay authenticated even when the screen is turned off
        // But there is no good way to keep the receiver active independently of the app's lifecycle
        // Un-authenticating the user on activity destroy is also not a good option,
        // because it's unclear when what activity is destroyed => possibly too many authentication requests
        if (screenOffReceiver != null) { // This only ScreenOffReceiver is registered to this activity's context
            screenOffReceiverRegistered = false
            unregisterReceiver(screenOffReceiver)
            screenOffReceiver = null
        }
        super.onDestroy()
    }
}
