package com.izzdarki.wallet.logic

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import izzdarki.wallet.R

fun Context.getClipboardManager() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


fun Context.copyToClipboard(text: String, secret: Boolean) {
    val clipData = ClipData.newPlainText(getString(R.string.secret_text), text)

    // Set sensitive flag
    if (secret && Build.VERSION.SDK_INT >= 24) { // extras are only available in API 24+
        val sensitiveFlagName =
            if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE
            else "android.content.extra.IS_SENSITIVE"
        clipData.description.extras = PersistableBundle().apply {
            putBoolean(sensitiveFlagName, true)
        }
    }
    getClipboardManager().setPrimaryClip(clipData)

    // Only show a toast for Android 12 and lower, according to https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
}
fun Context.clearClipboard() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getClipboardManager().clearPrimaryClip()
        } else {
            copyToClipboard("", secret = false)
        }
    } catch (e: Exception) {
        Toast.makeText(this, R.string.unable_to_clean_clipboard, Toast.LENGTH_SHORT).show()
    }
}