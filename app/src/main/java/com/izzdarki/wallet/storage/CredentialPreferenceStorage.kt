package com.izzdarki.wallet.storage

import android.content.Context
import android.content.SharedPreferences
import com.izzdarki.wallet.data.Barcode
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.utils.Utility
import java.util.Date
import kotlin.random.Random

object CredentialPreferenceStorage : CredentialReadStorage, CredentialWriteStorage {

    override fun readCredential(context: Context, id: Int): Credential? {
        return getPreferences(context).getString("$CREDENTIAL_KEY$id", null)?.let { credentialString ->
            credentialFromString(credentialString, id)
        }
    }

    @Synchronized
    override fun writeCredential(context: Context, credential: Credential): Boolean? {
        val success = addToAllIds(context, credential.id) // synchronously update ids
        if (!success)
            return false

        getPreferences(context).edit()
            .putString("$CREDENTIAL_KEY${credential.id}", credentialToString(credential))
            .apply()
        return null // Asynchronous write does not know success
    }

    @Synchronized
    override fun removeCredential(context: Context, id: Int): Boolean? {
        val success = getPreferences(context).edit()
            .putString(ALL_IDS_KEY, (readAllIds(context) - setOf(id)).joinToString(","))
            .commit() // Make sure that ids are updated synchronously
        if (!success)
            return false

        getPreferences(context).edit()
            .remove("$CREDENTIAL_KEY$id")
            .apply()
        return null // Asynchronous write does not know success
    }

    override fun readAllIds(context: Context): List<Int> {
        return getPreferences(context)
            .getString(ALL_IDS_KEY, null)
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { it.toIntOrNull() } // null should never occur
            ?: emptyList()
    }


    private fun addToAllIds(context: Context, id: Int): Boolean {
        val newIds: Set<Int> = readAllIds(context).toSet() + id
        return getPreferences(context).edit()
            .putString(ALL_IDS_KEY, newIds.joinToString(","))
            .commit()
    }

    /**
     * Get a string representation of the credential.
     * Uses a randomly chosen character for separation to ensure that all password strings are representable
     */
    internal fun credentialToString(credential: Credential): String {
        val separator = generateSeparator(credential) // always superseded by a number to represent level of nesting
        return separator + // separator is always the first character
                credential.name +
                "${separator}1" + credential.color +
                "${separator}1" + credential.creationDate.time +
                "${separator}1" + credential.alterationDate.time +
                "${separator}1" + credential.labels.joinToString("${separator}2") +
                "${separator}1" + credential.fields.joinToString("${separator}2") { propertyToString(it, separator) } +
                "${separator}1" + barcodeToString(credential.barcode, separator) +
                "${separator}1" + credential.imagePaths.joinToString("${separator}2")
    }

    internal fun credentialFromString(credentialString: String, id: Int): Credential {
        val separator = credentialString[0]
        val credentialComponents = credentialString.substring(startIndex = 1).split("${separator}1")
        return Credential(
            id = id,
            name = credentialComponents[0],
            color = credentialComponents[1].toInt(),
            creationDate = Date(credentialComponents[2].toLong()),
            alterationDate = Date(credentialComponents[3].toLong()),
            labels = credentialComponents[4]
                .split("${separator}2")
                .filter { it.isNotEmpty() } // handles the case of an empty label list
                .toMutableSet(),
            fields = credentialComponents[5]
                .split("${separator}2")
                .filter { it.isNotEmpty() } // handles the case of an empty property list
                .map { propertyFromString(it, separator) }
                .toMutableList(),
            barcode = barcodeFromString(credentialComponents[6], separator),
            imagePaths = credentialComponents[7]
                .split("${separator}2")
                .filter { it.isNotEmpty() } // handles the case of an empty image path list
                .toMutableList()
        )
    }

    private fun propertyToString(property: CredentialField, separator: Char): String {
        return property.name +
                "${separator}3" + property.value +
                "${separator}3" + property.secret
    }

    private fun barcodeToString(barcode: Barcode?, separator: Char): String {
        if (barcode == null)
            return "${separator}3"
        return barcode.code +
                "${separator}2" + barcode.type +
                "${separator}2" + barcode.showText
    }

    private fun propertyFromString(propertyString: String, separator: Char): CredentialField {
        val propertyComponents = propertyString.split("${separator}3")
        return CredentialField(
            propertyID = CredentialField.INVALID_ID, // Property IDs were only needed for the old storage system
            name = propertyComponents[0],
            value = propertyComponents[1],
            secret = propertyComponents[2].toBoolean()
        )
    }

    private fun barcodeFromString(barcodeString: String, separator: Char): Barcode? {
        if (barcodeString == "${separator}3")
            return null
        val barcodeComponents = barcodeString.split("${separator}2")
        return Barcode(
            code = barcodeComponents[0],
            type = barcodeComponents[1].toInt(),
            showText = barcodeComponents[2].toBoolean()
        )
    }

    private fun doesCredentialContainSeparator(credential: Credential, separator: Char) =
        credential.name.contains(separator) ||
                credential.labels.any { it.contains(separator) } ||
                credential.fields.any { it.name.contains(separator) || it.value.contains(separator) } ||
                credential.barcode?.code?.contains(separator) ?: false

    private fun generateSeparator(credential: Credential): Char {
        var separator: Char
        do {
            separator = generateRandomUTF8Character()
        } while (doesCredentialContainSeparator(credential, separator))
        return separator
    }

    private fun generateRandomUTF8Character(): Char {
        val codePoint = Random.nextInt(0x10FFFF + 1)
        return codePoint.toChar().toString().toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8)[0]
    }

    @Synchronized fun getPreferences(context: Context): SharedPreferences {
        if (preferences == null)
            preferences = Utility.openEncryptedPreferences(context, PREFERENCES_NAME)
        return preferences!!
    }

    private var preferences: SharedPreferences? = null

    private const val PREFERENCES_NAME = "credentials"
    private const val CREDENTIAL_KEY = "credential"
    private const val ALL_IDS_KEY = "all_ids"

}
