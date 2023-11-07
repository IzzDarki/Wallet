package com.izzdarki.wallet.services

import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class AutofillServiceFunctionsTest {

    @Test
    fun testFilling() {
        // TODO Also find out if the logical groups are overly restrictive in what they allow

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
            Credential(
                id = 1001,
                name = "All in One",
                color = 7,
                creationDate = Date(50),
                alterationDate = Date(51),
                labels = mutableSetOf(),
                fields = mutableListOf(
                    CredentialField(name = "url", value = "*", secret = false),
                    CredentialField(name = "random", value = "abc", secret = false),
                    CredentialField(name = "aharandom", value = "123", secret = false),
                    CredentialField(name = "phone", value = "0116384729", secret = false),
                    CredentialField(name = "address", value = "some STreet ψσλλο abc, No. 32, PO321", secret = false),
                    CredentialField(name = "mail", value = "weird@pca.de", secret = false),
                    CredentialField(name = "username", value = "my_twitter_username", secret = false),
                    CredentialField(name = "password", value = "my_twitter_password", secret = true),
                    CredentialField(name = "__", value = "GB33BUKB20201555555555", secret = true),
                    CredentialField(name = "__", value = "BOFSGBS1ZF2", secret = true),
                    CredentialField(name = "card pin", value = "10894", secret = true), // Should not be group PAYMENT
                    CredentialField(name = "__", value = "30569309025904", secret = true),
                    CredentialField(name = "cvv", value = "132", secret = true),
                ),
                barcode = null,
                imagePaths = mutableListOf(),
            ),
        )
    }

}