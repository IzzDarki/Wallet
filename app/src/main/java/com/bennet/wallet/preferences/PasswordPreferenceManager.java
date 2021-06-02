package com.bennet.wallet.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

import com.bennet.wallet.utils.Utility;
import com.bennet.wallet.utils.Utility.PreferenceArrayInt;

public class PasswordPreferenceManager {
    // preferences
    static public final String PASSWORDS_PREFERENCES_NAME = "passwords";

    static public final String PREFERENCE_ALL_PASSWORD_IDS = "password_ids";

    static public final String PREFERENCE_PASSWORD_NAME = "%d.name"; // String
    static public final String PREFERENCE_PASSWORD_ACCOUNT = "%d.account"; // String
    static public final String PREFERENCE_PASSWORD_PASSWORD = "%d.password"; // String
    static public final String PREFERENCE_PASSWORD_PROPERTIES_IDS = "%d.password_properties"; // String (PreferenceArrayInt)
    static public final String PREFERENCE_PASSWORD_PROPERTY_NAME = "%d.%d.property_name"; // String
    static public final String PREFERENCE_PASSWORD_PROPERTY_VALUE = "%d.%d.property_value"; // String
    static public final String PREFERENCE_PASSWORD_PROPERTY_HIDDEN = "%d.%d.property_hidden"; // boolean

    // static variables
    static protected SharedPreferences preferences;

    // init
    /**
     * Initializes this static class if it has not yet been initialized
     * @param context Context to initialize
     */
    static public void initOnce(Context context) {
        if (preferences == null) {
            try {
                MasterKey mainKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                preferences = EncryptedSharedPreferences.create(context, PASSWORDS_PREFERENCES_NAME, mainKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }


    // functions
    static public SharedPreferences getPreferences(Context context) {
        initOnce(context);
        return preferences;
    }

    /**
     * Returns the preference key of the password property with the given id
     * @param ID Id of the password
     * @param passwordPropertyPreferenceKey Password property key for preferences
     * @return Preference key of the given property with the specific card ID
     */
    static public String getKey(int ID, String passwordPropertyPreferenceKey) {
        return String.format(Locale.ENGLISH, passwordPropertyPreferenceKey, ID);
    }

    /**
     * Returns the preference key of the name of a specific property of a specific password
     * @param ID Id of the password
     * @param propertyID Id of the property
     * @return Preference key of the name of the specific property of the specific password
     */
    static public String getPropertyNameKey(int ID, int propertyID) {
        return String.format(Locale.ENGLISH, PREFERENCE_PASSWORD_PROPERTY_NAME, ID, propertyID);
    }

    /**
     * Returns the preference key of the value of a specific property of a specific password
     * @param ID Id of the password
     * @param propertyID Id of the property
     * @return Preference key of the value of the specific property of the specific password
     */
    static public String getPropertyValueKey(int ID, int propertyID) {
        return String.format(Locale.ENGLISH, PREFERENCE_PASSWORD_PROPERTY_VALUE, ID, propertyID);
    }

    static public String getPropertyHiddenKey(int ID, int propertyID) {
        return String.format(Locale.ENGLISH, PREFERENCE_PASSWORD_PROPERTY_HIDDEN, ID, propertyID);
    }

    // read functions
    /**
     * Reads password name from preferences
     * @param ID Id of the password
     * @return Password name or {@code null} if not found in preferences
     */
    static public String readPasswordName(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_NAME), null);
    }

    /**
     * Reads the password of a stored password from preferences
     * @param ID Id of the password
     * @return Password of the stored password or empty string if not found in preferences
     */
    static public String readPasswordPassword(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_PASSWORD), "");
    }

    /**
     * Reads password property ids from preferences
     * @param ID Id of the password
     * @return String to construct a {@link Utility.PreferenceArrayInt} of all password property ids or {@code null} if not found in preferences
     * @see #readPasswordPropertyIds(Context, int)
     */
    static public String readPasswordPropertyIdsString(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_PASSWORD_PROPERTIES_IDS), null);
    }

    /**
     * Reads passwords property ids from preferences
     * @param ID Id of the passwords
     * @return List of all passwords property ids or {@code null} if not found in preferences
     * @see #readPasswordPropertyIdsString(Context, int)
     */
    static public PreferenceArrayInt readPasswordPropertyIds(Context context, int ID) {
        return new PreferenceArrayInt(readPasswordPropertyIdsString(context, ID));
    }


    /**
     * Reads password property name from preferences
     * @param ID Id of the password
     * @param propertyID ID of the property
     * @return Password property name or {@code null} if not found in preferences
     */
    static public String readPasswordPropertyName(Context context, int ID, int propertyID) {
        return getPreferences(context).getString(getPropertyNameKey(ID, propertyID), null);
    }

    /**
     * Reads password property value from preferences
     * @param ID Id of the password
     * @param propertyID ID of the property
     * @return Password property value or {@code null} if not found in preferences
     */
    static public String readPasswordPropertyValue(Context context, int ID, int propertyID) {
        return getPreferences(context).getString(getPropertyValueKey(ID, propertyID), null);
    }

    /**
     * Reads if password property should be displayed as hidden
     * @param ID Id of the password
     * @param propertyID ID of the property
     * @return true if the password property should be displayed as hidden, false otherwise. Also true if not found in preferences
     */
    static public boolean readPasswordPropertyHidden(Context context, int ID, int propertyID) {
        return getPreferences(context).getBoolean(getPropertyHiddenKey(ID, propertyID), true);
    }


    // write functions

    /**
     * Writes new password name to preferences
     * @param ID Id of the password
     * @param passwordName New password name
     */
    static public void writePasswordName(Context context, int ID, String passwordName) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_PASSWORD_NAME), passwordName).apply();
    }

    /**
     * Writes new list of all password property ids to preferences
     * @param ID Id of the password
     * @param passwordPropertyIds New list of all password property ids
     */
    static public void writePasswordPropertyIds(Context context, int ID, @Nullable PreferenceArrayInt passwordPropertyIds) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_PASSWORD_PROPERTIES_IDS), PreferenceArrayInt.toPreference(passwordPropertyIds)).apply();
    }

    /**
     * Writes new password property name to preferences
     * @param ID Id of the card
     * @param propertyID Id of the property
     * @param passwordPropertyName New name of password property
     */
    static public void writePasswordPropertyName(Context context, int ID, int propertyID, String passwordPropertyName) {
        getPreferences(context).edit().putString(getPropertyNameKey(ID, propertyID), passwordPropertyName).apply();
    }

    /**
     * Writes new password property value to preferences
     * @param ID Id of the password
     * @param propertyID Id of the property
     * @param passwordPropertyValue New value of password property
     */
    static public void writePasswordPropertyValue(Context context, int ID, int propertyID, String passwordPropertyValue) {
        getPreferences(context).edit().putString(getPropertyValueKey(ID, propertyID), passwordPropertyValue).apply();
    }

    /**
     * Writes new value for displaying a password property as hidden to preferences
     * @param ID Id of the password
     * @param propertyID Id of the property
     * @param passwordPropertyHidden true if password should be displayed as hidden, false otherwise
     */
    static public void writePasswordPropertyHidden(Context context, int ID, int propertyID, boolean passwordPropertyHidden) {
        getPreferences(context).edit().putBoolean(getPropertyHiddenKey(ID, propertyID), passwordPropertyHidden).apply();
    }


    // remove functions
    /**
     * Removes password name from preferences
     * @param ID Id of the password
     */
    static public void removePasswordName(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_NAME)).apply();
    }

    /**
     * Removes password property ids list from preferences
     * @param ID Id of the password
     */
    static public void removePasswordPropertyIds(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_PASSWORD_PROPERTIES_IDS)).apply();
    }

    /**
     * Removes password property name from preferences
     * @param ID Id of the password
     * @param propertyID Id of the property
     */
    static public void removePasswordPropertyName(Context context, int ID, int propertyID) {
        getPreferences(context).edit().remove(getPropertyNameKey(ID, propertyID)).apply();
    }

    /**
     * Removes password property value from preferences
     * @param ID Id of the password
     * @param propertyID Id of the property
     */
    static public void removePasswordPropertyValue(Context context, int ID, int propertyID) {
        getPreferences(context).edit().remove(getPropertyValueKey(ID, propertyID)).apply();
    }

    /**
     * Removes value for displaying a password property as hidden from preferences
     * @param ID Id of the password
     * @param propertyID Id of the property
     */
    static public void removePasswordPropertyHidden(Context context, int ID, int propertyID) {
        getPreferences(context).edit().remove(getPropertyHiddenKey(ID, propertyID)).apply();
    }

    // combined remove functions
    /**
     * Removes password property from preferences
     * @param ID Id of the password
     * @param propertyID Id of the property
     */
    static public void removePasswordProperty(Context context, int ID, int propertyID) {
        removePasswordPropertyName(context, ID, propertyID);
        removePasswordPropertyValue(context, ID, propertyID);
        removePasswordPropertyHidden(context, ID, propertyID);
    }

    /**
     * Removes all password properties that are currently stored in preferences and also removes the property ids list itself from preferences
     * @param ID Id of the password
     */
    static public void removePasswordProperties(Context context, int ID) {
        PreferenceArrayInt propertyIds = readPasswordPropertyIds(context, ID);
        for (Integer propertyID : propertyIds)
            removePasswordProperty(context, ID, propertyID);
        removePasswordPropertyIds(context, ID);
    }

    /**
     * Removes all preferences of a password. Also removes password id from all card ids list.
     * @param ID Id of the password
     */
    static public void removePassword(Context context, int ID) {
        removePasswordName(context, ID);
        removePasswordProperties(context, ID);
        removeFromAllPasswordIDs(context, ID);
    }


    // read and write all password ids
    /**
     * Reads list of all password ids from preferences
     * @return List of all password ids
     */
    static public PreferenceArrayInt readAllPasswordIDs(Context context) {
        return new PreferenceArrayInt(getPreferences(context).getString(PREFERENCE_ALL_PASSWORD_IDS, null));
    }

    /**
     * Overwrites list of all password ids on preferences
     * @param allPasswordIDs New list of all password ids
     */
    static public void writeAllPasswordIDs(Context context, PreferenceArrayInt allPasswordIDs) {
        getPreferences(context).edit().putString(PREFERENCE_ALL_PASSWORD_IDS, Utility.PreferenceArrayInt.toPreference(allPasswordIDs)).apply();
    }


    /**
     * Adds {@code ID} to list of all passwords ids in preferences <br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case {@code ID} would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all card ids is edited in that moment
     * @param ID Id of the password
     */
    static public void addToAllPasswordIDs(Context context, int ID) {
        PreferenceArrayInt passwordIDs = readAllPasswordIDs(context);
        if (!passwordIDs.contains(ID))
            passwordIDs.add(ID);
        writeAllPasswordIDs(context, passwordIDs);
    }

    /**
     * Removes {@code ID} from list of all passwords ids in preferences <br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case {@code ID} would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all card ids is edited in that moment
     * @param ID Id of the password
     */
    static public void removeFromAllPasswordIDs(Context context, int ID) {
        PreferenceArrayInt passwordIDs = readAllPasswordIDs(context);
        passwordIDs.remove((Integer) ID); // no need to check if cardIDs contains ID
        writeAllPasswordIDs(context, passwordIDs);
    }

}
