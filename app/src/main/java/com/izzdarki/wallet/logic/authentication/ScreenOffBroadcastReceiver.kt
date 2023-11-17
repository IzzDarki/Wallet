package com.izzdarki.wallet.logic.authentication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenOffBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val INTENT_FILTER_ACTION = Intent.ACTION_SCREEN_OFF
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.d("asdf", "screen off")
            if (isAuthenticationEnabled(context))
                removeLastAuthenticationTime(context)
        }
    }
}