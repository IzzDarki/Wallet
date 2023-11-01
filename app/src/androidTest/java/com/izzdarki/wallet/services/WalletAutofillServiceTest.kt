package com.izzdarki.wallet.services

import android.content.Context
import android.service.autofill.FillResponse
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.services.WalletAutofillService.Companion.addAuthenticationToFillResponse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class WalletAutofillServiceTest {

    @Test(expected = Test.None::class) // Assert doesn't throw exception
    fun testCreatingAuthenticationIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val fillResponseBuilder = FillResponse.Builder()

        val autofillViewsData = getAutofillViewsData(context)

        context.addAuthenticationToFillResponse(
            fillResponseBuilder,
            dataSources,
            autofillViewsData,
            fillableAutofillIds = arrayOf(
                autofillViewsData[0].autofillId,
                autofillViewsData[1].autofillId,
                autofillViewsData[3].autofillId,
            ),
        )
    }

    companion object {
        val dataSources = listOf(
            Credential(
                id = 182,
                name = "pw",
                color = 0,
                creationDate = Date(134),
                alterationDate = Date(139),
                labels = mutableSetOf("test", "set"),
                fields = mutableListOf(
                    CredentialField(name = "Password", value = "pw", secret = true),
                    CredentialField(name = "test", value = "test", secret = false),
                    CredentialField(name = "test", value = "test", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 234,
                name = "username",
                color = 4,
                creationDate = Date(4112),
                alterationDate = Date(42564),
                labels = mutableSetOf("test", "set"),
                fields = mutableListOf(
                    CredentialField(name = "user", value = "username", secret = false),
                    CredentialField(name = "test", value = "test", secret = false),
                    CredentialField(name = "test", value = "test", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = -2345,
                name = "pw_username_email",
                color = 4,
                creationDate = Date(4112),
                alterationDate = Date(42564),
                labels = mutableSetOf("test", "set"),
                fields = mutableListOf(
                    CredentialField(name = "password", value = "pw_username_email", secret = true),
                    CredentialField(name = "eml", value = "pw_username_email@email.com", secret = false),
                    CredentialField(name = "username", value = "pw_username_email", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
        )

        fun getAutofillViewsData(context: Context) = listOf(
            AutofillViewData(
                autofillId = View(context).autofillId,
                autofillHints = listOf(View.AUTOFILL_HINT_USERNAME),
                hint = "Username",
                text = "",
            ),
            AutofillViewData(
                autofillId = View(context).autofillId,
                autofillHints = listOf(View.AUTOFILL_HINT_PASSWORD),
                hint = "Password",
                text = "",
            ),
            AutofillViewData(
                autofillId = View(context).autofillId,
                autofillHints = emptyList(),
                hint = null,
                text = "",
            ),
            AutofillViewData(
                autofillId = View(context).autofillId,
                autofillHints = listOf(View.AUTOFILL_HINT_PHONE),
                hint = "phone",
                text = "+1 234 567 890",
            ),
            AutofillViewData(
                autofillId = View(context).autofillId,
                autofillHints = emptyList(),
                hint = "Email",
                text = "example@email.com",
            ),
        )
    }

}
