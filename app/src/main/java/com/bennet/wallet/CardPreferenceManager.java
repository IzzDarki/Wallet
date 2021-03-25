package com.bennet.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Map;

import com.bennet.wallet.Utility.PreferenceArrayInt;

public class CardPreferenceManager {
    // preferences
    static public final String CARDS_PREFERENCES_NAME_OLD = "com.bennet.wallet.cards";
    static public final String CARDS_PREFERENCES_NAME_ENCRYPTED = "cards_encrypted";
    static protected final String PREFERENCE_ALL_CARD_IDS = "com.bennet.wallet.home_activity.card_ids"; // PreferenceArrayInt

    static protected final String PREFERENCE_CARD_NAME = "com.bennet.wallet.cards.%d.name"; // String
    static protected final String PREFERENCE_CARD_CODE = "com.bennet.wallet.cards.%d.code"; // String
    static protected final String PREFERENCE_CARD_CODE_TYPE = "com.bennet.wallet.cards.%d.code_type"; // int (see preference values below)
    static protected final String PREFERENCE_CARD_CODE_TYPE_TEXT = "com.bennet.wallet.cards.%d.code_type_text"; // boolean
    static protected final String PREFERENCE_CARD_ID = "com.bennet.wallet.cards.%d.id"; // String
    static protected final String PREFERENCE_CARD_COLOR = "com.bennet.wallet.cards.%d.color"; // @ColorInt int
    static protected final String PREFERENCE_CARD_FRONT_IMAGE = "com.bennet.wallet.cards.%d.front_image_file_path"; // String
    static protected final String PREFERENCE_CARD_BACK_IMAGE = "com.bennet.wallet.cards.%d.back_image_file_path"; // String
    static protected final String PREFERENCE_CARD_PROPERTIES_IDS = "com.bennet.wallet.cards.%d.card_properties_ids"; // String (PreferenceArrayInt)
    static protected final String PREFERENCE_CARD_PROPERTY_NAME =  "com.bennet.wallet.cards.%d.%d.name"; // String
    static protected final String PREFERENCE_CARD_PROPERTY_VALUE = "com.bennet.wallet.cards.%d.%d.value"; // String


    // preference values
    static public final int CARD_CODE_TYPE_AZTEC = 1;
    static public final int CARD_CODE_TYPE_DATA_MATRIX = 2;
    static public final int CARD_CODE_TYPE_PDF_417 = 4;
    static public final int CARD_CODE_TYPE_QR = 5;
    static public final int CARD_CODE_TYPE_CODABAR = 8;
    static public final int CARD_CODE_TYPE_CODE_39 = 9;
    static public final int CARD_CODE_TYPE_CODE_93 = 10;
    static public final int CARD_CODE_TYPE_CODE_128 = 11;
    static public final int CARD_CODE_TYPE_EAN_8 = 12;
    static public final int CARD_CODE_TYPE_EAN_13 = 13;
    static public final int CARD_CODE_TYPE_ITF = 14;
    static public final int CARD_CODE_TYPE_UPC_A = 15;
    static public final int CARD_CODE_TYPE_UPC_E = 16;


    // default preference values
    static public final boolean CARD_CODE_TYPE_TEXT_DEFAULT = false;

    // static variables
    static private SharedPreferences preferences = null;
    static public @ColorInt int cardDefaultColor = Utility.makeRGB(0, 0, 0, 0);


    // init
    /**
     * Initializes this static class if it has not yet been initialized
     * @param context Context to initialize
     */
    static private void init(Context context) {
        if (preferences == null) {
            try {
                MasterKey mainKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                preferences = EncryptedSharedPreferences.create(context, CARDS_PREFERENCES_NAME_ENCRYPTED, mainKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
            encryptOldPreferences(context); // Migrates old preferences (if present) to new encrypted preferences

            cardDefaultColor = context.getResources().getColor(R.color.cardDefaultColor);
        }
    }

    /**
     * If old unencrypted preferences are there, this copies all old preferences to new encrypted ones and deletes old preferences afterwards.
     * CardPreferenceManager::preferences needs to be already set to the new encrypted preferences.
     */
    static private void encryptOldPreferences(Context context) {
        SharedPreferences oldPreferences = context.getSharedPreferences(CARDS_PREFERENCES_NAME_OLD, Context.MODE_PRIVATE);
        Map<String, ?> prefContent = oldPreferences.getAll();
        if (prefContent.size() > 0) {

            SharedPreferences.Editor editor = preferences.edit();

            for (Map.Entry<String, ?> entry : prefContent.entrySet()) {
                if (entry.getValue().getClass() == String.class)
                    editor.putString(entry.getKey(), (String) entry.getValue());

                else if (entry.getValue().getClass() == Integer.class)
                    editor.putInt(entry.getKey(), (Integer) entry.getValue());

                else if (entry.getValue().getClass() == Boolean.class)
                    editor.putBoolean(entry.getKey(), (Boolean) entry.getValue());

                else { // TODO remove or comment debug log
                    if (BuildConfig.DEBUG) {
                        Log.e("encryptOldPref", "Could not encrypt " + entry.getKey() + " (" + entry.getValue().getClass().getName() + ")" + ": " + entry.getValue().toString());
                    }
                }
            }
            editor.apply();

            oldPreferences.edit().clear().apply();
        }

        // deleting the preferences file doesn't really work that well
    }


    // functions
    static public SharedPreferences getPreferences(Context context) {
        if (preferences == null)
            init(context);
        return preferences;
    }

    /**
     * Formats the card property preference with the given id
     * @param ID Id of the card
     * @param cardPropertyPreferenceKey Card property key for preferences
     * @return complete key of the given property with the specific card ID
     */
    static public String getKey(int ID, String cardPropertyPreferenceKey) {
        return String.format(Locale.ENGLISH, cardPropertyPreferenceKey, ID);
    }

    /**
     * Returns preference key of the name of a specific property of a specific card
     * @param ID Id of the card
     * @param propertyID Id of the property
     * @return Preference key of the name of the specific property of the specific card
     */
    static public String getPropertyNameKey(int ID, int propertyID) {
        return String.format(Locale.ENGLISH, PREFERENCE_CARD_PROPERTY_NAME, ID, propertyID);
    }

    /**
     * Returns preference key of the value of a specific property of a specific card
     * @param ID Id of the card
     * @param propertyID Id of the property
     * @return Preference key of the value of the specific property of the specific card
     */
    static public String getPropertyValueKey(int ID, int propertyID) {
        return String.format(Locale.ENGLISH, PREFERENCE_CARD_PROPERTY_VALUE, ID, propertyID);
    }


    // read functions
    /**
     * Reads card name from preferences
     * @param ID Id of the card
     * @return Card name or {@code null} if not found in preferences
     */
    static public String readCardName(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_NAME), null);
    }

    /**
     * Reads card code from preferences
     * @param ID Id of the card
     * @return Card code or empty string as default value if not found in preferences
     */
    static public String readCardCode(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_CODE), "");
    }

    /**
     * Reads card code type from preferences
     * @param ID Id of the card
     * @return Card code type or {@code -1} if not found in preferences, values defined in {@link CardPreferenceManager}, for example {@link #CARD_CODE_TYPE_QR}
     */
    static public int readCardCodeType(Context context, int ID) {
        return getPreferences(context).getInt(getKey(ID, PREFERENCE_CARD_CODE_TYPE), -1);
    }

    /**
     * Reads card code type text from preferences
     * @param ID Id of the card
     * @return Card code type text or {@value #CARD_CODE_TYPE_TEXT_DEFAULT} as default value if not found in preferences
     */
    static public boolean readCardCodeTypeText(Context context, int ID) {
        return getPreferences(context).getBoolean(getKey(ID, PREFERENCE_CARD_CODE_TYPE_TEXT), CARD_CODE_TYPE_TEXT_DEFAULT);
    }

    /**
     * Reads card id from getPreferences(context). The card id is a property if the card. In contrast, the {@code ID} is the id used to identify each card uniquely in getPreferences(context).
     * @param ID Id of the card
     * @return Card id or empty string as default value if not found in preferences
     */
    static public String readCardID(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_ID), "");
    }

    /**
     * Reads card color from preferences
     * @param ID Id of the card
     * @return Card color or {@link #cardDefaultColor} as default value if not found in preferences <br>
     *     Note that {@link #cardDefaultColor} corresponds to {@link com.bennet.wallet.R.color#cardDefaultColor}.
     */
    @ColorInt
    static public int readCardColor(Context context, int ID) {
        return getPreferences(context).getInt(getKey(ID, PREFERENCE_CARD_COLOR), cardDefaultColor);
    }

    /**
     * Reads card front image absolute path from preferences
     * @param ID Id of the card
     * @return Absolute path of card front image or {@code null} if not found in preferences
     */
    static public String readCardFrontImagePath(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_FRONT_IMAGE), null);
    }

    /**
     * Reads card front image as file from preferences
     * @param ID Id of the card
     * @return Card front image file or {@code null} if not found in preferences
     */
    static public File readCardFrontImageFile(Context context, int ID) {
        String filePath = readCardFrontImagePath(context, ID);
        if (filePath != null)
            return new File(filePath);
        else
            return null;
    }

    /**
     * Reads card back image absolute path from preferences
     * @param ID Id of the card
     * @return Absolute path of card back image or {@code null} if not found in preferences
     */
    static public String readCardBackImagePath(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_BACK_IMAGE), null);
    }

    /**
     * Reads card back image as file from preferences
     * @param ID Id of the card
     * @return Card back image file or {@code null} if not found in preferences
     */
    static public File readCardBackImageFile(Context context, int ID) {
        String filePath = readCardBackImagePath(context, ID);
        if (filePath != null)
            return new File(filePath);
        else
            return null;
    }

    /**
     * Reads card property ids from preferences
     * @param ID Id of the card
     * @return String to construct a {@link com.bennet.wallet.Utility.PreferenceArrayInt} of all card property ids or {@code null} if not found in preferences
     * @see #readCardPropertyIds(Context, int)
     */
    static public String readCardPropertyIdsString(Context context, int ID) {
        return getPreferences(context).getString(getKey(ID, PREFERENCE_CARD_PROPERTIES_IDS), null);
    }

    /**
     * Reads card property ids from preferences
     * @param ID Id of the card
     * @return List of all card property ids or {@code null} if not found in preferences
     * @see #readCardPropertyIdsString(Context, int)
     */
    static public PreferenceArrayInt readCardPropertyIds(Context context, int ID) {
        return new PreferenceArrayInt(readCardPropertyIdsString(context, ID));
    }

    /**
     * Reads card property name from preferences
     * @param ID Id of the card
     * @param propertyID ID of the property
     * @return Card property name or {@code null} if not found in preferences
     */
    static public String readCardPropertyName(Context context, int ID, int propertyID) {
        return getPreferences(context).getString(getPropertyNameKey(ID, propertyID), null);
    }

    /**
     * Reads card property value from preferences
     * @param ID Id of the card
     * @param propertyID ID of the property
     * @return Card property value or {@code null} if not found in preferences
     */
    static public String readCardPropertyValue(Context context, int ID, int propertyID) {
        return getPreferences(context).getString(getPropertyValueKey(ID, propertyID), null);
    }


    // write functions
    /**
     * Writes New card name to preferences
     * @param ID Id of the card
     * @param cardName New card name
     */
    static public void writeCardName(Context context, int ID, String cardName) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_NAME), cardName).apply();
    }

    /**
     * Writes new card code to preferences
     * @param ID Id of the card
     * @param cardCode New card code
     */
    static public void writeCardCode(Context context, int ID, String cardCode) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_CODE), cardCode).apply();
    }

    /**
     * Writes new card code type to preferences
     * @param ID Id of the card
     * @param cardCodeType New card code type, values defined in {@link CardPreferenceManager}, for example {@link #CARD_CODE_TYPE_QR}
     */
    static public void writeCardCodeType(Context context, int ID, int cardCodeType) {
        getPreferences(context).edit().putInt(getKey(ID, PREFERENCE_CARD_CODE_TYPE), cardCodeType).apply();
    }

    /**
     * Writes new card code type text to preferences
     * @param ID Id of the card
     * @param cardCodeTypeText New card code type text
     */
    static public void writeCardCodeTypeText(Context context, int ID, boolean cardCodeTypeText) {
        getPreferences(context).edit().putBoolean(getKey(ID, PREFERENCE_CARD_CODE_TYPE_TEXT), cardCodeTypeText).apply();
    }

    /**
     * Writes new card id to getPreferences(context). The card id is a property if the card. In contrast, the {@code ID} is the id used to identify each card uniquely in getPreferences(context).
     * @param ID Id of the card
     * @param cardID New card id
     */
    static public void writeCardID(Context context, int ID, String cardID) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_ID), cardID).apply();
    }

    /**
     * Writes new card color to preferences
     * @param ID Id of the card
     * @param cardColor New card color
     */
    static public void writeCardColor(Context context, int ID, @ColorInt int cardColor) {
        getPreferences(context).edit().putInt(getKey(ID, PREFERENCE_CARD_COLOR), cardColor).apply();
    }

    /**
     * Writes new card front image to preferences
     * @param ID Id of the card
     * @param cardFrontImagePath absolute path of new card front image
     */
    static public void writeCardFrontImage(Context context, int ID, String cardFrontImagePath) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_FRONT_IMAGE), cardFrontImagePath).apply();
    }

    /**
     * Writes new card front image to preferences
     * @param ID Id of the card
     * @param cardFrontImageFile Front image file. If {@code null} front image will be removed
     */
    static public void writeCardFrontImage(Context context, int ID, File cardFrontImageFile) {
        if (cardFrontImageFile != null)
            writeCardFrontImage(context, ID, cardFrontImageFile.getAbsolutePath());
        else
            removeCardFrontImage(context, ID);
    }

    /**
     * Writes new card back image to preferences
     * @param ID Id of the card
     * @param cardBackImagePath absolute path of new card back image
     */
    static public void writeCardBackImage(Context context, int ID, String cardBackImagePath) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_BACK_IMAGE), cardBackImagePath).apply();
    }

    /**
     * Writes new card back image to preferences
     * @param ID Id of the card
     * @param cardBackImageFile Back image file. If {@code null} back image will be removed
     */
    static public void writeCardBackImage(Context context, int ID, File cardBackImageFile) {
        if (cardBackImageFile != null)
            writeCardBackImage(context, ID, cardBackImageFile.getAbsolutePath());
        else
            removeCardBackImage(context, ID);
    }

    /**
     * Writes new list of all card property ids to preferences
     * @param ID Id of the card
     * @param cardPropertyIds New list of all card property ids
     */
    static public void writeCardPropertyIds(Context context, int ID, @Nullable PreferenceArrayInt cardPropertyIds) {
        getPreferences(context).edit().putString(getKey(ID, PREFERENCE_CARD_PROPERTIES_IDS), PreferenceArrayInt.toPreference(cardPropertyIds)).apply();
    }

    /**
     * Writes New card property name to preferences
     * @param ID Id of the card
     * @param propertyID Id of the property
     * @param cardPropertyName New name of card property
     */
    static public void writeCardPropertyName(Context context, int ID, int propertyID, String cardPropertyName) {
        getPreferences(context).edit().putString(getPropertyNameKey(ID, propertyID), cardPropertyName).apply();
    }

    /**
     * Writes New card property value to preferences
     * @param ID Id of the card
     * @param propertyID Id of the property
     * @param cardPropertyValue New value of card property
     */
    static public void writeCardPropertyValue(Context context, int ID, int propertyID, String cardPropertyValue) {
        getPreferences(context).edit().putString(getPropertyValueKey(ID, propertyID), cardPropertyValue).apply();
    }


    // remove functions
    /**
     * Removes card name from preferences
     * @param ID Id of the card
     */
    static public void removeCardName(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_NAME)).apply();
    }

    /**
     * Removes card code from preferences
     * @param ID Id of the card
     */
    static public void removeCardCode(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_CODE)).apply();
    }

    /**
     * Removes card code type from preferences
     * @param ID Id of the card
     */
    static public void removeCardCodeType(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_CODE_TYPE)).apply();
    }

    /**
     * Removes card code type text from preferences
     * @param ID Id of the card
     */
    static public void removeCardCodeTypeText(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_CODE_TYPE_TEXT)).apply();
    }

    /**
     * Removes card id from getPreferences(context). The card id is a property if the card. In contrast, the {@code ID} is the id used to identify each card uniquely in getPreferences(context).
     * @param ID Id of the card
     */
    static public void removeCardID(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_ID)).apply();
    }

    /**
     * Removes card color from preferences
     * @param ID Id of the card
     */
    static public void removeCardColor(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_COLOR)).apply();
    }

    /**
     * Removes card front image from getPreferences(context). Does <b>not</b> delete the file <br>
     *     Use {@link #deleteCardFrontImage(Context, int)} instead if you want to delete front image file and remove it's preferences
     * @param ID Id of the card
     */
    static public void removeCardFrontImage(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_FRONT_IMAGE)).apply();
    }

    /**
     * Removes card back image from getPreferences(context). Does <b>not</b> delete the file <br>
     *     Use {@link #deleteCardBackImage(Context, int)} instead if you want to delete back image file and remove it's preferences
     * @param ID Id of the card
     */
    static public void removeCardBackImage(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_BACK_IMAGE)).apply();
    }

    /**
     * Removes card property ids list from preferences
     * @param ID Id of the card
     */
    static public void removeCardPropertyIds(Context context, int ID) {
        getPreferences(context).edit().remove(getKey(ID, PREFERENCE_CARD_PROPERTIES_IDS)).apply();
    }

    /**
     * Removes card property name from preferences
     * @param ID Id of the card
     * @param propertyID Id of the property
     */
    static public void removeCardPropertyName(Context context, int ID, int propertyID) {
        getPreferences(context).edit().remove(getPropertyNameKey(ID, propertyID)).apply();
    }

    /**
     * Removes card property value from preferences
     * @param ID Id of the card
     * @param propertyID Id of the property
     */
    static public void removeCardPropertyValue(Context context, int ID, int propertyID) {
        getPreferences(context).edit().remove(getPropertyValueKey(ID, propertyID)).apply();
    }


    // extended remove functions
    /**
     * Deletes the front image, that is currently stored in preferences and also removes it from preferences <br>
     *     Use {@link #removeCardFrontImage(Context, int)} instead if you want to remove the front image from preferences without deleting it's file
     * @param ID Id of the card
     */
    static public void deleteCardFrontImage(Context context, int ID) {
        File image = readCardFrontImageFile(context, ID);
        if (image != null) {
            if (!image.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardPreferenceManager", "Couldn't delete front image");
                 */
            }
        }
        removeCardFrontImage(context, ID);
    }

    /**
     * Deletes the back image, that is currently stored in preferences and also removes it from preferences <br>
     *     Use {@link #removeCardBackImage(Context, int)} instead if you want to remove the back image from preferences without deleting it's file
     * @param ID Id of the card
     */
    static public void deleteCardBackImage(Context context, int ID) {
        File image = readCardBackImageFile(context, ID);
        if (image != null) {
            if (!image.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardPreferenceManager", "Couldn't delete back image");
                 */
            }
        }
        removeCardBackImage(context, ID);
    }

    /**
     * Removes card property from preferences
     * @param ID ID of the card
     * @param propertyID Id of the property
     */
    static public void removeCardProperty(Context context, int ID, int propertyID) {
        removeCardPropertyName(context, ID, propertyID);
        removeCardPropertyValue(context, ID, propertyID);
    }

    /**
     * Removes all card properties that are currently stored in preferences and also removes the property ids list itself from preferences
     * @param ID Id of the card
     */
    static public void removeCardProperties(Context context, int ID) {
        PreferenceArrayInt propertyIds = readCardPropertyIds(context, ID);
        for (Integer propertyID : propertyIds)
            removeCardProperty(context, ID, propertyID);
        removeCardPropertyIds(context, ID);
    }

    /**
     * Removes all getPreferences(context). Also removes card id from all card ids list. Does <b>not</b> delete the file <br>
     *     Use {@link #deleteCard(Context, int)} instead if you also want to delete image files
     * @param ID Id of the card
     */
    static public void removeCard(Context context, int ID) {
        removeCardName(context, ID);
        removeCardCode(context, ID);
        removeCardCodeType(context, ID);
        removeCardCodeTypeText(context, ID);
        removeCardID(context, ID);
        removeCardColor(context, ID);
        removeCardFrontImage(context, ID);
        removeCardBackImage(context, ID);
        removeCardProperties(context, ID);
        removeFromAllCardIDs(context, ID);
    }

    /**
     * Removes all getPreferences(context). Also deletes image files. Also removes card id from all card ids list<br>
     *     Use {@link #removeCard(Context, int)} instead if you want to remove preferences without deleting image files
     * @param ID Id of the card
     */
    static public void deleteCard(Context context, int ID) {
        removeCardName(context, ID);
        removeCardCode(context, ID);
        removeCardCodeType(context, ID);
        removeCardCodeTypeText(context, ID);
        removeCardID(context, ID);
        removeCardColor(context, ID);
        deleteCardFrontImage(context, ID);
        deleteCardBackImage(context, ID);
        removeCardProperties(context, ID);
        removeFromAllCardIDs(context, ID);
    }


    // check functions
    /**
     * Checks if preferences contain a certain card property
     * @param ID Id of he card
     * @param cardPropertyPreferenceKey Preference key of the property to look for
     * @return {@code true} if preferences contain the card property, {@code false} otherwise
     */
    static public boolean contains(Context context, int ID, String cardPropertyPreferenceKey) {
        return getPreferences(context).contains(String.format(Locale.ENGLISH, cardPropertyPreferenceKey, ID));
    }


    // read and write all card ids
    /**
     * Reads list of all card ids from preferences
     * @return List of all card ids
     */
    static public PreferenceArrayInt readAllCardIDs(Context context) {
        return new PreferenceArrayInt(getPreferences(context).getString(PREFERENCE_ALL_CARD_IDS, null));
    }

    /**
     * Overwrites list of all card ids on preferences
     * @param allCardIDs New list of all card ids
     */
    static public void writeAllCardIDs(Context context, PreferenceArrayInt allCardIDs) {
        getPreferences(context).edit().putString(PREFERENCE_ALL_CARD_IDS, PreferenceArrayInt.toPreference(allCardIDs)).apply();
    }

    /**
     * Adds {@code ID} to list of all card ids in preferences <br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case {@code ID} would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all card ids is edited in that moment
     * @param ID Id of the card
     */
    static public void addToAllCardIDs(Context context, int ID) {
        PreferenceArrayInt cardIDs = readAllCardIDs(context);
        if (!cardIDs.contains(ID))
            cardIDs.add(ID);
        writeAllCardIDs(context, cardIDs);
    }

    /**
     * Removes {@code ID} from list of all card ids in preferences <br>
     *     Note that this reads the list from preferences first and would not recognize if somewhere else the list was read from preferences
     *     and could possibly write the same list again to preferences in which case {@code ID} would still be on the list.
     *     Only use this function if you are sure, that nowhere else the list of all card ids is edited in that moment
     * @param ID Id of the card
     */
    static public void removeFromAllCardIDs(Context context, int ID) {
        PreferenceArrayInt cardIDs = readAllCardIDs(context);
        cardIDs.remove((Integer) ID); // no need to check if cardIDs contains ID
        writeAllCardIDs(context, cardIDs);
    }
}
