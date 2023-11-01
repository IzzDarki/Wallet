package com.izzdarki.wallet.ui.authentication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.services.AutofillViewData
import com.izzdarki.wallet.services.WalletAutofillService.Companion.createFillResponse
import com.izzdarki.wallet.storage.CredentialPreferenceStorage

@RequiresApi(Build.VERSION_CODES.O)
class AutofillAuthenticationActivity : AuthenticationActivity() {

    companion object {
        const val EXTRA_DATA_SOURCE_IDS = "extra_data_source_ids" // Array<Long>
        const val EXTRA_AUTOFILL_VIEW_DATA = "extra_autofill_view_data" // Array<AutofillViewData> (in the form of Parcelable array list)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("autofill", "AutofillAuthenticationActivity created")
        super.onCreate(savedInstanceState)
    }

    override fun onSuccessfulAuthentication() {
        // Get dataSources and list of AutofillViewData from intent
        val dataSources = intent.getLongArrayExtra(EXTRA_DATA_SOURCE_IDS)?.toList()
            ?.mapNotNull { id -> CredentialPreferenceStorage.readCredential(this, id) }
            ?: return // If extra not passed, AuthenticationActivity should have been used instead of this one

        val viewsData: List<AutofillViewData> = when {
            Build.VERSION.SDK_INT >= 33 -> intent.getParcelableArrayListExtra(EXTRA_AUTOFILL_VIEW_DATA, AutofillViewData::class.java)
            else -> @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(EXTRA_AUTOFILL_VIEW_DATA)
        } ?: return // If extra not passed, AuthenticationActivity should have been used instead of this one

        // Create fill response
        val fillResponse = createFillResponse(dataSources, viewsData)

        // Set result
        val replyIntent = Intent().apply {
            putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse)
        }
        setResult(RESULT_OK, replyIntent)
    }

}