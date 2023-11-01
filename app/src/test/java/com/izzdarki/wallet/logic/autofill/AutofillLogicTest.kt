package com.izzdarki.wallet.logic.autofill

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
            listOf(allCredentials[0]),
            findDataSourcesForRequest(allCredentials, "accounts.google.com", "")
        )
        assertEquals(
            listOf(allCredentials[1], allCredentials[2]),
            findDataSourcesForRequest(allCredentials, "work.facebook.com", " ")
        )
        assertEquals(
            listOf(allCredentials[3]),
            findDataSourcesForRequest(allCredentials, "twitter.com", "")
        )

        // Match web domain using field
        assertEquals(
            listOf(allCredentials[3]),
            findDataSourcesForRequest(allCredentials, "login.on.x.com", "")
        )

        // Match package name
        assertEquals(
            listOf(allCredentials[3]),
            findDataSourcesForRequest(allCredentials, null, "com.twitter.android")
        )

    }

    @Test fun testValueForEmail() {
        // Match by value (but name also describes the email)
        assertEquals(
            allCredentials[0].fields[2],
            valueForEmail(allCredentials[0])
        )
        assertEquals(
            allCredentials[1].fields[2],
            valueForEmail(allCredentials[1])
        )
        assertEquals(
            allCredentials[2].fields[2],
            valueForEmail(allCredentials[2])
        )

        // Match by value (name does not describe the email)
        assertEquals(
            allCredentials[3].fields[2],
            valueForEmail(allCredentials[3])
        )

        // Match by name (because value is not an email) => weird use case
        assertEquals(
            allCredentials[4].fields[0],
            valueForEmail(allCredentials[4])
        )
    }

    @Test
    fun testValueForPassword() {
        assertEquals(
            allCredentials[0].fields[1],
            valueForPassword(allCredentials[0])
        )
        assertEquals(
            allCredentials[1].fields[1],
            valueForPassword(allCredentials[1])
        )
        assertEquals(
            allCredentials[2].fields[1],
            valueForPassword(allCredentials[2])
        )

        assertEquals(
            allCredentials[3].fields[1],
            valueForPassword(allCredentials[3])
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
        )
    }

}