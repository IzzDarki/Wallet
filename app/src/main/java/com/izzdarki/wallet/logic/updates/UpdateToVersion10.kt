@file:Suppress("DEPRECATION")
package com.izzdarki.wallet.logic.updates

import android.content.Context
import com.izzdarki.wallet.data.Barcode
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.logic.generateNewId
import com.izzdarki.wallet.storage.AppPreferenceManager
import com.izzdarki.wallet.storage.CardStorage
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.storage.PasswordStorage
import izzdarki.wallet.R
import java.io.File

/**
 * Moves all cards and passwords from [CardStorage] and [PasswordStorage]
 * to [CredentialPreferenceStorage].
 * Adds a label "Card" to all credentials from [CardStorage]
 * Writes the password as the first property, because it is not
 * contained as a special component of [Credential]
 *
 * Also moves custom sorting orders (just without group by labels) to [AppPreferenceManager].
 * The new sorting order will reflect the new ids and will
 * contain first the cards and then the passwords.
 */
fun updateToCredentialPreferences(context: Context) {

    // Get all cards
    val cards = readAllCards(context)
    val passwords = readAllPasswords(context)

    val credentials = cards + passwords

    // Write all credentials
    val usedNewIds = mutableListOf<Int>()
    val idMap = mutableMapOf<Int, Int>()
    for (credential in credentials) {
        // Pick new id
        val newID = generateNewId(usedNewIds)
        idMap[credential.id] = newID
        credential.id = newID
        CredentialPreferenceStorage.writeCredential(context, credential)
    }

    // Move custom sorting orders combined to AppPreferenceManager
    val cardsCustomSortingOrder = CardStorage.readCustomSortingNoGrouping(context)
        .mapNotNull { id -> idMap[id] }
    val passwordsCustomSortingOrder = PasswordStorage.readCustomSortingNoGrouping(context)
        .mapNotNull { id -> idMap[id] }
    val combinedSortingOrder = (cardsCustomSortingOrder + passwordsCustomSortingOrder).distinct()
    AppPreferenceManager.setCredentialsCustomSortingOrder(context, combinedSortingOrder)
}


/**
 * Clear and delete the preferences of [CardStorage] and [PasswordStorage]
 */
fun removeOldPreferences(context: Context) {
    CardStorage.getPreferences(context).edit().clear().commit()
    PasswordStorage.getPreferences(context).edit().clear().commit()

    val sharedPrefDir = "${context.applicationInfo.dataDir}/shared_prefs"

    val cardStorageFile = File(sharedPrefDir, "${CardStorage.CARDS_PREFERENCES_NAME_ENCRYPTED}.xml")
    val passwordStorageFile = File(sharedPrefDir, "${PasswordStorage.PASSWORDS_PREFERENCES_NAME}.xml")

    if (cardStorageFile.exists())
        cardStorageFile.delete()

    if (passwordStorageFile.exists())
        passwordStorageFile.delete()
}

private fun readAllCards(context: Context): List<Credential> {
    return CardStorage.readAllIDs(context)
        .map { id ->
            Credential(
                id,
                name = CardStorage.readName(context, id),
                color = CardStorage.readColor(context, id),
                creationDate = CardStorage.readCreationDate(context, id),
                alterationDate = CardStorage.readAlterationDate(context, id),
                labels = (CardStorage.readLabels(context, id).toSet() +
                        setOf(context.getString(R.string.card))).toMutableSet(), // add a label "Card"
                fields = CardStorage.readProperties(context, id),
                barcode = readBarcode(context, id),
                imagePaths = listOfNotNull(
                    CardStorage.readFrontImagePath(context, id),
                    CardStorage.readBackImagePath(context, id)
                ).toMutableList()
            )
        }
}

private fun readAllPasswords(context: Context): List<Credential> {
    return PasswordStorage.readAllIDs(context)
        .map { id ->
            Credential(
                id,
                name = PasswordStorage.readName(context, id),
                color = PasswordStorage.readColor(context, id),
                creationDate = PasswordStorage.readCreationDate(context, id),
                alterationDate = PasswordStorage.readAlterationDate(context, id),
                labels = PasswordStorage.readLabels(context, id).toMutableSet(),
                fields = readPropertiesAndAddPassword(context, id),
                barcode = null,
                imagePaths = mutableListOf()
            )
        }
}

private fun readBarcode(context: Context, id: Int): Barcode? {
    val code = CardStorage.readCode(context, id)

    return if (code == null || code == "")
        null
    else
        Barcode(
            code = code,
            type = CardStorage.readCodeType(context, id),
            showText = CardStorage.readCodeTypeText(context, id),
    )
}

private fun readPropertiesAndAddPassword(context: Context, id: Int): MutableList<CredentialField> {
    val properties = PasswordStorage.readProperties(context, id)
    val passwordValue = PasswordStorage.readPasswordValue(context, id)
    if (passwordValue != "")
        properties.add(
            index = 0,
            CredentialField(
                propertyID = CredentialField.INVALID_ID,
                name = context.getString(R.string.password),
                value = passwordValue,
                secret = true,
            )
        )
    return properties
}
