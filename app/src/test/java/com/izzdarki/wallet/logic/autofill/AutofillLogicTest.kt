package com.izzdarki.wallet.logic.autofill

import android.view.View
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class AutofillLogicTest {

    @Test
    fun testFindDataSource() {
        // Match web domain on credential name
        assertEquals(
            listOf(allCredentials[0], allCredentials[5]),
            findDataSourcesForRequest(allCredentials, "accounts.google.com", "")
        )
        assertEquals(
            listOf(allCredentials[1], allCredentials[2], allCredentials[5]),
            findDataSourcesForRequest(allCredentials, "work.facebook.com", " ")
        )
        assertEquals(
            listOf(allCredentials[3], allCredentials[5]),
            findDataSourcesForRequest(allCredentials, "twitter.com", "")
        )

        // Match web domain using field
        assertEquals(
            listOf(allCredentials[3], allCredentials[5]),
            findDataSourcesForRequest(allCredentials, "login.on.x.com", "")
        )

        // Match package name
        assertEquals(
            listOf(allCredentials[3]),
            findDataSourcesForRequest(allCredentials, null, "com.twitter.android")
        )

    }

    @Test
    fun testValueGivenAutofillHints() {
        assertEquals(
            allCredentials[0].fields[0],
            valueGivenAutofillHints(allCredentials[0], listOf(View.AUTOFILL_HINT_USERNAME))
        )
        assertEquals(
            allCredentials[0].fields[0],
            valueGivenAutofillHints(allCredentials[0], listOf(W3C_USERNAME_HINT))
        )
        assertEquals(
            allCredentials[0].fields[1],
            valueGivenAutofillHints(allCredentials[0], listOf(View.AUTOFILL_HINT_PASSWORD))
        )
        assertEquals(
            allCredentials[0].fields[2],
            valueGivenAutofillHints(allCredentials[0], listOf(View.AUTOFILL_HINT_EMAIL_ADDRESS))
        )
        assertEquals(
            allCredentials[6].fields[9],
            valueGivenAutofillHints(allCredentials[6], listOf(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER))
        )
    }

    @Test
    fun testValueGivenHintAndText() {
        assertEquals(
            allCredentials[0].fields[0],
            valueGivenHintAndText(allCredentials[0], hint = "username", text = null)
        )
        assertEquals(
            allCredentials[0].fields[1],
            valueGivenHintAndText(allCredentials[0], hint = "pw", text = null)
        )
        assertEquals(
            allCredentials[0].fields[2],
            valueGivenHintAndText(allCredentials[0], hint = "mail", text = null)
        )
        assertEquals(
            allCredentials[0].fields[2],
            valueGivenHintAndText(allCredentials[0], hint = null, text = "example@mail.com")
        )
        assertEquals(
            allCredentials[0].fields[2],
            valueGivenHintAndText(allCredentials[0], hint = null, text = "email") // text (not hint) describing email should also work
        )
        assertEquals(
            allCredentials[6].fields[9],
            valueGivenHintAndText(allCredentials[6], hint = "Credit-card number", text = "non-sense text")
        )
        assertEquals(
            allCredentials[6].fields[7],
            valueGivenHintAndText(allCredentials[6], hint = "IBAN", text = "non-sense text")
        )
        assertEquals(
            allCredentials[6].fields[8],
            valueGivenHintAndText(allCredentials[6], hint = "bic", text = "non-sense text")
        )
        assertEquals(
            allCredentials[6].fields[11],
            valueGivenHintAndText(allCredentials[6], hint = "card code verification", text = "non-sense text") // should get the "cvv" field
        )
        assertEquals(
            allCredentials[6].fields[2],
            valueGivenHintAndText(allCredentials[6], hint = "telephone number", text = "non-sense text") // should get the "phone" field
        )
    }

    @Test fun testValueForEmail() {
        // Match by value (but name also describes the email)
        assertEquals(
            allCredentials[0].fields[2],
            emailHeuristic.findMatchingField(allCredentials[0])
        )
        assertEquals(
            allCredentials[1].fields[2],
            emailHeuristic.findMatchingField(allCredentials[1])
        )
        assertEquals(
            allCredentials[2].fields[2],
            emailHeuristic.findMatchingField(allCredentials[2])
        )

        // Match by value (name does not describe the email)
        assertEquals(
            allCredentials[3].fields[2],
            emailHeuristic.findMatchingField(allCredentials[3])
        )

        // Match by name (because value is not an email) => weird use case
        assertEquals(
            allCredentials[4].fields[0],
            emailHeuristic.findMatchingField(allCredentials[4])
        )
    }

    @Test
    fun testValueForPassword() {
        assertEquals(
            allCredentials[0].fields[1],
            passwordHeuristic.findMatchingField(allCredentials[0])
        )
        assertEquals(
            allCredentials[1].fields[1],
            passwordHeuristic.findMatchingField(allCredentials[1])
        )
        assertEquals(
            allCredentials[2].fields[1],
            passwordHeuristic.findMatchingField(allCredentials[2])
        )

        assertEquals(
            allCredentials[3].fields[1],
            passwordHeuristic.findMatchingField(allCredentials[3])
        )
    }

    @Test
    fun testValueForCreditCardNumber() {
        assertEquals(
            allCredentials[6].fields[9],
            creditCardNumberHeuristic.findMatchingField(allCredentials[6])
        )
    }

    companion object {
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
            Credential(
                id = 6,
                name = "always",
                color = 5,
                creationDate = Date(50),
                alterationDate = Date(51),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "mail", value = "weird@gmail.de", secret = false),
                    CredentialField(name = "url", value = "*", secret = false), // Makes this credential be used for any webDomain
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
            Credential(
                id = 7,
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
            ),
        )
    }

}