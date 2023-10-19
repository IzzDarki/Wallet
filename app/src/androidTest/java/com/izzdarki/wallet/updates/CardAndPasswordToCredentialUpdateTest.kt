package com.izzdarki.wallet.updates

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.storage.AppPreferenceManager
import com.izzdarki.wallet.storage.CardStorage
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.storage.PasswordStorage
import com.izzdarki.wallet.utils.Utility
import com.izzdarki.wallet.utils.updateToCredentialPreferences
import izzdarki.wallet.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

@RunWith(AndroidJUnit4::class)
class CardAndPasswordToCredentialUpdateTest {

    @Test
    fun testCardsAndPasswordsAndSortingOrder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Check if CardStorage contains anything
        if (CardStorage.readAllIDs(context).isNotEmpty()) {
            val foundCards = CardStorage.readAll(context)
            fail("CardStorage should be empty in testing environment: found $foundCards")
            return
        }

        // Create some cards
        CardStorage.writeComplete(
            context,
            ID = 1234,
            name = "Card 1",
            color = 0,
            creationDate = Date(12345),
            alterationDate = Date(54321),
            labels = Utility.PreferenceArrayString(listOf("Label 1", "Label 2").iterator()),
            code = "1234567890",
            codeType = CardStorage.CARD_CODE_TYPE_CODABAR,
            codeTypeText = true,
            frontImage = File("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/1234_front.jpg"),
            backImage = null,
            properties = listOf(
                CredentialField(
                    propertyID = 143,
                    name = "Property 1",
                    value = "uiwrw1",
                    secret = false
                )
            )
        )
        CardStorage.writeComplete(
            context,
            ID = 234,
            name = "Card 2",
            color = 1,
            creationDate = Date(54321),
            alterationDate = Date(12345),
            labels = Utility.PreferenceArrayString(listOf("Label 3", "Label 4").iterator()),
            code = "0987654321",
            codeType = CardStorage.CARD_CODE_TYPE_QR,
            codeTypeText = false,
            frontImage = File("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/234_front.jpg"),
            backImage = File("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/234_back.jpg"),
            properties = listOf(
                CredentialField(
                    propertyID = 234,
                    name = "Property 2",
                    value = "asf",
                    secret = true
                ),
                CredentialField(
                    propertyID = 345,
                    name = "Property 3",
                    value = "s",
                    secret = false
                )
            )
        )
        CardStorage.writeComplete(
            context,
            ID = 345,
            name = "Card 3",
            color = 2,
            creationDate = Date(12345),
            alterationDate = Date(54321),
            labels = Utility.PreferenceArrayString(),
            code = "1234567890",
            codeType = CardStorage.CARD_CODE_TYPE_ITF,
            codeTypeText = true,
            frontImage = null,
            backImage = null,
            properties = listOf()
        )
        CardStorage.writeComplete(
            context,
            ID = 456,
            name = "Card 4",
            color = 3,
            creationDate = Date(54321),
            alterationDate = Date(12345),
            labels = Utility.PreferenceArrayString(listOf("Label 5").iterator()),
            code = "",
            codeType = CardStorage.CARD_CODE_TYPE_AZTEC,
            codeTypeText = false,
            frontImage = null,
            backImage = File("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/456_back.jpg"),
            properties = listOf()
        )

        // Create some passwords
        PasswordStorage.writeComplete(
            context,
            ID = 567,
            name = "Password 1",
            passwordValue = "1234567890",
            color = 0,
            creationDate = Date(12345),
            alterationDate = Date(54321),
            labels = Utility.PreferenceArrayString(listOf("Label 6", "Label 7").iterator()),
            properties = listOf(
                CredentialField(
                    propertyID = 678,
                    name = "Property 4",
                    value = "ag2+πψψβσϑÜſ",
                    secret = false
                )
            )
        )

        PasswordStorage.writeComplete(
            context,
            ID = 5678,
            name = "Password 2",
            passwordValue = "1234567890",
            color = 0,
            creationDate = Date(12345),
            alterationDate = Date(54321),
            labels = Utility.PreferenceArrayString(listOf("Label 6", "Label 7").iterator()),
            properties = listOf(
                CredentialField(
                    propertyID = 678,
                    name = "Property 4",
                    value = "ag2+πψψβσϑÜſ",
                    secret = false
                )
            )
        )

        // Set custom sorting orders
        val cardCustomSortingOrder = listOf(456, 1234, 345, 234, 4) // 4 is a non-existing id => should be ignored
        val passwordCustomSortingOrder = listOf(5678, 567)
        CardStorage.writeCustomSortingNoGrouping(context, Utility.PreferenceArrayInt(cardCustomSortingOrder.iterator()))
        PasswordStorage.writeCustomSortingNoGrouping(context, Utility.PreferenceArrayInt(passwordCustomSortingOrder.iterator()))

        // Run the update
        updateToCredentialPreferences(context)

        // Assert that enough cards and passwords were created
        assertEquals(5, CredentialPreferenceStorage.readAllIds(context).size)

        // Find new ids
        val allCredentials = CredentialPreferenceStorage.readAllCredentials(context)
        val card1Id = allCredentials.find { it.name == "Card 1" }!!.id
        val card2Id = allCredentials.find { it.name == "Card 2" }!!.id
        val card3Id = allCredentials.find { it.name == "Card 3" }!!.id
        val card4Id = allCredentials.find { it.name == "Card 4" }!!.id
        val password1Id = allCredentials.find { it.name == "Password 1" }!!.id
        val password2Id = allCredentials.find { it.name == "Password 2" }!!.id

        // Assert that sorting order is now in CredentialStorage
        val expectedSortingOrder = listOf(card4Id, card1Id, card3Id, card2Id, password2Id, password1Id)
        assertEquals(expectedSortingOrder, AppPreferenceManager.getCredentialsCustomSortingOrder(context))

        // Assert that cards are now in CredentialStorage
        val card1 = CredentialPreferenceStorage.readCredential(context, card1Id)
        assertNotNull(card1)
        if (card1 != null) {
            assertEquals("Card 1", card1.name)
            assertEquals(0, card1.color)
            assertEquals(Date(12345), card1.creationDate)
            assertEquals(Date(54321), card1.alterationDate)
            assertEquals(setOf("Label 1", "Label 2", "Card"), card1.labels)
            assertEquals(1, card1.fields.size)
            assertEquals("Property 1", card1.fields[0].name)
            assertEquals("uiwrw1", card1.fields[0].value)
            assertEquals(false, card1.fields[0].secret)
            assertEquals("1234567890", card1.barcode?.code)
            assertEquals(CardStorage.CARD_CODE_TYPE_CODABAR, card1.barcode?.type)
            assertEquals(true, card1.barcode?.showText)
            assertEquals(listOf("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/1234_front.jpg"), card1.imagePaths)
        }

        val card2 = CredentialPreferenceStorage.readCredential(context, card2Id)
        assertNotNull(card2)
        if (card2 != null) {
            assertEquals("Card 2", card2.name)
            assertEquals(1, card2.color)
            assertEquals(Date(54321), card2.creationDate)
            assertEquals(Date(12345), card2.alterationDate)
            assertEquals(setOf("Label 3", "Label 4", "Card"), card2.labels)
            assertEquals(2, card2.fields.size)
            assertEquals("Property 2", card2.fields[0].name)
            assertEquals("asf", card2.fields[0].value)
            assertEquals(true, card2.fields[0].secret)
            assertEquals("Property 3", card2.fields[1].name)
            assertEquals("s", card2.fields[1].value)
            assertEquals(false, card2.fields[1].secret)
            assertEquals("0987654321", card2.barcode?.code)
            assertEquals(CardStorage.CARD_CODE_TYPE_QR, card2.barcode?.type)
            assertEquals(false, card2.barcode?.showText)
            assertEquals(listOf("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/234_front.jpg", "/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/234_back.jpg"), card2.imagePaths)
        }

        val card3 = CredentialPreferenceStorage.readCredential(context, card3Id)
        assertNotNull(card3)
        if (card3 != null) {
            assertEquals("Card 3", card3.name)
            assertEquals(2, card3.color)
            assertEquals(Date(12345), card3.creationDate)
            assertEquals(Date(54321), card3.alterationDate)
            assertEquals(setOf("Card"), card3.labels)
            assertEquals(0, card3.fields.size)
            assertEquals("1234567890", card3.barcode?.code)
            assertEquals(CardStorage.CARD_CODE_TYPE_ITF, card3.barcode?.type)
            assertEquals(true, card3.barcode?.showText)
            assertEquals(listOf<String>(), card3.imagePaths)
        }

        val card4 = CredentialPreferenceStorage.readCredential(context, card4Id)
        assertNotNull(card4)
        if (card4 != null) {
            assertEquals("Card 4", card4.name)
            assertEquals(3, card4.color)
            assertEquals(Date(54321), card4.creationDate)
            assertEquals(Date(12345), card4.alterationDate)
            assertEquals(setOf("Label 5", "Card"), card4.labels)
            assertEquals(0, card4.fields.size)
            assertNull(card4.barcode)
            assertEquals(listOf("/storage/emulated/0/Android/data/com.izzdarki.wallet/files/Cards_Images/456_back.jpg"), card4.imagePaths)
        }

        // Assert that passwords are now in CredentialStorage
        val password1 = CredentialPreferenceStorage.readCredential(context, password1Id)
        assertNotNull(password1)
        if (password1 != null) {
            assertEquals("Password 1", password1.name)
            assertEquals(0, password1.color)
            assertEquals(Date(12345), password1.creationDate)
            assertEquals(Date(54321), password1.alterationDate)
            assertEquals(setOf("Label 6", "Label 7"), password1.labels)
            assertEquals(2, password1.fields.size)
            assertEquals(context.getString(R.string.password), password1.fields[0].name)
            assertEquals("1234567890", password1.fields[0].value)
            assertEquals(true, password1.fields[0].secret)
            assertEquals("Property 4", password1.fields[1].name)
            assertEquals("ag2+πψψβσϑÜſ", password1.fields[1].value)
            assertEquals(false, password1.fields[1].secret)
            assertEquals(null, password1.barcode)
            assertEquals(listOf<String>(), password1.imagePaths)
        }

        val password2 = CredentialPreferenceStorage.readCredential(context, password2Id)
        assertNotNull(password2)
        if (password2 != null) {
            assertEquals("Password 1", password2.name)
            assertEquals(0, password2.color)
            assertEquals(Date(12345), password2.creationDate)
            assertEquals(Date(54321), password2.alterationDate)
            assertEquals(setOf("Label 6", "Label 7"), password2.labels)
            assertEquals(2, password2.fields.size)
            assertEquals(context.getString(R.string.password), password2.fields[0].name)
            assertEquals("1234567890", password2.fields[0].value)
            assertEquals(true, password2.fields[0].secret)
            assertEquals("Property 4", password2.fields[1].name)
            assertEquals("ag2+πψψβσϑÜſ", password2.fields[1].value)
            assertEquals(false, password2.fields[1].secret)
            assertEquals(null, password2.barcode)
            assertEquals(listOf<String>(), password2.imagePaths)
        }
    }

}