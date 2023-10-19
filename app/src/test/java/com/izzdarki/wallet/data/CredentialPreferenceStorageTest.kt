package com.izzdarki.wallet.data

import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class CredentialPreferenceStorageTest {

    @Test
    fun testCredentialToAndFromString() {
        // Tests if the credential can be converted to a string and back to a credential
        // Note that this will only work for credentials for which the property IDs are set to CredentialProperty.INVALID_ID
        val credential = Credential(
            id = 12,
            name = "Test Credential",
            color = 0xFF0000,
            creationDate = Date(123),
            alterationDate = Date(4312),
            labels = mutableSetOf("label1", "label2"),
            fields = mutableListOf(
                CredentialField(CredentialField.INVALID_ID, "property1", "value1", false),
                CredentialField(CredentialField.INVALID_ID, "property2", "value2", true)
            ),
            barcode = Barcode("barcode", Barcode.TYPE_AZTEC, true),
            imagePaths = mutableListOf("path1", "path2")
        )
        val credentialString = CredentialPreferenceStorage.credentialToString(credential)
        val credentialFromString = CredentialPreferenceStorage.credentialFromString(credentialString, 12)
        assertEquals(credential, credentialFromString)

    }

}