package com.izzdarki.wallet.ui.authentication

import android.os.Build
import com.izzdarki.wallet.services.AutofillViewData

class AutofillAuthenticationActivity : AuthenticationActivity() {

    companion object {
        const val EXTRA_AUTOFILL_VIEW_DATA = "extra_autofill_view_data"
    }

    override fun onSuccessfulAuthentication() {
        // Get list of AutofillViewData from intent
        val viewsDataArray: Array<AutofillViewData>? = when {
            Build.VERSION.SDK_INT >= 33 -> intent.getSerializableExtra(EXTRA_AUTOFILL_VIEW_DATA, Array<AutofillViewData>::class.java)
            else -> @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_AUTOFILL_VIEW_DATA) as Array<AutofillViewData>?
        }
        val viewsData = viewsDataArray?.toList() ?: return // If extra not passed, AuthenticationActivity should have been used instead of this one

        // TODO implement
    }

}