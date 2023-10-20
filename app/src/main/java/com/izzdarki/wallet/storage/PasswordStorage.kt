@file:Suppress("DEPRECATION")
package com.izzdarki.wallet.storage

import android.content.Context
import android.content.SharedPreferences
import izzdarki.wallet.R
import com.izzdarki.wallet.utils.AppUtility
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.utils.Utility
import com.izzdarki.wallet.utils.Utility.PreferenceArrayString
import com.izzdarki.wallet.utils.Utility.PreferenceArrayInt
import com.izzdarki.wallet.utils.Utility.getAttributeColor
import java.util.*

@Deprecated("Implementations of interfaces CredentialReadStorage and CredentialWriteStorage are used now for cards and passwords together")
object PasswordStorage : CardOrPasswordStorage {
    const val PASSWORDS_PREFERENCES_NAME = "passwords"

    private const val PREFERENCE_ALL_PASSWORD_IDS = "password_ids"
    private const val PREFERENCE_CUSTOM_SORTING_NO_GROUPING = "custom_sorting_no_grouping" // String (PreferenceArrayInt)
    private const val PREFERENCE_CUSTOM_SORTING_WITH_GROUPING = "custom_sorting_with_grouping" // String (Map<String, List<String>>)

    private const val PREFERENCE_PASSWORD_NAME = "%d.name" // String
    private const val PREFERENCE_PASSWORD_VALUE = "%d.password" // String
    private const val PREFERENCE_PASSWORD_COLOR = "%d.color" // @ColorInt int
    private const val PREFERENCE_PASSWORD_CREATION_DATE = "%d.creation_date" // long (Date)
    private const val PREFERENCE_PASSWORD_ALTERATION_DATE = "%d.alteration_date" // long (Date)
    private const val PREFERENCE_PASSWORD_LABELS = "%d.labels" // String (PreferenceArrayString)
    private const val PREFERENCE_PASSWORD_PROPERTIES_IDS = "%d.password_properties" // String (PreferenceArrayInt)
    private const val PREFERENCE_PASSWORD_PROPERTY_NAME = "%d.%d.property_name" // String
    private const val PREFERENCE_PASSWORD_PROPERTY_VALUE = "%d.%d.property_value" // String
    private const val PREFERENCE_PASSWORD_PROPERTY_SECRET = "%d.%d.property_secret" // boolean

    // region read functions
    /**
     * @return Password name or empty string if not found in preferences
     */
    override fun readName(context: Context, ID: Int): String {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_NAME), "")!!
    }

    /**
     * @return Password color or default color if not found in preferences
     */
    override fun readColor(context: Context, ID: Int): Int {
        return getPreferences(context).getInt(
            getKey(ID, PREFERENCE_PASSWORD_COLOR),
            context.getAttributeColor(R.attr.colorPrimaryContainer)
        )
    }

    /**
     * @return Password creation date or (January 1, 1970, 00:00:00 GMT) if not found in preferences
     */
    override fun readCreationDate(context: Context, ID: Int): Date {
        return Date(
            getPreferences(context).getLong(getKey(ID, PREFERENCE_PASSWORD_CREATION_DATE), 0)
        )
    }

    /**
     * @return Password alteration date or (January 1, 1970, 00:00:00 GMT) if not found in preferences
     */
    override fun readAlterationDate(context: Context, ID: Int): Date {
        return Date(
            getPreferences(context).getLong(getKey(ID, PREFERENCE_PASSWORD_ALTERATION_DATE), 0)
        )
    }

    override fun readLabels(context: Context, ID: Int): PreferenceArrayString {
        val preferenceString = getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_LABELS), "")
        return PreferenceArrayString(preferenceString)
    }

    override fun readPropertyIds(context: Context, ID: Int): PreferenceArrayInt {
        return PreferenceArrayInt(
            getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_PROPERTIES_IDS), "")
        )
    }

    override fun readPropertyName(context: Context, ID: Int, propertyID: Int): String {
        return getPreferences(context).getString(getPropertyNameKey(ID, propertyID), "")!!
    }

    override fun readPropertyValue(context: Context, ID: Int, propertyID: Int): String {
        return getPreferences(context).getString(getPropertyValueKey(ID, propertyID), "")!!
    }

    /**
     * @return `true` if the property should be secret, `false` otherwise. Also `true` if not found in preferences
     */
    override fun readPropertySecret(context: Context, ID: Int, propertyID: Int): Boolean {
        return getPreferences(context).getBoolean(getPropertySecretKey(ID, propertyID), true)
    }

    /**
     * @return Password of the stored password or empty string if not found in preferences
     */
    fun readPasswordValue(context: Context, ID: Int): String {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_VALUE), "")!!
    }
    // endregion


    // region write functions
    override fun writeName(context: Context, ID: Int, name: String) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_PASSWORD_NAME), name).apply()
    }

    override fun writeColor(context: Context, ID: Int, color: Int) {
        getPreferences(context).edit().putInt(getKey(ID, PREFERENCE_PASSWORD_COLOR), color).apply()
    }

    override fun writeCreationDate(context: Context, ID: Int, creationDate: Date) {
        getPreferences(context).edit().putLong(getKey(ID, PREFERENCE_PASSWORD_CREATION_DATE), creationDate.time).apply()
    }

    override fun writeAlterationDate(context: Context, ID: Int, alterationDate: Date) {
        getPreferences(context).edit().putLong(getKey(ID, PREFERENCE_PASSWORD_ALTERATION_DATE), alterationDate.time).apply()
    }

    override fun writeLabels(context: Context, ID: Int, labels: PreferenceArrayString) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_PASSWORD_LABELS), labels.toPreference()).apply()
    }

    override fun writePropertyIds(context: Context, ID: Int, propertyIDs: PreferenceArrayInt) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_PASSWORD_PROPERTIES_IDS), propertyIDs.toPreference()).apply()
    }

    override fun writePropertyName(context: Context, ID: Int, propertyID: Int, propertyName: String) {
        getPreferences(context).edit().putString(getPropertyNameKey(ID, propertyID), propertyName).apply()
    }

    override fun writePropertyValue(context: Context, ID: Int, propertyID: Int, propertyValue: String) {
        getPreferences(context).edit().putString(getPropertyValueKey(ID, propertyID), propertyValue).apply()
    }

    override fun writePropertySecret(context: Context, ID: Int, propertyID: Int, propertySecret: Boolean) {
        getPreferences(context).edit().putBoolean(getPropertySecretKey(ID, propertyID), propertySecret).apply()
    }

    fun writePasswordValue(context: Context, ID: Int, passwordValue: String) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_PASSWORD_VALUE), passwordValue).apply()
    }

    /**
     * Note that this function also removes old properties
     */
    fun writeComplete(
        context: Context,
        ID: Int,
        name: String,
        passwordValue: String,
        color: Int,
        creationDate: Date,
        alterationDate: Date,
        labels: PreferenceArrayString,
        properties: List<CredentialField>
    ) {
        writeCommon(
            context,
            ID,
            name,
            color,
            creationDate,
            alterationDate,
            labels,
            properties
        )
        writePasswordValue(context, ID, passwordValue)
    }
    // endregion


    // region remove functions
    override fun removeName(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_NAME)).apply()
    }

    override fun removeColor(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_COLOR)).apply()
    }

    override fun removeCreationDate(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_CREATION_DATE)).apply()
    }

    override fun removeAlterationDate(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_ALTERATION_DATE)).apply()
    }

    override fun removeLabels(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_LABELS)).apply()
    }

    override fun removePropertyIds(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_PROPERTIES_IDS)).apply()
    }

    override fun removePropertyName(context: Context, ID: Int, propertyID: Int) {
        getPreferences(context).edit().remove(getPropertyNameKey(ID, propertyID)).apply()
    }

    override fun removePropertyValue(context: Context, ID: Int, propertyID: Int) {
        getPreferences(context).edit().remove(getPropertyValueKey(ID, propertyID)).apply()
    }

    override fun removePropertySecret(context: Context, ID: Int, propertyID: Int) {
        getPreferences(context).edit().remove(getPropertySecretKey(ID, propertyID)).apply()
    }

    fun removePasswordValue(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_VALUE)).apply()
    }

    override fun removeComplete(context: Context, ID: Int) {
        super.removeComplete(context, ID)
        removePasswordValue(context, ID)
    }
    // endregion


    // region all ids
    override fun readAllIDs(context: Context): PreferenceArrayInt {
        return PreferenceArrayInt(getPreferences(context).getString(PREFERENCE_ALL_PASSWORD_IDS, ""))
    }

    override fun writeAllIDs(context: Context, allIDs: PreferenceArrayInt) {
        getPreferences(context).edit().putString(PREFERENCE_ALL_PASSWORD_IDS, allIDs.toPreference()).apply()
    }

    /**
     * Adds [ID] to list of all password ids in preferences but only if it is not yet contained<br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case [ID] would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all password ids is edited in that moment
     * @param ID Id of the password
     */
    override fun addToAllIDs(context: Context, ID: Int) {
        val allIDs = readAllIDs(context)
        if (ID !in allIDs)
            allIDs.add(ID)
        writeAllIDs(context, allIDs)
    }


    /**
     * Removes [ID] from list of all password ids in preferences <br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case [ID] would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all password ids is edited in that moment
     * @param ID Id of the password
     */
    override fun removeFromAllIDs(context: Context, ID: Int) {
        val allIDs = readAllIDs(context)
        allIDs.remove(ID) // no need to check if allIDs contains ID
        writeAllIDs(context, allIDs)
    }
    // endregion


    // region custom sorting
    override fun readCustomSortingNoGrouping(context: Context): PreferenceArrayInt {
        return PreferenceArrayInt(
            getPreferences(context).getString(PREFERENCE_CUSTOM_SORTING_NO_GROUPING, "")
        )
    }

    override fun readCustomSortingWithGrouping(context: Context): Map<String, List<String>> {
        return AppUtility.CustomSortingWithGrouping.fromString(
            CardStorage.getPreferences(context).getString(PREFERENCE_CUSTOM_SORTING_WITH_GROUPING, "")!!
        )
    }

    override fun writeCustomSortingNoGrouping(
        context: Context,
        customSorting: PreferenceArrayInt
    ) {
        getPreferences(context).edit().putString(PREFERENCE_CUSTOM_SORTING_NO_GROUPING, customSorting.toPreference()).apply()
    }

    override fun writeCustomSortingWithGrouping(context: Context, customSorting: Map<String, List<String>>) {
        getPreferences(context).edit().putString(
            PREFERENCE_CUSTOM_SORTING_WITH_GROUPING,
            AppUtility.CustomSortingWithGrouping.toString(customSorting)
        ).apply()
    }
    // endregion


    // region private helper
    private var preferences: SharedPreferences? = null

    @Synchronized fun getPreferences(context: Context): SharedPreferences {
        if (preferences == null)
            // preferences = context.getSharedPreferences("passwords-test", Context.MODE_PRIVATE) // not encrypted for testing
            preferences = Utility.openEncryptedPreferences(context, PASSWORDS_PREFERENCES_NAME)
        return preferences!!
    }

    private fun getKey(ID: Int, propertyPreferenceKey: String?): String {
        return String.format(Locale.ENGLISH, propertyPreferenceKey!!, ID)
    }

    private fun getPropertyNameKey(ID: Int, propertyID: Int): String {
        return String.format(Locale.ENGLISH,
            PREFERENCE_PASSWORD_PROPERTY_NAME, ID, propertyID)
    }

    private fun getPropertyValueKey(ID: Int, propertyID: Int): String {
        return String.format(Locale.ENGLISH,
            PREFERENCE_PASSWORD_PROPERTY_VALUE, ID, propertyID)
    }

    private fun getPropertySecretKey(ID: Int, propertyID: Int): String {
        return String.format(Locale.ENGLISH,
            PREFERENCE_PASSWORD_PROPERTY_SECRET, ID, propertyID)
    }
    // endregion
}
