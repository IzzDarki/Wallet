package com.izzdarki.wallet.storage

import android.content.Context
import androidx.preference.PreferenceManager
import izzdarki.wallet.R
import com.izzdarki.wallet.ui.credentials.CredentialActivity
import java.util.*

/**
 * This static class is used for reading the app preferences
 */
object AppPreferenceManager {

    private const val PREFERENCE_SORTING_TYPE_KEY = "cards.sorting_type" // Int
    private const val PREFERENCE_SORT_REVERSE_KEY = "cards.sort_reverse" // Boolean
    private const val PREFERENCE_CUSTOM_SORTING_ORDER_KEY = "custom_sorting_order" // List<Long>

    enum class SortingType {
        ByName,
        CustomSorting,
        ByCreationDate,
        ByAlterationDate
    }

    @JvmStatic
    fun getCredentialsSortingType(context: Context): SortingType
        = getSortingType(context, PREFERENCE_SORTING_TYPE_KEY)

    @JvmStatic
    fun isCredentialsSortReverse(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREFERENCE_SORT_REVERSE_KEY, false)
    }

    @JvmStatic
    fun getCredentialsCustomSortingOrder(context: Context): List<Long> {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREFERENCE_CUSTOM_SORTING_ORDER_KEY, "")!!
            .split(",")
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toLongOrNull() }
    }


    @JvmStatic
    fun setCredentialsSortingType(context: Context, sortingType: SortingType) {
        setSortingType(context, PREFERENCE_SORTING_TYPE_KEY, sortingType)
    }

    @JvmStatic
    fun setCredentialsSortReverse(context: Context, sortReverse: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PREFERENCE_SORT_REVERSE_KEY, sortReverse)
            .apply()
    }

    @JvmStatic
    fun setCredentialsCustomSortingOrder(context: Context, customSortingOrder: List<Long>) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(PREFERENCE_CUSTOM_SORTING_ORDER_KEY, customSortingOrder.joinToString(","))
            .apply()
    }

    /**
     * @param context Context
     * @return Returns the string value of the app theme in preferences, null if not found in preferences
     */
    @JvmStatic
    fun getAppDarkMode(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.preferences_theme_key), null)
    }

    @JvmStatic
    fun isBackConfirmNewCredential(context: Context): Boolean {
        val stringSet = getBackConfirmStringSet(context)
        return stringSet!!.contains(context.getString(R.string.preferences_back_confirm_entry_values_new_entry))
    }

    @JvmStatic
    fun isBackConfirmCrop(context: Context): Boolean {
        val stringSet = getBackConfirmStringSet(context)
        return stringSet!!.contains(context.getString(R.string.preferences_back_confirm_entry_values_crop))
    }

    @JvmStatic
    fun isBackConfirmEditCredential(context: Context): Boolean {
        val stringSet = getBackConfirmStringSet(context)
        return stringSet!!.contains(context.getString(R.string.preferences_back_confirm_entry_values_edit_entry))
    }

    @JvmStatic
    fun getDefaultBarcodeType(context: Context): Int {
        val defaultTypeString = PreferenceManager.getDefaultSharedPreferences(context).getString(
            context.getString(R.string.preferences_default_values_barcode_key),
            context.getString(R.string.preferences_default_values_barcode_default)
        )!!
        return CredentialActivity.barcodeTypeStringToInt(context, defaultTypeString)
    }

    @JvmStatic
    fun getDefaultWithText(context: Context): Boolean {
        val defaultTextString = PreferenceManager.getDefaultSharedPreferences(context).getString(
            context.getString(R.string.preferences_default_values_code_text_key),
            context.getString(R.string.preferences_default_values_code_text_default)
        )
        return defaultTextString == context.getString(R.string.preferences_default_values_code_text_entry_values_with_text)
    }

    /**
     * Checks in the user preferences, whether the user wants to receive detailed error messages or not
     * @param context Context
     * @return Returns true when detailed error messages are turned on, otherwise false
     */
    @JvmStatic
    fun isDetailedErrors(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(R.string.preferences_detailed_errors_key),
            context.resources.getBoolean(R.bool.preferences_detailed_errors_default)
        )
    }

    @JvmStatic
    fun isLengthHiddenInSecretFields(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(R.string.preferences_hide_length_secret_field_key),
            context.resources.getBoolean(R.bool.preferences_hide_length_secret_field_default)
        )
    }

    @JvmStatic
    fun isMonospaceInSecretFields(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(R.string.preferences_secret_field_monospace_key),
            context.resources.getBoolean(R.bool.preferences_secret_field_monospace_default)
        )
    }

    // helper
    private fun getBackConfirmStringSet(context: Context): Set<String>? {
        val defaultValueArray =
            context.resources.getStringArray(R.array.preferences_back_confirm_default)
        val defaultValue: Set<String> = HashSet(listOf(*defaultValueArray))
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet(context.getString(R.string.preferences_back_confirm_key), defaultValue)
    }

    private fun getSortingType(context: Context, preferenceKey: String): SortingType {
        val sortingTypeInt = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(preferenceKey, 0)

        return when (sortingTypeInt) {
            3 -> SortingType.ByAlterationDate
            2 -> SortingType.ByCreationDate
            1 -> SortingType.CustomSorting
            else -> SortingType.ByName
        }
    }

    private fun setSortingType(context: Context, preferenceKey: String, sortingType: SortingType) {
        val sortingTypeInt = when (sortingType) {
            SortingType.ByName -> 0
            SortingType.CustomSorting -> 1
            SortingType.ByCreationDate -> 2
            SortingType.ByAlterationDate -> 3
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(preferenceKey, sortingTypeInt).apply()
    }
}