package com.izzdarki.wallet.preferences

import android.content.Context
import com.izzdarki.wallet.utils.CardOrPasswordPreviewData
import com.izzdarki.wallet.utils.ItemProperty
import com.izzdarki.wallet.utils.Utility
import java.util.*

sealed interface CardOrPasswordPreferenceManager {
    fun readName(context: Context, ID: Int): String
    fun readColor(context: Context, ID: Int): Int
    fun readCreationDate(context: Context, ID: Int): Date
    fun readAlterationDate(context: Context, ID: Int): Date
    fun readLabels(context: Context, ID: Int): Utility.PreferenceArrayString
    fun readPropertyIds(context: Context, ID: Int): Utility.PreferenceArrayInt
    fun readPropertyName(context: Context, ID: Int, propertyID: Int): String
    fun readPropertyValue(context: Context, ID: Int, propertyID: Int): String
    fun readPropertySecret(context: Context, ID: Int, propertyID: Int): Boolean
    fun readProperties(context: Context, ID: Int): MutableList<ItemProperty> {
        return readPropertyIds(context, ID)
            .map { propertyID ->
                ItemProperty(
                    propertyID,
                    name = readPropertyName(context, ID, propertyID),
                    value = readPropertyValue(context, ID, propertyID),
                    secret = readPropertySecret(context, ID, propertyID),
                )
            }.toMutableList()
    }

    fun writeName(context: Context, ID: Int, name: String)
    fun writeColor(context: Context, ID: Int, color: Int)
    fun writeCreationDate(context: Context, ID: Int, creationDate: Date)
    fun writeAlterationDate(context: Context, ID: Int, alterationDate: Date)
    fun writeLabels(context: Context, ID: Int, labels: Utility.PreferenceArrayString)
    fun writePropertyIds(context: Context, ID: Int, propertyIDs: Utility.PreferenceArrayInt)
    fun writePropertyName(context: Context, ID: Int, propertyID: Int, propertyName: String)
    fun writePropertyValue(context: Context, ID: Int, propertyID: Int, propertyValue: String)
    fun writePropertySecret(context: Context, ID: Int, propertyID: Int, propertySecret: Boolean)

    /**
     * Writes data, that is common between cards and passwords
     * Note that this function also removes old properties
     */
    fun writeCommon(
        context: Context,
        ID: Int,
        name: String,
        color: Int,
        creationDate: Date,
        alterationDate: Date,
        labels: Utility.PreferenceArrayString,
        properties: List<ItemProperty>
    ) {
        addToAllIDs(context, ID) // This should be fine. At this moment no other process should modify this preference list (Note that this only adds the ID if it is not yet contained in the list)
        writeName(context, ID, name)
        writeColor(context, ID, color)
        writeCreationDate(context, ID, creationDate)
        writeAlterationDate(context, ID, alterationDate)
        writeLabels(context, ID, labels)

        // remove old properties
        for (propertyID in readPropertyIds(context, ID)) {
            removePropertyName(context, ID, propertyID)
            removePropertyValue(context, ID, propertyID)
            removePropertySecret(context, ID, propertyID)
        }

        // write properties
        val currentPropertyIDs =  Utility.PreferenceArrayInt() // Collects all current propertyIDs to write into preferences
        for (property in properties) {
            currentPropertyIDs.add(property.propertyID)
            writePropertyName(context, ID, property.propertyID, property.name)
            writePropertyValue(context, ID, property.propertyID, property.value)
            writePropertySecret(context, ID, property.propertyID, property.secret)
        }
        writePropertyIds(context, ID, currentPropertyIDs)
    }

    fun removeName(context: Context, ID: Int)
    fun removeColor(context: Context, ID: Int)
    fun removeCreationDate(context: Context, ID: Int)
    fun removeAlterationDate(context: Context, ID: Int)
    fun removeLabels(context: Context, ID: Int)
    fun removePropertyIds(context: Context, ID: Int)
    fun removePropertyName(context: Context, ID: Int, propertyID: Int)
    fun removePropertyValue(context: Context, ID: Int, propertyID: Int)
    fun removePropertySecret(context: Context, ID: Int, propertyID: Int)
    fun removeComplete(context: Context, ID: Int) {
        removeName(context, ID)
        removeColor(context, ID)
        removeCreationDate(context, ID)
        removeAlterationDate(context, ID)
        removeLabels(context, ID)
        for (propertyID in readPropertyIds(context, ID)) {
            removePropertyName(context, ID, propertyID)
            removePropertyValue(context, ID, propertyID)
            removePropertySecret(context, ID, propertyID)
        }
        removePropertyIds(context, ID)
        removeFromAllIDs(context, ID)
    }

    fun readAllIDs(context: Context): Utility.PreferenceArrayInt
    fun writeAllIDs(context: Context, allIDs: Utility.PreferenceArrayInt)
    fun addToAllIDs(context: Context, ID: Int)
    fun removeFromAllIDs(context: Context, ID: Int)
    fun readAll(context: Context): List<CardOrPasswordPreviewData> {
        return readAllIDs(context).map { ID ->
            CardOrPasswordPreviewData(
                ID,
                readName(context, ID),
                readColor(context, ID)
            )
        }
    }

    fun readCustomSortingNoGrouping(context: Context): Utility.PreferenceArrayInt
    fun readCustomSortingWithGrouping(context: Context): Map<String, List<String>>
    fun writeCustomSortingNoGrouping(context: Context, customSorting: Utility.PreferenceArrayInt)
    fun writeCustomSortingWithGrouping(context: Context, customSorting: Map<String, List<String>>)

    fun collectAllLabels(context: Context): Set<String> {
        val labels = mutableSetOf<String>()
        for (ID in readAllIDs(context)) {
            labels.addAll(readLabels(context, ID))
        }
        return labels
    }
}