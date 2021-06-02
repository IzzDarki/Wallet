package com.bennet.wallet.activities.cards;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bennet.colorpickerview.dialog.ColorPickerDialogFragment;
import com.bennet.wallet.activities.CodeScannerActivity;
import com.bennet.wallet.activities.GetContentImageActivity;
import com.bennet.wallet.activities.GetImageActivity;
import com.bennet.wallet.activities.HomeActivity;
import com.bennet.wallet.activities.ImageCaptureActivity;
import com.bennet.wallet.preferences.AppPreferenceManager;
import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.utils.CardPropertyView;
import com.bennet.wallet.services.CreateExampleCardService;
import com.bennet.wallet.R;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.bennet.wallet.utils.Utility.PreferenceArrayInt;

public class EditCardActivity extends CardActivity implements ColorPickerDialogFragment.ColorPickerDialogListener {
    // intent extras
    static public final String EXTRA_CREATE_NEW_CARD = "com.bennet.wallet.edit_card_activity.create_new_card";

    // app intents
    static public final int REQUEST_IMAGE_CAPTURE_FRONT = 1;
    static public final int REQUEST_IMAGE_CAPTURE_BACK = 2;
    static public final int REQUEST_GET_CONTENT_FRONT = 3;
    static public final int REQUEST_GET_CONTENT_BACK = 4;
    static public final int REQUEST_QR_CODE_SCAN = 5;

    // color picker ids
    static protected final int PICK_COLOR_ID = 0;

    // final variables
    protected final CardPropertyView.OnSetPropertyDeletedListener onSetPropertyDeletedListener = (view, isDeleted) -> {
        if (isDeleted) {
            hasBeenModified = true;
            cardPropertyIDs.remove((Integer) view.getPropertyID());
            linearLayout.removeView(view);

            if (view.getImeOptions() == EditorInfo.IME_ACTION_DONE) { // if the property that gets removed was the last visible one
                // set the property view at the end of the visible list to IME done
                CardPropertyView lastPropertyView = getLastVisiblePropertyView();
                if (lastPropertyView != null)
                    lastPropertyView.setImeDone();
                else
                    cardIDInputSetImeOptions(EditorInfo.IME_ACTION_DONE);
            }
            if (cardView != null) {
                cardView.post(() -> {
                    cardView.drawAnimation(); // refresh because position has changed (view above was removed)
                });
            }
        }
    };

    // UI
    protected TextInputEditText cardNameInput;
    protected TextInputLayout cardCodeInputLayout;
    protected TextInputEditText cardCodeInput;
    protected LinearLayout cardCodeTypeAndShowLayout;
    protected MaterialAutoCompleteTextView cardCodeTypeInput;
    protected MaterialAutoCompleteTextView cardCodeTypeTextInput;
    protected TextInputEditText cardIDInput;
    protected LinearLayout createNewCardPropertyButton;
    protected MaterialButton cardColorButton;
    protected MaterialButton cardFrontImageButton;
    protected MaterialButton cardBackImageButton;

    // variables
    protected File lastFrontImage;
    protected File lastBackImage;
    /** Contains all card properties as {@link CardPropertyView} (also the ones that are not visible because the user has pressed their delete buttons) */
    protected List<CardPropertyView> cardPropertyViews = new ArrayList<>();
    private boolean isCreateNewCardIntent;
    private boolean isMahlerCardInit = false;
    private boolean hasBeenModified = false;

    // lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isCreateNewCardIntent = getIntent().getBooleanExtra(EXTRA_CREATE_NEW_CARD, false);

        setContentView(R.layout.activity_edit_card);

        // hooks
        cardViewLayout = findViewById(R.id.edit_card_card_view_layout);
        scrollView = findViewById(R.id.edit_card_scroll_view);
        linearLayout = findViewById(R.id.edit_card_linear_layout);
        cardNameInput = findViewById(R.id.edit_card_name_input);
        cardCodeInputLayout = findViewById(R.id.edit_card_code_layout);
        cardCodeInput = findViewById(R.id.edit_card_code_input);
        cardCodeTypeAndShowLayout = findViewById(R.id.edit_card_code_type_and_show_layout);
        cardCodeTypeInput = findViewById(R.id.edit_card_code_type_input);
        cardCodeTypeTextInput = findViewById(R.id.edit_card_code_type_text_input);
        cardIDInput = findViewById(R.id.edit_card_id_input);
        createNewCardPropertyButton = findViewById(R.id.edit_card_create_new_card_property_button);
        cardColorButton = findViewById(R.id.edit_card_color_button);
        cardFrontImageButton = findViewById(R.id.edit_card_front_image_button);
        cardBackImageButton = findViewById(R.id.edit_card_back_image_button);

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.edit_card);

        // init
        if (isCreateNewCardIntent)
            initNewCard(); // create new card
        else {
            initFromPreferences(); // load existing card
            lastFrontImage = currentFrontImage;
            lastBackImage = currentBackImage;
            /* explanation
                last images are the images, that are currently saved preferences, they are never being displayed
                current images are the images, that are not saved in preferences, they are currently being displayed
                if EditCardActivity "save"s, the last images get deleted and new ones get saved in preferences
                if EditCardActivity "cancel"s, the current images get deleted and the preferences remain unchanged
                if the images remain unchanged, last and current images are the same, then nothing gets deleted and preferences remain unchanged (scenario same for both "cancel" and "save")
                 */
        }

        // scroll view
        hideScrollbar();

        // card name
        cardNameInput.setText(cardName);
        cardNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                readNameInput();
                cardNameInput.setText(cardName);
            }
        });

        // card code
        cardCodeInput.setText(cardCode);
        cardCodeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                readCodeInput();
                cardCodeInput.setText(cardCode);
            }
        });
        cardCodeInput.addTextChangedListener(new TextWatcher() { // hide or show text fields for card code type and card code text type
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(""))
                    hideCardCodeTypeAndShowLayout();
                else
                    showCardCodeTypeAndShowLayout();
            }
        });
        cardCodeInputLayout.setEndIconOnClickListener(v -> scanQRCode());


        // card code type
        if (cardCode.equals(""))
            hideCardCodeTypeAndShowLayout();
        cardCodeTypeInput.setText(codeTypeIntToString(this, cardCodeType));
        cardCodeTypeInput.setAdapter(getNewCardCodeTypeAdapter());
        cardCodeTypeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                readCodeTypeInput();
        });
        cardCodeTypeInput.setWidth((int) ((getCalculatedLayoutWidth() - Utility.DPtoPX(getResources().getDisplayMetrics(), 8)) / 2));

        // card code text type
        cardCodeTypeTextInput.setText(codeTypeTextBoolToString(cardCodeTypeText));
        cardCodeTypeTextInput.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_meu_popup_item, new String[]{getString(R.string.card_code_type_text_value_show_text), getString(R.string.card_code_type_text_value_dont_show_text)}));
        cardCodeTypeTextInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                readCodeTypeTextInput();
        });
        cardCodeTypeTextInput.setWidth((int) ((getCalculatedLayoutWidth() - Utility.DPtoPX(getResources().getDisplayMetrics(), 8)) / 2));

        // card ID
        cardIDInput.setText(cardID);
        cardIDInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                readIDInput();
                cardIDInput.setText(cardID);
            }
        });

        // create new card property button
        createNewCardPropertyButton.setOnClickListener(v -> createNewCardProperty());

        // card color
        cardColorButton.setOnClickListener(v -> pickColor());
        updateColorButtonColor();

        // card images
        cardFrontImageButton.setOnClickListener(v -> chooseImage(true));
        cardBackImageButton.setOnClickListener(v -> chooseImage(false));

        // card properties
        for (int property_ID : cardPropertyIDs) {
            CardPropertyView propertyView = CardPropertyView.loadFromPreferences(this, ID, property_ID, null, onSetPropertyDeletedListener);
            addPropertyView(propertyView);
        }

        // card view
        createCardView();
        cardView.setFrontText(getString(R.string.front_image));
        cardView.setBackText(getString(R.string.back_image));
    }


    // activity results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE_FRONT:
                onImageCaptureResult(resultCode, data, true);
                break;

            case REQUEST_IMAGE_CAPTURE_BACK:
                onImageCaptureResult(resultCode, data, false);
                break;

            case REQUEST_GET_CONTENT_FRONT:
                onGetContentResult(resultCode, data, true);
                break;

            case REQUEST_GET_CONTENT_BACK:
                onGetContentResult(resultCode, data, false);
                break;

            case REQUEST_QR_CODE_SCAN:
                onScanQRCodeResult(resultCode, data);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    protected void onImageCaptureResult(int resultCode, @Nullable Intent data, boolean isFront) {
        switch (resultCode) {
            case RESULT_OK:
                setCardImage(isFront, (File)data.getSerializableExtra(ImageCaptureActivity.EXTRA_RESULT_FILE));
                break;

            case ImageCaptureActivity.RESULT_NO_IMAGE_CAPTURE_INTENT:
                Toast.makeText(this, R.string.no_image_capture_intent, Toast.LENGTH_SHORT).show();
                break;

            case ImageCaptureActivity.RESULT_PERMISSION_DENIED:
                Toast.makeText(this, R.string.image_capture_camera_permission_denied, Toast.LENGTH_SHORT).show();
                break;

            case RESULT_CANCELED:
                break;

            default:
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    protected void onGetContentResult(int resultCode, @Nullable Intent data, boolean isFront) {
        switch (resultCode) {
            case RESULT_OK:
                setCardImage(isFront, (File)data.getSerializableExtra(GetContentImageActivity.EXTRA_RESULT_FILE));
                break;

            case GetContentImageActivity.RESULT_NO_GET_CONTENT_INTENT:
                Toast.makeText(this, R.string.no_get_content_intent, Toast.LENGTH_SHORT).show();
                break;

            case RESULT_CANCELED:
                break;

            default:
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    protected void onScanQRCodeResult(int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            BarcodeFormat barcodeFormat = (BarcodeFormat) data.getSerializableExtra(CodeScannerActivity.EXTRA_RESULT_CODE_TYPE);

            try {
                cardCodeType = cardCodeBarcodeFormatToInt(barcodeFormat);
            } catch (IllegalStateException e){
                Toast.makeText(this, R.string.unsupported_type_barcode, Toast.LENGTH_LONG).show();
                return;
            }

            cardCode = data.getStringExtra(CodeScannerActivity.EXTRA_RESULT_CODE);
            cardCodeInput.setText(cardCode);
            cardCodeTypeInput.setText(codeTypeIntToString(this, cardCodeType));
            cardCodeTypeInput.setAdapter(getNewCardCodeTypeAdapter()); // reset the adapter (otherwise there are filters)
        }
        else if (resultCode == CodeScannerActivity.RESULT_PERMISSION_DENIED)
            Toast.makeText(this, R.string.code_scanner_camera_permission_denied, Toast.LENGTH_SHORT).show();
    }


    // handling action bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_card_action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.edit_card_action_bar_done) {  // finish if input can be saved (no input errors)
            save();
            return true;
        } else if (itemId == R.id.edit_card_action_bar_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
            builder.setTitle(R.string.delete_card);
            builder.setMessage(R.string.delete_card_dialog_message);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                deleteCardAndFinish();
                dialog.dismiss();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        requestCancel();
        return true;
    }


    // behavior
    @Override
    public void onBackPressed() { // return to ShowCardActivity without any change of the preferences
       requestCancel();
    }

    public void finishAndShowCard() {
        hideKeyboard(); // TODO remove this, if it didn't fix the "random" weird sizing issue
        finish();
        Intent intent = new Intent(this, ShowCardActivity.class);
        intent.putExtra(EXTRA_CARD_ID, ID);
        startActivity(intent);
    }

    public void finishAndReturnToHome() {
        finish();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // will clear back stack until instance of HomeActivity
        intent.putExtra(EXTRA_CARD_ID, ID);
        startActivity(intent);
    }


    // functions
    protected void save() {
        if (checkInputAndSavePreferences())
            finishAndShowCard();
    }

    /**
     * Either cancels or shows an alert box to cancel
     * Should be called when user cancels card editing
     *
     * <br>Detail:
     * Alert dialog is shown, when
     * a new card gets canceled and the according setting is enabled
     * or when
     * editing an existing card gets canceled, the card has been modified the according setting is enabled
     */
    protected void requestCancel() {
        checkAndReadInput(); // Read input to figure out of card has been modified (member variable hasBeenModified)
        if (AppPreferenceManager.isBackConfirmNewCard(this) && isCreateNewCardIntent || AppPreferenceManager.isBackConfirmEditCard(this) && !isCreateNewCardIntent && hasBeenModified) {
            String dialogTitle;
            String dialogMessage;
            String dialogSaveInstead;
            if (isCreateNewCardIntent) {
                dialogTitle = getString(R.string.cancel_create_new_card_title);
                dialogMessage = getString(R.string.cancel_create_new_card_message);
            }
            else {
                dialogTitle = getString(R.string.cancel_edit_card_title);
                dialogMessage = getString(R.string.cancel_edit_card_message);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
            builder.setTitle(dialogTitle);
            builder.setMessage(dialogMessage);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.discard, (dialog, which) -> {
                cancelDirectly();
                dialog.dismiss();
            });
            builder.setNeutralButton(R.string.save, (dialog, which) -> {
               save();
               dialog.dismiss();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
            builder.show();
        }
        else
            cancelDirectly();
    }

    /**
     * Helper for {@link #requestCancel()}
     */
    protected void cancelDirectly() {
        if (currentFrontImage != lastFrontImage)
            deleteFrontImage();
        if (currentBackImage != lastBackImage)
            deleteBackImage();

        if (getIntent().getBooleanExtra(EXTRA_CREATE_NEW_CARD, false)) {
            setResult(RESULT_CANCELED);
            finish(); // return to calling activity (do not launch ShowCardActivity)
        }
        else
            finishAndShowCard();
    }

    protected void chooseImage(final boolean isFront) {
        hideKeyboard();
        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));

        final View dialogLayout = View.inflate(this, R.layout.image_chooser_dialog, null);
        final ViewGroup deleteImageGroup = dialogLayout.findViewById(R.id.image_chooser_dialog_delete);
        final ViewGroup selectImageGroup = dialogLayout.findViewById(R.id.image_chooser_dialog_storage);
        final ViewGroup imageCaptureGroup = dialogLayout.findViewById(R.id.image_chooser_dialog_camera);


        builder.setView(dialogLayout);
        dialog = builder.create();

        deleteImageGroup.setOnClickListener(v -> {
            dialog.dismiss();
            if (isFront)
                removeFrontImage();
            else
                removeBackImage();
        });

        selectImageGroup.setOnClickListener(v -> {
            dialog.dismiss();
            selectImage(isFront);
        });

        imageCaptureGroup.setOnClickListener(v -> {
            dialog.dismiss();
            takeImage(isFront);
        });

        dialog.show();
    }

    protected void takeImage(boolean isFront) {
        Intent imageCapture = new Intent(this, ImageCaptureActivity.class);
        imageCapture.putExtra(GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY, getString(R.string.fileprovider_authority));
        imageCapture.putExtra(GetImageActivity.EXTRA_FOLDER_PATH, getCacheDir() + "/" + getString(R.string.cards_images_folder_name));
        imageCapture.putExtra(GetImageActivity.EXTRA_FILE_NAME, createImageName(isFront));
        imageCapture.putExtra(GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE, getCalculatedLayoutWidth());
        imageCapture.putExtra(GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE, getCalculatedLayoutHeight());
        if (isFront)
            startActivityForResult(imageCapture, REQUEST_IMAGE_CAPTURE_FRONT);
        else
            startActivityForResult(imageCapture, REQUEST_IMAGE_CAPTURE_BACK);
    }

    protected void selectImage(boolean isFront) {
        Intent getContent = new Intent(this, GetContentImageActivity.class);
        getContent.putExtra(GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY, getString(R.string.fileprovider_authority));
        getContent.putExtra(GetImageActivity.EXTRA_FOLDER_PATH, getCacheDir() + "/" + getString(R.string.cards_images_folder_name));
        getContent.putExtra(GetImageActivity.EXTRA_FILE_NAME, createImageName(isFront));
        getContent.putExtra(GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE, getCalculatedLayoutWidth());
        getContent.putExtra(GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE, getCalculatedLayoutHeight());
        getContent.putExtra(GetContentImageActivity.EXTRA_TYPE, "image/*");
        if (isFront)
            startActivityForResult(getContent, REQUEST_GET_CONTENT_FRONT);
        else
            startActivityForResult(getContent, REQUEST_GET_CONTENT_BACK);
    }

    protected void scanQRCode() {
        Intent scanQRCode = new Intent(this, CodeScannerActivity.class);
        startActivityForResult(scanQRCode, REQUEST_QR_CODE_SCAN);
    }

    protected void initNewCard() {
        ID = generateNewCardID();
        cardName = getString(R.string.new_card_name);
        cardCode = "";
        cardID = "";
        cardCodeType = AppPreferenceManager.getDefaultCardCodeType(this);
        cardCodeTypeText = AppPreferenceManager.getDefaultWithText(this);
        cardColor = getResources().getColor(R.color.cardDefaultColor);
        cardPropertyIDs = new PreferenceArrayInt(null);
    }

    /**
     * removes all preferences and deletes all image files
     */
    protected void deleteCardAndFinish() {
        finishAndReturnToHome();

        CardPreferenceManager.deleteCard(this, ID); // Ctrl + Q

        // cached front and back images will be deleted with ClearDirectoryService
    }


    // color picker
    public void pickColor() {
        final ColorPickerDialogFragment dialogFragment = ColorPickerDialogFragment.newInstance(PICK_COLOR_ID, null, null, cardColor, false);

        dialogFragment.setCustomButton(getResources().getString(R.string.edit_card_auto_color_button_text), v -> {
            autoSelectColor();
            dialogFragment.setColor(cardColor);
        });

        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
        dialogFragment.show(getSupportFragmentManager(), "d");
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        if (dialogId == PICK_COLOR_ID) {
            hasBeenModified = true;
            cardColor = color;
            updateColorButtonColor();
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {}


    // subroutines
    protected void createNewCardProperty() {
        // set the property view at the end of the visible list to IME next
        CardPropertyView lastPropertyView = getLastVisiblePropertyView();
        if (lastPropertyView != null)
            lastPropertyView.setImeNext();
        else
            cardIDInputSetImeOptions(EditorInfo.IME_ACTION_NEXT);

        CardPropertyView newProperty = new CardPropertyView(this, cardPropertyIDs, ID, null, onSetPropertyDeletedListener);
        addPropertyView(newProperty);
        cardPropertyIDs.add(newProperty.getPropertyID());
        hasBeenModified = true;
    }

    protected void addPropertyView(CardPropertyView cardPropertyView) {
        linearLayout.addView(cardPropertyView, linearLayout.indexOfChild(createNewCardPropertyButton));
        cardPropertyViews.add(cardPropertyView);
        if (cardView != null) {
            cardView.post(() -> {
                cardView.drawAnimation(); // refresh because position has changed (new view was added above)
            });
        }
    }

    /**
     * updates all preferences and deletes last images if necessary
     */
    protected boolean checkInputAndSavePreferences() {
        // update values from input boxes
        if (checkAndReadInput()) {
            CardPreferenceManager.addToAllCardIDs(this, ID);

            CardPreferenceManager.writeCardName(this, ID, cardName);
            CardPreferenceManager.writeCardCode(this, ID, cardCode);
            CardPreferenceManager.writeCardCodeType(this, ID, cardCodeType);
            CardPreferenceManager.writeCardCodeTypeText(this, ID, cardCodeTypeText);
            CardPreferenceManager.writeCardID(this, ID, cardID);
            CardPreferenceManager.writeCardColor(this, ID, cardColor);

            moveFrontImageFromCacheToFiles();
            moveBackImageFromCacheToFiles();

            CardPreferenceManager.writeCardFrontImage(this, ID, currentFrontImage);
            CardPreferenceManager.writeCardBackImage(this, ID, currentBackImage);

            for (CardPropertyView propertyView : cardPropertyViews)
                propertyView.save(); // saves or removes each property

            CardPreferenceManager.writeCardPropertyIds(this, ID, cardPropertyIDs);
            return true;
        }
        else {
            Toast.makeText(this, R.string.there_are_still_errors, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    protected void autoSelectColor() {
        if (cardView.getFrontImage() != null && cardView.getBackImage() != null) {
            @ColorInt int color1 = Utility.getAverageColorRGB(cardView.getFrontImage());
            @ColorInt int color2 = Utility.getAverageColorRGB(cardView.getBackImage());
            cardColor = Utility.getAverageColorARGB(color1, color2);
        }
        else if (cardView.getFrontImage() != null)
            cardColor = Utility.getAverageColorRGB(cardView.getFrontImage());
        else if (cardView.getBackImage() != null)
            cardColor = Utility.getAverageColorRGB(cardView.getBackImage());
        else
            Toast.makeText(this, R.string.no_images_to_calc_color, Toast.LENGTH_SHORT).show();
    }

    /**
     * deletes current image file (if it's not the same as last image) and replaces it with <code>imageFile</code>, while not changing anything about last image
     * @param isFront true, when setting front image, false when setting back image
     * @param imageFile file to set as image
     */
    protected void setCardImage(boolean isFront, File imageFile) {
        hasBeenModified = true;
        if (isFront) {
            if (currentFrontImage != lastFrontImage)
                deleteFrontImage();
            currentFrontImage = imageFile;
            updateFrontImage();
        }
        else {
            if (currentBackImage != lastBackImage)
                deleteBackImage();
            currentBackImage = imageFile;
            updateBackImage();
        }
        if ((isFront && currentFrontImage == null) || (!isFront && currentBackImage == null))
            throw new IllegalStateException("EditCardActivity.setCardImage: Image was not set for whatever reason. isFront: " + isFront + ", currentFrontImage: " + currentFrontImage + ", currentBackImage: " + currentBackImage);

    }

    /**
     * deletes last front image and copies to current front image from cache to files directory, if front image has been changed
     */
    protected void moveFrontImageFromCacheToFiles() {
        if (currentFrontImage != lastFrontImage) {
            // delete last
            deleteLastFrontImage();

            // copy current
            if (currentFrontImage != null) {
                OutputStream outputStream = null;
                try {
                    File imagesDirectory = new File(getFilesDir() + "/" + getString(R.string.cards_images_folder_name));
                    if (!imagesDirectory.exists())
                        imagesDirectory.mkdirs();


                    File newFrontImage = new File(imagesDirectory, currentFrontImage.getName());

                    MasterKey mainKey = new MasterKey.Builder(this)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

                    EncryptedFile encryptedFile = new EncryptedFile.Builder(this,
                            newFrontImage,
                            mainKey,
                            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build();

                    outputStream = encryptedFile.openFileOutput();

                    Utility.copyFile(new FileInputStream(currentFrontImage), outputStream);

                    if (!currentFrontImage.delete()) {
                        //if (BuildConfig.DEBUG)
                        //    Log.e("EditCardActivity", "Could not delete front image file after copying it from cache to files");
                    }

                    currentFrontImage = newFrontImage;

                    // TODO remove old code from before encryption
                    //currentFrontImage = Utility.moveFile(currentFrontImage, getFilesDir() + "/" + getString(R.string.cards_images_folder_name));

                } catch (IOException | GeneralSecurityException e) {
                    /*
                    if (BuildConfig.DEBUG)
                        Log.e("EditCardActivity", "couldn't copy current front image file from cache to files directory");
                     */
                    throw new AssertionError(e);
                } finally {
                    // flush and close the stream
                    if (outputStream != null) {
                        try {
                            outputStream.flush();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * deletes last back image and copies current back image from cache to files directory, if back image has been changed
     */
    protected void moveBackImageFromCacheToFiles() {
        if (currentBackImage != lastBackImage) {
            // delete last
            deleteLastBackImage();

            // copy current
            if (currentBackImage != null) {
                OutputStream outputStream = null;
                try {
                    File imagesDirectory = new File(getFilesDir() + "/" + getString(R.string.cards_images_folder_name));
                    if (!imagesDirectory.exists())
                        imagesDirectory.mkdirs();

                    File newBackImage = new File(imagesDirectory, currentBackImage.getName());

                    MasterKey mainKey = new MasterKey.Builder(this)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

                    EncryptedFile encryptedFile = new EncryptedFile.Builder(this,
                            newBackImage,
                            mainKey,
                            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build();

                    outputStream = encryptedFile.openFileOutput();

                    Utility.copyFile(new FileInputStream(currentBackImage), outputStream);

                    if (!currentBackImage.delete()) {
                        //if (BuildConfig.DEBUG)
                        //    Log.e("EditCardActivity", "Could not delete back image file after copying it from cache to files");
                    }

                    currentBackImage = newBackImage;

                    // TODO remove old code from before encryption
                    //currentBackImage = Utility.moveFile(currentBackImage, getFilesDir() + "/" + getString(R.string.cards_images_folder_name));

                } catch (IOException | GeneralSecurityException e) {
                    /*
                    if (BuildConfig.DEBUG)
                        Log.e("EditCardActivity", "couldn't copy current back image file from cache to files directory");
                     */
                    throw new AssertionError(e);
                } finally {
                    // flush and close the stream
                    if (outputStream != null) {
                        try {
                            outputStream.flush();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    protected void removeFrontImage() {
        if (currentFrontImage != null) {
            cardView.removeFrontImage();
            CardPreferenceManager.removeCardFrontImage(this, ID);
            deleteFrontImage();
        }
    }

    protected void removeBackImage() {
        if (currentBackImage != null) {
            cardView.removeBackImage();
            CardPreferenceManager.removeCardBackImage(this, ID);
            deleteBackImage();
        }
    }

    /**
     * deletes last front image
     * @throws AssertionError if file couldn't get deleted
     */
    protected void deleteLastFrontImage() {
        if (lastFrontImage != null) {
            if  (!lastFrontImage.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("EditCardActivity", "last front image file couldn't be deleted");
                 */
            }
            lastFrontImage = null;
        }
    }

    /**
     * deletes last back image
     * @throws AssertionError if file couldn't get deleted
     */
    protected void deleteLastBackImage() {
        if (lastBackImage != null) {
            if (!lastBackImage.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("EditCardActivity", "last back image file couldn't be deleted");
                 */
            }
            lastBackImage = null;
        }
    }

    private boolean readNameInput() {
        String newCardName = cardNameInput.getText().toString().trim();

        if (!cardName.equals(newCardName)) {
            hasBeenModified = true;
            cardName = newCardName;

            if (cardName.equals("")) {
                cardNameInput.setError(getString(R.string.card_name_empty_error));
                return false;
            }
            else
                cardNameInput.setError(null);
        }
        return true;
    }

    private boolean readCodeInput() {
        String newCardCode = cardCodeInput.getText().toString().trim();
        if (!cardCode.equals(newCardCode)) {
            hasBeenModified = true;
            cardCode = newCardCode;
        }
        return true;
    }

    private boolean readCodeTypeInput() {
        int newCardCodeType = codeTypeStringToInt(this, cardCodeTypeInput.getText().toString());
        if (cardCodeType != newCardCodeType) {
            hasBeenModified = true;
            cardCodeType = newCardCodeType;
        }
        return true;
    }

    private boolean readCodeTypeTextInput() {
        boolean newCardCodeTypeText = codeTypeTextStringToBool(cardCodeTypeTextInput.getText().toString());
        if (cardCodeTypeText != newCardCodeTypeText) {
            hasBeenModified = true;
            cardCodeTypeText = newCardCodeTypeText;
        }
        return true;
    }

    private boolean readIDInput() {
        String newCardID = cardIDInput.getText().toString().trim();

        if (!cardID.equals(newCardID)) {
            hasBeenModified = true;
            cardID = newCardID;

            // Mahler easter egg
            if (isCreateNewCardIntent && cardID.equals(getString(R.string.mahler_is_cool))) {
                makeMahlerCard();
            }
        }
        return true;
    }


    // helper functions
    /**
     * Reads all the input from the input boxes
     * @return false when there are errors, true otherwise
     */
    protected boolean checkAndReadInput() {
        return readNameInput() && readCodeInput() && readCodeTypeInput() && readCodeTypeTextInput() && readIDInput();
    }

    protected String createImageName(boolean isFront) {
        String frontOrBack = (isFront ? "front" : "back");
        return "JPEG_" + ID + "_" + frontOrBack + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
    }

    protected int generateNewCardID() {
        List<Integer> cardIDs = CardPreferenceManager.readAllCardIDs(this);
        int newID;
        do {
            newID = new Random().nextInt();
        } while (cardIDs.contains(newID));
        return newID;
    }

    private void updateColorButtonColor() {
        cardColorButton.setBackgroundColor(cardColor);
        if (Utility.isColorDark(cardColor))
            cardColorButton.setTextColor(getResources().getColor(R.color.onDarkTextColor));
        else
            cardColorButton.setTextColor(getResources().getColor(R.color.onLightTextColor));
    }

    private ArrayAdapter<String> getNewCardCodeTypeAdapter() {
        final String[] codeValues = new String[] {
                getString(R.string.card_code_type_value_qr),
                getString(R.string.card_code_type_value_code_39),
                getString(R.string.card_code_type_value_code_128),
                getString(R.string.card_code_type_value_upc_a),
                getString(R.string.card_code_type_value_ean_13),
                getString(R.string.card_code_type_value_ean_8),
                getString(R.string.card_code_type_value_itf),
                getString(R.string.card_code_type_value_upc_e),
                getString(R.string.card_code_type_value_data_matrix),
                getString(R.string.card_code_type_value_pdf_417),
                getString(R.string.card_code_type_value_aztec),
                getString(R.string.card_code_type_value_codabar),
                getString(R.string.card_code_type_value_code_93),
        };
        return new ArrayAdapter<>(this, R.layout.dropdown_meu_popup_item, codeValues);
    }

    private void hideCardCodeTypeAndShowLayout() {
        if (linearLayout.indexOfChild(cardCodeTypeAndShowLayout) != -1)
            linearLayout.removeView(cardCodeTypeAndShowLayout);
    }

    private void showCardCodeTypeAndShowLayout() {
        if (linearLayout.indexOfChild(cardCodeTypeAndShowLayout) == -1)
            linearLayout.addView(cardCodeTypeAndShowLayout, linearLayout.indexOfChild(cardCodeInputLayout) + 1); // below cardCodeInputLayout
    }

    private CardPropertyView getLastVisiblePropertyView() {
        for (int i = cardPropertyViews.size() - 1; i >= 0; i--) {
            CardPropertyView propertyView = cardPropertyViews.get(i);
            if (!propertyView.isDeleted())
                return propertyView;
        }
        return null;
    }

    private void cardIDInputSetImeOptions(int imeOptions) {
        Utility.setImeOptionsAndRestart(cardIDInput, imeOptions);
    }

    private void makeMahlerCard() {
        if (!isMahlerCardInit && cardPropertyIDs.size() == 0) {
            deleteFrontImage();
            InputStream frontImageStream = getResources().openRawResource(R.raw.front_mahler_image);
            // ensure cards images folder exists
            File cacheCardsImagesFolder = new File(getCacheDir() + "/" + getString(R.string.cards_images_folder_name));
            if (!cacheCardsImagesFolder.exists())
                cacheCardsImagesFolder.mkdirs();
            currentFrontImage = CreateExampleCardService.copyCardImage(this, frontImageStream, cacheCardsImagesFolder, "JPEG_" + ID + "_" + getString(R.string.mahler_card_front_image_file_name));
            updateFrontImage();

            cardName = "Gustav Mahler";
            cardNameInput.setText(cardName);

            cardID = "";
            cardIDInput.setText(cardID);

            cardColor = Color.rgb(221, 213, 177);
            updateColorButtonColor();

            createNewCardProperty();
            CardPropertyView infoView = cardPropertyViews.get(cardPropertyViews.size() - 1);
            infoView.setPropertyName(getString(R.string.info));
            infoView.inputLayout.setHint(infoView.getPropertyName());
            infoView.setPropertyValue(getString(R.string.mahler_is_cool));
            infoView.inputEditText.setText(infoView.getPropertyValue());

            createNewCardProperty();
            CardPropertyView importantView = cardPropertyViews.get(cardPropertyViews.size() - 1);
            importantView.setPropertyName(getString(R.string.important));
            importantView.inputLayout.setHint(importantView.getPropertyName());
            importantView.setPropertyValue(getString(R.string.listen_to_mahler_9));
            importantView.inputEditText.setText(importantView.getPropertyValue());

            isMahlerCardInit = true;
        }
    }

}