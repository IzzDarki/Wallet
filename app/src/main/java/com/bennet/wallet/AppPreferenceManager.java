package com.bennet.wallet;

import android.content.Context;

import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This static class is used for reading the app preferences
 */
public class AppPreferenceManager {

    static protected Set<String> getBackConfirmStringSet(Context context) {
        String[] defaultValueArray = context.getResources().getStringArray(R.array.preferences_back_confirm_default);
        Set<String> defaultValue = new HashSet<>(Arrays.asList(defaultValueArray));
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(context.getString(R.string.preferences_back_confirm_key), defaultValue);
    }

    static public boolean isBackConfirmNewCard(Context context) {
        Set<String> stringSet = getBackConfirmStringSet(context);
        return stringSet.contains(context.getString(R.string.preferences_back_confirm_entry_values_new_card));
    }

    static public boolean isBackConfirmCrop(Context context) {
        Set<String> stringSet = getBackConfirmStringSet(context);
        return stringSet.contains(context.getString(R.string.preferences_back_confirm_entry_values_crop));
    }

    static public int getDefaultCardCodeType(Context context) {
        String defaultTypeString = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.preferences_default_values_barcode_key), context.getString(R.string.preferences_default_values_barcode_default));
        return CardActivity.codeTypeStringToInt(context, defaultTypeString);
    }

    static public boolean getDefaultWithText(Context context) {
        String defaultTextString = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.preferences_default_values_code_text_key), context.getString(R.string.preferences_default_values_code_text_default));
        return defaultTextString.equals(context.getString(R.string.preferences_default_values_code_text_entry_values_with_text));
    }

    /**
     * Checks in the user preferences, whether the user wants to receive detailed error messages or not
     * @param context Context
     * @return Returns true when detailed error messages are turned on, otherwise false
     */
    static public boolean isDetailedErrors(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.preferences_detailed_errors_key), context.getResources().getBoolean(R.bool.preferences_detailed_errors_default));
    }
}
