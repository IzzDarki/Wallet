package com.bennet.wallet.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.bennet.wallet.R
import com.bennet.wallet.utils.Utility.PreferenceArrayString
import com.bennet.wallet.utils.Utility.PreferenceArrayInt
import com.bennet.wallet.utils.Utility.openEncryptedPreferences
import java.util.*

import com.bennet.wallet.preferences.CardPreferenceManager.readBackImageFile

import com.bennet.wallet.preferences.CardPreferenceManager.readFrontImageFile
import com.bennet.wallet.utils.AppUtility
import com.bennet.wallet.utils.ItemProperty
import java.io.File


object CardPreferenceManager : CardOrPasswordPreferenceManager {
    const val CARDS_PREFERENCES_NAME_OLD = "com.bennet.wallet.cards"
    private const val CARDS_PREFERENCES_NAME_ENCRYPTED = "cards_encrypted"
    private const val PREFERENCE_ALL_CARD_IDS = "com.bennet.wallet.home_activity.card_ids" // (String) PreferenceArrayInt
    private const val PREFERENCE_CUSTOM_SORTING_NO_GROUPING = "custom_sorting_no_grouping" // String (PreferenceArrayInt)
    private const val PREFERENCE_CUSTOM_SORTING_WITH_GROUPING = "custom_sorting_with_grouping" // String (Map<String, List<String>>)

    private const val PREFERENCE_CARD_NAME = "com.bennet.wallet.cards.%d.name" // String
    private const val PREFERENCE_CARD_CODE = "com.bennet.wallet.cards.%d.code" // String
    private const val PREFERENCE_CARD_CODE_TYPE = "com.bennet.wallet.cards.%d.code_type" // int (see preference values below)
    private const val PREFERENCE_CARD_CODE_TYPE_TEXT = "com.bennet.wallet.cards.%d.code_type_text" // boolean
    private const val PREFERENCE_CARD_ID = "com.bennet.wallet.cards.%d.id" // String
    private const val PREFERENCE_CARD_COLOR = "com.bennet.wallet.cards.%d.color" // @ColorInt int
    private const val PREFERENCE_CARD_LABELS = "%d.labels" // String (PreferenceArrayString)
    private const val PREFERENCE_CARD_FRONT_IMAGE = "com.bennet.wallet.cards.%d.front_image_file_path" // String
    private const val PREFERENCE_CARD_BACK_IMAGE = "com.bennet.wallet.cards.%d.back_image_file_path" // String
    private const val PREFERENCE_CARD_CREATION_DATE = "%d.creation_date" // long (Date)
    private const val PREFERENCE_CARD_ALTERATION_DATE = "%d.alteration_date" // long (Date)
    private const val PREFERENCE_CARD_PROPERTIES_IDS = "com.bennet.wallet.cards.%d.card_properties_ids" // String (PreferenceArrayInt)
    private const val PREFERENCE_CARD_PROPERTY_NAME = "com.bennet.wallet.cards.%d.%d.name" // String
    private const val PREFERENCE_CARD_PROPERTY_VALUE = "com.bennet.wallet.cards.%d.%d.value" // String
    private const val PREFERENCE_CARD_PROPERTY_SECRET = "com.bennet.wallet.cards.%d.%d.property_secret" // boolean

    private const val CARD_CODE_TYPE_TEXT_DEFAULT = false // only used when value on preferences is missing (should never be missing)

    // preference values
    const val CARD_CODE_TYPE_AZTEC = 1
    const val CARD_CODE_TYPE_DATA_MATRIX = 2
    const val CARD_CODE_TYPE_PDF_417 = 4
    const val CARD_CODE_TYPE_QR = 5
    const val CARD_CODE_TYPE_CODABAR = 8
    const val CARD_CODE_TYPE_CODE_39 = 9
    const val CARD_CODE_TYPE_CODE_93 = 10
    const val CARD_CODE_TYPE_CODE_128 = 11
    const val CARD_CODE_TYPE_EAN_8 = 12
    const val CARD_CODE_TYPE_EAN_13 = 13
    const val CARD_CODE_TYPE_ITF = 14
    const val CARD_CODE_TYPE_UPC_A = 15
    const val CARD_CODE_TYPE_UPC_E = 16


    // region read functions
    /**
     * @return Card name or empty string if not found in preferences
     */
    override fun readName(context: Context, ID: Int): String {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_NAME), "")!!
    }

    /**
     * @return Color or [R.color.card_default_color] as default value if not found in preferences
     * */
    override fun readColor(context: Context, ID: Int): Int {
        return getPreferences(context).getInt(
            getKey(ID, PREFERENCE_CARD_COLOR),
            context.resources.getColor(R.color.card_default_color)
        )
    }

    /**
     * @return Creation date or (January 1, 1970, 00:00:00 GMT) if not found in preferences
     * */
    override fun readCreationDate(context: Context, ID: Int): Date {
        val time = getPreferences(context).getLong(getKey(ID, PREFERENCE_CARD_CREATION_DATE), 0)
        return Date(time)
    }

    /**
     * @return Alteration date or (January 1, 1970, 00:00:00 GMT) if not found in preferences
     */
    override fun readAlterationDate(context: Context, ID: Int): Date {
        val time = getPreferences(context).getLong(getKey(ID, PREFERENCE_CARD_ALTERATION_DATE), 0)
        return Date(time)
    }

    override fun readLabels(context: Context, ID: Int): PreferenceArrayString {
        val preferenceString =
            getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_LABELS), "")
        return PreferenceArrayString(preferenceString)
    }

    override fun readPropertyIds(context: Context, ID: Int): PreferenceArrayInt {
        return PreferenceArrayInt(
            getPreferences(context).getString(
                getKey(
                    ID,
                    PREFERENCE_CARD_PROPERTIES_IDS
                ), null
            ))
    }

    override fun readPropertyName(context: Context, ID: Int, propertyID: Int): String {
        return getPreferences(context).getString(getPropertyNameKey(ID, propertyID), "")!!
    }

    override fun readPropertyValue(context: Context, ID: Int, propertyID: Int): String {
        return getPreferences(context).getString(getPropertyValueKey(ID, propertyID), "")!!
    }

    /**
     * @return true if the property should be secret, false otherwise. Also false if not found in preferences
     */
    override fun readPropertySecret(context: Context, ID: Int, propertyID: Int): Boolean {
        return getPreferences(context).getBoolean(getPropertySecretKey(ID, propertyID), false)
    }

    /**
     * @return Card code or empty string as default value if not found in preferences
     */
    fun readCode(context: Context, ID: Int): String? {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_CODE), "")
    }

    /**
     * @return Card code type or `-1` if not found in preferences, values defined in [CardPreferenceManager], for example [CARD_CODE_TYPE_QR]
     */
    fun readCodeType(context: Context, ID: Int): Int {
        return getPreferences(context).getInt(getKey(ID, PREFERENCE_CARD_CODE_TYPE), -1)
    }

    /**
     * @return Card code type text or [CARD_CODE_TYPE_TEXT_DEFAULT] as default value if not found in preferences
     */
    fun readCodeTypeText(context: Context, ID: Int): Boolean {
        return getPreferences(context).getBoolean(getKey(ID, PREFERENCE_CARD_CODE_TYPE_TEXT), CARD_CODE_TYPE_TEXT_DEFAULT)
    }

    /**
     * @return Card id or empty string as default value if not found in preferences
     */
    @Deprecated("Card id doesn't exist anymore")
    fun readID(context: Context, ID: Int): String? {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_ID), "")
    }

    /**
     * @return Absolute path of card front image or `null` if not found in preferences
     */
    fun readFrontImagePath(context: Context, ID: Int): String? {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_FRONT_IMAGE), null)
    }

    /**
     * @return Card front image file or `null` if not found in preferences
     */
    fun readFrontImageFile(context: Context, ID: Int): File? {
        val filePath = readFrontImagePath(context, ID)
        return if (filePath != null) File(filePath) else null
    }

    /**
     * @return Absolute path of card back image or `null` if not found in preferences
     */
    fun readBackImagePath(context: Context, ID: Int): String? {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_BACK_IMAGE), null)
    }

    /**
     * @return Card back image file or `null` if not found in preferences
     */
    fun readBackImageFile(context: Context, ID: Int): File? {
        val filePath = readBackImagePath(context, ID)
        return if (filePath != null) File(filePath) else null
    }
    // endregion


    // region write functions
    override fun writeName(context: Context, ID: Int, name: String) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_NAME), name).apply()
    }

    override fun writeColor(context: Context, ID: Int, color: Int) {
        getPreferences(context).edit().putInt(getKey(ID, PREFERENCE_CARD_COLOR), color).apply()
    }

    override fun writeCreationDate(context: Context, ID: Int, creationDate: Date) {
        getPreferences(context).edit().putLong(getKey(ID, PREFERENCE_CARD_CREATION_DATE), creationDate.time).apply()
    }

    override fun writeAlterationDate(context: Context, ID: Int, alterationDate: Date) {
        getPreferences(context).edit().putLong(getKey(ID, PREFERENCE_CARD_ALTERATION_DATE), alterationDate.time).apply()
    }

    override fun writeLabels(context: Context, ID: Int, labels: PreferenceArrayString) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_LABELS), labels.toPreference()).apply()
    }

    override fun writePropertyIds(context: Context, ID: Int, propertyIDs: PreferenceArrayInt) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_PROPERTIES_IDS), propertyIDs.toPreference()).apply()
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

    fun writeCode(context: Context, ID: Int, cardCode: String?) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_CODE), cardCode).apply()
    }

    /**
     * @param codeType New card code type, values defined in [CardPreferenceManager], for example [CARD_CODE_TYPE_QR]
     */
    fun writeCodeType(context: Context, ID: Int, codeType: Int) {
        getPreferences(context).edit().putInt(getKey(ID, PREFERENCE_CARD_CODE_TYPE), codeType).apply()
    }

    fun writeCodeTypeText(context: Context, ID: Int, codeTypeText: Boolean) {
        getPreferences(context).edit()
            .putBoolean(getKey(ID, PREFERENCE_CARD_CODE_TYPE_TEXT), codeTypeText).apply()
    }

    @Deprecated("Card id doesn't exist anymore")
    fun writeID(context: Context, ID: Int, cardID: String?) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_ID), cardID).apply()
    }

    /**
     * Writes new card front image to preferences
     * @param ID Id of the card
     * @param frontImageFile Front image file. If `null` front image will be removed
     */
    fun writeFrontImage(context: Context, ID: Int, frontImageFile: File?) {
        if (frontImageFile != null)
            getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_FRONT_IMAGE), frontImageFile.absolutePath).apply()
        else
            removeFrontImage(context, ID)
    }

    /**
     * Writes new card back image to preferences
     * @param ID Id of the card
     * @param backImageFile Back image file. If `null` back image will be removed
     */
    fun writeBackImage(context: Context, ID: Int, backImageFile: File?) {
        if (backImageFile != null)
            getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_BACK_IMAGE), backImageFile.absolutePath).apply()
        else
            removeBackImage(context, ID)
    }

    /**
     * Note that this function also removes old properties
     */
    fun writeComplete(
        context: Context,
        ID: Int,
        name: String,
        color: Int,
        creationDate: Date,
        alterationDate: Date,
        labels: PreferenceArrayString,
        code: String,
        codeType: Int,
        codeTypeText: Boolean,
        frontImage: File?,
        backImage: File?,
        properties: List<ItemProperty>,
    ) {
        writeCommon(
            context,
            ID,
            name,
            color,
            creationDate,
            alterationDate,
            labels,
            properties,
        )
        writeCode(context, ID, code)
        writeCodeType(context, ID, codeType)
        writeCodeTypeText(context, ID, codeTypeText)
        writeFrontImage(context, ID, frontImage)
        writeBackImage(context, ID, backImage)
    }
    // endregion


    // region remove functions
    override fun removeName(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_NAME)).apply()
    }

    override fun removeColor(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_COLOR)).apply()
    }

    override fun removeCreationDate(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_CREATION_DATE)).apply()
    }

    override fun removeAlterationDate(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_ALTERATION_DATE)).apply()
    }

    override fun removeLabels(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_LABELS)).apply()
    }

    override fun removePropertyIds(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_PROPERTIES_IDS)).apply()
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

    fun removeCode(context: Context?, ID: Int) {
        getPreferences(context!!).edit().remove(getKey(ID, PREFERENCE_CARD_CODE)).apply()
    }

    fun removeCodeType(context: Context?, ID: Int) {
        getPreferences(context!!).edit().remove(getKey(ID, PREFERENCE_CARD_CODE_TYPE)).apply()
    }

    fun removeCodeTypeText(context: Context?, ID: Int) {
        getPreferences(context!!).edit().remove(getKey(ID, PREFERENCE_CARD_CODE_TYPE_TEXT)).apply()
    }

    @Deprecated("Card id doesn't exist anymore")
    fun removeID(context: Context?, ID: Int) {
        getPreferences(context!!).edit().remove(getKey(ID, PREFERENCE_CARD_ID)).apply()
    }

    private fun removeFrontImage(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_FRONT_IMAGE)).apply()
    }

    fun deleteFrontImage(context: Context, ID: Int) {
        val image: File? = readFrontImageFile(context, ID)
        if (image != null) {
            if (!image.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardPreferenceManager", "Couldn't delete front image");
                 */
            }
        }
        removeFrontImage(context, ID)
    }

    private fun removeBackImage(context: Context, ID: Int) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_BACK_IMAGE)).apply()
    }

    fun deleteBackImage(context: Context, ID: Int) {
        val image: File? = readBackImageFile(context, ID)
        if (image != null) {
            if (!image.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardPreferenceManager", "Couldn't delete back image");
                 */
            }
        }
        removeBackImage(context, ID)
    }

    override fun removeComplete(context: Context, ID: Int) {
        super.removeComplete(context, ID)

        removeCode(context, ID)
        removeCodeType(context, ID)
        removeCodeTypeText(context, ID)
        deleteFrontImage(context, ID)
        deleteBackImage(context, ID)
    }

    // endregion


    // region all IDs
    override fun readAllIDs(context: Context): PreferenceArrayInt {
        return PreferenceArrayInt(getPreferences(context).getString(PREFERENCE_ALL_CARD_IDS, null));
    }

    override fun writeAllIDs(context: Context, allIDs: PreferenceArrayInt) {
        getPreferences(context).edit().putString(PREFERENCE_ALL_CARD_IDS, allIDs?.toPreference()).apply();
    }

    /**
     * Adds [ID] to list of all card ids in preferences but only if it is not yet contained<br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case [ID] would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all card ids is edited in that moment
     * @param ID Id of the card
     */
    override fun addToAllIDs(context: Context, ID: Int) {
        val cardIDs: PreferenceArrayInt = readAllIDs(context)
        if (!cardIDs.contains(ID))
            cardIDs.add(ID)
        writeAllIDs(context, cardIDs)
    }

    /**
     * Removes [ID] from list of all card ids in preferences <br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case [ID] would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all card ids is edited in that moment
     * @param ID Id of the card
     */
    override fun removeFromAllIDs(context: Context, ID: Int) {
        val cardIDs: PreferenceArrayInt = readAllIDs(context)
        cardIDs.remove(ID) // no need to check if cardIDs contains ID

        writeAllIDs(context, cardIDs)
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
            getPreferences(context).getString(PREFERENCE_CUSTOM_SORTING_WITH_GROUPING, "")!!
        )
    }

    override fun writeCustomSortingNoGrouping(context: Context, customSorting: PreferenceArrayInt
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

    fun getPreferences(context: Context): SharedPreferences {
        if (preferences == null)
            // preferences = context.getSharedPreferences("cards-test", MODE_PRIVATE) // not encrypted for testing
            preferences = openEncryptedPreferences(context, CARDS_PREFERENCES_NAME_ENCRYPTED)
        return preferences!!
    }

    private fun getKey(ID: Int, cardPropertyPreferenceKey: String?): String {
        return String.format(Locale.ENGLISH, cardPropertyPreferenceKey!!, ID)
    }

    private fun getPropertyNameKey(ID: Int, propertyID: Int): String {
        return String.format(Locale.ENGLISH, PREFERENCE_CARD_PROPERTY_NAME, ID, propertyID)
    }

    private fun getPropertyValueKey(ID: Int, propertyID: Int): String {
        return String.format(Locale.ENGLISH, PREFERENCE_CARD_PROPERTY_VALUE, ID, propertyID)
    }

    private fun getPropertySecretKey(ID: Int, propertyID: Int): String {
        return String.format(Locale.ENGLISH, PREFERENCE_CARD_PROPERTY_SECRET, ID, propertyID)
    }
    // endregion
}