package com.izzdarki.wallet.services

import android.content.Context
import android.service.autofill.FillResponse
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.logic.autofill.W3C_CURRENT_PASSWORD_HINT
import com.izzdarki.wallet.logic.autofill.W3C_USERNAME_HINT
import com.izzdarki.wallet.services.WalletAutofillService.Companion.addAuthenticationToFillResponse
import org.junit.Test
import org.junit.Assert.*
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
            allCredentials,
            autofillViewsData,
            fillableAutofillIds = arrayOf(
                autofillViewsData[0].autofillId,
                autofillViewsData[1].autofillId,
                autofillViewsData[3].autofillId,
            ),
        )
    }

    @Test
    fun testFillLoginData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val viewsData = listOf(
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(W3C_USERNAME_HINT),
                hint = "username",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // Useless view that is focused (something like parent view of focused text field)
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = null,
                text = "",
                isFocused = true,
            ),
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(W3C_CURRENT_PASSWORD_HINT),
                hint = "Password",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Email-address",
                text = "",
                isFocused = true,
            ),
            AutofillViewData( // OTHER
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Address",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // OTHER
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "phone",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Credit card number",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "cvv",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "~cryptic name~", // Matched IBAN because that's it's field name
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "BIC",
                text = "",
                isFocused = false,
            ),
        )

        val extractedValues = WalletAutofillService.extractValuesToFillViews(credentialsWithManyFields, viewsData)
        assertEquals(
            setOf(
                credentialsWithManyFields.fields[4], // mail
                credentialsWithManyFields.fields[5], // username
                credentialsWithManyFields.fields[6], // password
            ),
            extractedValues.map { it.second }.toSet()
        )
    }

    @Test
    fun testFillPaymentData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewsData = listOf(
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(W3C_USERNAME_HINT),
                hint = "username",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // Useless view that is focused (something like parent view of focused text field)
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = null,
                text = "",
                isFocused = true,
            ),
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(W3C_CURRENT_PASSWORD_HINT),
                hint = "Password",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Email-address",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // OTHER
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Address",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // OTHER
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "phone",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Credit card number",
                text = "",
                isFocused = true,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "cvv",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "~cryptic name~", // Matched IBAN because that's it's field name
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "BIC",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // LOGIN (pin belongs to login, card pins should not be filled to payment requests)
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "card pin",
                text = "",
                isFocused = false,
            ),
        )

        val extractedValues = WalletAutofillService.extractValuesToFillViews(credentialsWithManyFields, viewsData)
        assertEquals(
            setOf(
                credentialsWithManyFields.fields[7], // IBAN
                credentialsWithManyFields.fields[8], // BIC
                credentialsWithManyFields.fields[9], // Credit card number
                credentialsWithManyFields.fields[11] // CVV
            ),
            extractedValues.map { it.second }.toSet()
        )
    }

    @Test
    fun testFillOtherData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewsData = listOf(
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(W3C_USERNAME_HINT),
                hint = "username",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // Useless view that is focused (something like parent view of focused text field)
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = null,
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(W3C_CURRENT_PASSWORD_HINT),
                hint = "Password",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // LOGIN
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Email-address",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // OTHER
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Address",
                text = "",
                isFocused = true,
            ),
            AutofillViewData( // OTHER
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "phone",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "Credit card number",
                text = "",
                isFocused = true,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "cvv",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "~cryptic name~", // Matched IBAN because that's it's field name
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // PAYMENT
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "BIC",
                text = "",
                isFocused = false,
            ),
            AutofillViewData( // LOGIN (pin belongs to login, card pins should not be filled to payment requests)
                autofillId = View(context).autofillId,
                autofillHints = listOf(),
                hint = "card pin",
                text = "",
                isFocused = false,
            ),
        )

        val extractedValues = WalletAutofillService.extractValuesToFillViews(credentialsWithManyFields, viewsData)
        assertEquals(
            setOf(
                credentialsWithManyFields.fields[2], // phone
                credentialsWithManyFields.fields[3], // address
            ),
            extractedValues.map { it.second }.toSet()
        )
    }

    companion object {
        val credentialsWithManyFields = Credential(
            id = 1001,
            name = "All in One",
            color = 7,
            creationDate = Date(50),
            alterationDate = Date(51),
            labels = mutableSetOf(),
            fields = mutableListOf(
                CredentialField(name = "random", value = "abc", secret = false), // 0
                CredentialField(name = "aharandom", value = "123", secret = false), // 1
                CredentialField(name = "phone", value = "0116384729", secret = false), // 2
                CredentialField(name = "address", value = "some STreet ψσλλο abc, No. 32, PO321", secret = false), // 3
                CredentialField(name = "mail", value = "weird@pca.de", secret = false), // 4
                CredentialField(name = "username", value = "my_twitter_username", secret = false), // 5
                CredentialField(name = "password", value = "my_twitter_password", secret = true), // 6
                CredentialField(name = "~cryptic name~", value = "GB33BUKB20201555555555", secret = true), // IBAN // 7
                CredentialField(name = "__", value = "BOFSGBS1ZF2", secret = true), // BIC // 8
                CredentialField(name = "__", value = "30569309025904", secret = true), // Credit card number // 9
                CredentialField(name = "card pin", value = "10894", secret = true), // Should not be group PAYMENT // 10
                CredentialField(name = "cvv", value = "132", secret = true), // CVV // 11
            ),
            barcode = null,
            imagePaths = mutableListOf(),
        )

        val allCredentials = listOf(
            Credential(
                id = 1,
                name = "Google",
                color = 1,
                creationDate = Date(10),
                alterationDate = Date(11),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "username", value = "my_google_username", secret = false),
                    CredentialField(name = "password", value = "my_google_password", secret = true),
                    CredentialField(name = "email", value = "my_google_email@gmail.com", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 2,
                name = "Facebook for work",
                color = 2,
                creationDate = Date(20),
                alterationDate = Date(21),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "username", value = "my_facebook_work__username", secret = false),
                    CredentialField(name = "password", value = "my_facebook_work_password", secret = true),
                    CredentialField(name = "email", value = "my_facebook_email@facebook.cn", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 3,
                name = "Facebook home",
                color = 3,
                creationDate = Date(30),
                alterationDate = Date(31),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "username", value = "my_facebook_home_username", secret = false),
                    CredentialField(name = "password", value = "my_facebook_home_password", secret = true),
                    CredentialField(name = "email", value = "my_facebook_home_email@abc.g.x", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 100,
                name = "Visa stuff",
                color = 4,
                creationDate = Date(40),
                alterationDate = Date(41),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "__", value = "30569309025904", secret = true),
                    CredentialField(name = "cvv", value = "132", secret = true),
                    CredentialField(name = "url", value = "*", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 100,
                name = "Bank stuff",
                color = 4,
                creationDate = Date(40),
                alterationDate = Date(41),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "__", value = "GB33BUKB20201555555555", secret = true),
                    CredentialField(name = "__", value = "BOFSGBS1ZF2", secret = true),
                    CredentialField(name = "pin", value = "10894", secret = true), // Should not be group PAYMENT
                    CredentialField(name = "url", value = "*", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 4,
                name = "Twitter",
                color = 4,
                creationDate = Date(40),
                alterationDate = Date(41),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "username", value = "my_twitter_username", secret = false),
                    CredentialField(name = "password", value = "my_twitter_password", secret = true),
                    CredentialField(name = "field0", value = "twitter@x.de", secret = false),
                    CredentialField(name = "field1", value = "com.twitter.android", secret = false),
                    CredentialField(name = "field2", value = "https://x.com/home", secret = false),
                    CredentialField(name = "keyword", value = "whatever", secret = true),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 5,
                name = "WeirdEmail",
                color = 5,
                creationDate = Date(50),
                alterationDate = Date(51),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "mail", value = "weird@pca.de", secret = false),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            credentialsWithManyFields
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
