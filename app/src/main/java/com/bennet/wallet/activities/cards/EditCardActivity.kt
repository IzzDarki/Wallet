package com.bennet.wallet.activities.cards

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.bennet.colorpickerview.dialog.ColorPickerDialogFragment
import com.bennet.wallet.R
import com.bennet.wallet.activities.*
import com.bennet.wallet.adapters.EditPropertyAdapter
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.CardPreferenceManager
import com.bennet.wallet.services.CreateExampleCardService
import com.bennet.wallet.components.EditLabelsComponent
import com.bennet.wallet.utils.ItemProperty
import com.bennet.wallet.utils.Utility
import com.bennet.wallet.utils.Utility.PreferenceArrayInt
import com.bennet.wallet.utils.Utility.PreferenceArrayString
import com.bennet.wallet.utils.Utility.IDGenerator
import com.bennet.wallet.utils.Utility.hideKeyboard
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*

class EditCardActivity
    : CardActivity(), ColorPickerDialogFragment.ColorPickerDialogListener {

    // region static const val
    companion object {
        const val EXTRA_CREATE_NEW_CARD = "com.bennet.wallet.edit_card_activity.create_new_card"
        const val PICK_COLOR_DIALOG_ID = 0
    }
    // endregion


    // region UI
    private lateinit var cardNameInputLayout: TextInputLayout
    private lateinit var cardNameInputEditText: TextInputEditText
    private lateinit var cardCodeInputLayout: TextInputLayout
    private lateinit var cardCodeInputEditText: TextInputEditText
    private lateinit var cardCodeTypeAndTextLayout: LinearLayoutCompat
    private lateinit var cardCodeTypeInput: MaterialAutoCompleteTextView
    private lateinit var cardCodeTypeTextInput: MaterialAutoCompleteTextView
    private lateinit var propertiesRecyclerView: RecyclerView
    private lateinit var addNewPropertyChip: Chip
    private lateinit var editLabelsComponent: EditLabelsComponent
    private lateinit var cardColorChip: Chip
    private lateinit var cardFrontImageChip: Chip
    private lateinit var cardBackImageChip: Chip
    // endregion


    // region variables
    private var isCreateNewCardIntent = false
    private var isMahlerCardInit = false
    private var hasBeenModified = false
    private var lastFrontImage: File? = null
    private var lastBackImage: File? = null
    // endregion


    // region card properties (some others are inherited from CardActivity)
    private var cardProperties: MutableList<ItemProperty> = mutableListOf()
    private lateinit var labels: PreferenceArrayString  // will not be kept up to date (only readAndCheckLabels updates labels)
    // endregion


    // region overrides
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreateNewCardIntent = intent.getBooleanExtra(EXTRA_CREATE_NEW_CARD, false)

        setContentView(R.layout.activity_edit_card)

        // hooks
        cardViewLayout = findViewById(R.id.edit_card_card_view_layout)
        scrollView = findViewById(R.id.edit_card_scroll_view)
        linearLayout = findViewById(R.id.edit_card_linear_layout)
        cardNameInputLayout = findViewById(R.id.edit_card_name_layout)
        cardNameInputEditText = findViewById(R.id.edit_card_name_input)
        cardCodeInputLayout = findViewById(R.id.edit_card_code_layout)
        cardCodeInputEditText = findViewById(R.id.edit_card_code_input)
        cardCodeTypeAndTextLayout = findViewById(R.id.edit_card_code_type_and_text_layout)
        cardCodeTypeInput = findViewById(R.id.edit_card_code_type_input)
        cardCodeTypeTextInput = findViewById(R.id.edit_card_code_type_text_input)
        propertiesRecyclerView = findViewById(R.id.edit_card_recycler_view)
        addNewPropertyChip = findViewById(R.id.edit_card_add_new_property_chip)
        cardColorChip = findViewById(R.id.edit_card_color_chip)
        cardFrontImageChip = findViewById(R.id.edit_card_front_image_chip)
        cardBackImageChip = findViewById(R.id.edit_card_back_image_chip)
        editLabelsComponent = EditLabelsComponent(this, R.id.edit_card_labels_chip_group, R.id.edit_card_labels_add_chip, linearLayout)

        // toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // init
        if (isCreateNewCardIntent) {
            initNewCard() // create new card
            supportActionBar!!.setTitle(R.string.new_card)
        } else {
            initFromPreferences() // load existing card
            supportActionBar!!.setTitle(R.string.edit_card)
            lastFrontImage = currentFrontImage
            lastBackImage = currentBackImage
            /* explanation
                last images are the images, that are currently saved preferences, they are never being displayed
                current images are the images, that are not saved in preferences, they are currently being displayed
                if EditCardActivity "save"s, the last images get deleted and new ones get saved in preferences
                if EditCardActivity "cancel"s, the current images get deleted and the preferences remain unchanged
                if the images remain unchanged, last and current images are the same, then nothing gets deleted and preferences remain unchanged (scenario same for both "cancel" and "save")
                 */
        }

        // scroll view (TODO feature was removed, because it didn't do anything)
        // hideScrollbar()

        // card name
        cardNameInputEditText.setText(cardName)
        cardNameInputEditText.addTextChangedListener(object : TextWatcher {
            // this callback is able to set the error immediately
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val newName = s.toString()
                if (newName == "")
                    cardNameInputLayout.error = getString(R.string.card_name_empty_error)
                else
                    cardNameInputLayout.error = null
                cardName = newName
            }
        })
        cardNameInputEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                cardName = cardName.trim()
                cardNameInputEditText.setText(cardName)

                // Mahler easter egg
                if (isCreateNewCardIntent && cardCode == getString(R.string.mahler_is_cool))
                    makeMahlerCard()
            }
        }

        // card code
        cardCodeInputEditText.setText(cardCode)
        cardCodeInputEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                cardCode = cardCodeInputEditText.text.toString().trim()
                cardCodeInputEditText.setText(cardCode)
            }
        }
        cardCodeInputEditText.addTextChangedListener(object : TextWatcher {
            // hide or show text fields for card code type and card code text type
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString() == "")
                    hideCardCodeTypeAndTextLayout()
                else
                    showCardCodeTypeAndTextLayout()
            }
        })
        cardCodeInputLayout.setEndIconOnClickListener { scanQRCode() }

        // card code type
        if (cardCode == "")
            hideCardCodeTypeAndTextLayout()
        cardCodeTypeInput.setText(codeTypeIntToString(this, cardCodeType))
        cardCodeTypeInput.setAdapter<ArrayAdapter<String>>(getNewCardCodeTypeAdapter())
        cardCodeTypeInput.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus)
                cardCodeType = codeTypeStringToInt(this, cardCodeTypeInput.text.toString())
        }
        cardCodeTypeInput.width = ((calculatedLayoutWidth - resources.getDimension(R.dimen.text_input_padding_bottom)) / 2).toInt()

        // card code text type
        cardCodeTypeTextInput.setText(codeTypeTextBoolToString(cardCodeTypeText))
        cardCodeTypeTextInput.setAdapter(
            ArrayAdapter(
                this,
                R.layout.dropdown_meu_popup_item,
                arrayOf(
                    getString(R.string.card_code_type_text_value_show_text),
                    getString(R.string.card_code_type_text_value_dont_show_text)
                )
            )
        )
        cardCodeTypeTextInput.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus)
                cardCodeTypeText = codeTypeTextStringToBool(cardCodeTypeTextInput.text.toString())
        }
        cardCodeTypeTextInput.width = ((calculatedLayoutWidth - resources.getDimension(R.dimen.text_input_padding_bottom)) / 2).toInt()

        // labels chip group
        editLabelsComponent.displayLabels(labels)

        // create new card property button
        addNewPropertyChip.setOnClickListener { addNewProperty() }

        // card color
        cardColorChip.setOnClickListener { pickColor() }
        updateColorButtonColor()

        // card images
        cardFrontImageChip.setOnClickListener { chooseImage(true) }
        cardBackImageChip.setOnClickListener { chooseImage(false) }

        // card view
        createCardView()
        cardView.frontText = getString(R.string.front_image)
        cardView.backText = getString(R.string.back_image)

        // recycler view
        propertiesRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = EditPropertyAdapter(cardProperties) {
            onPropertyRemoval()
        }
        propertiesRecyclerView.adapter = adapter
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Every touch event goes through this function
        if (editLabelsComponent.dispatchTouchEvent(ev))
            return true
        else
            return super.dispatchTouchEvent(ev)
    }

    override fun finish() {
        hideKeyboard()
        super.finish()
    }
    // endregion


    // region main functions
    override fun initFromPreferences() {
        super.initFromPreferences()

        labels = CardPreferenceManager.readLabels(this, ID)
        cardProperties = CardPreferenceManager.readProperties(this, ID)

        if (cardProperties.isEmpty()) {
            // set IME options on last input field to done
            if (cardCodeInputEditText.visibility != View.GONE)
                Utility.setImeOptionsAndRestart(cardCodeInputEditText, EditorInfo.IME_ACTION_DONE)
            else
                Utility.setImeOptionsAndRestart(cardCodeTypeTextInput, EditorInfo.IME_ACTION_DONE)
        }
    }

    private fun initNewCard() {
        ID = generateNewCardID()
        cardName = getString(R.string.new_card)
        labels = PreferenceArrayString()
        cardCode = ""
        cardCodeType = AppPreferenceManager.getDefaultCardCodeType(this)
        cardCodeTypeText = AppPreferenceManager.getDefaultWithText(this)
        cardColor = resources.getColor(R.color.card_default_color)

        cardProperties.add(
            ItemProperty(
                name = getString(R.string.card_id),
                value = "",
                secret = false,
                propertiesNeededToCreateNewID = cardProperties
            )
        )
    }

    private fun addNewProperty(
        name: String = getString(R.string.new_card_property_name),
        value: String = "",
        secret: Boolean = false
    ) {
        cardProperties.add(
            ItemProperty(name, value, secret, propertiesNeededToCreateNewID = cardProperties)
        )
        propertiesRecyclerView.adapter?.notifyItemInserted(cardProperties.size - 1)
        val secondLastViewHolder = propertiesRecyclerView.findViewHolderForAdapterPosition(cardProperties.size - 2) as? EditPropertyAdapter.ViewHolder
        secondLastViewHolder?.setImeOptions(EditorInfo.IME_ACTION_NEXT)

        // set IME options on other input fields to next (only these 2 could be set to done)
        Utility.setImeOptionsAndRestart(cardCodeInputEditText, EditorInfo.IME_ACTION_NEXT)
        Utility.setImeOptionsAndRestart(cardCodeTypeTextInput, EditorInfo.IME_ACTION_NEXT)
    }

    private fun saveAndShowCard() {
        if (!readAndCheckAllInput()) {
            // When there are errors, saving will be aborted and Toast will be shown to user
            Toast.makeText(this, R.string.there_are_still_errors, Toast.LENGTH_SHORT).show()
            return
        }

        writeToPreferences()

        finishAndShowCard()
    }

    /**
     * If possible cancels the editing activity.
     * If there are errors in user input, a Toast is shown and the activity will not be canceled.
     * If the back confirm preference is true, an AlertDialog is shown and the activity will not be canceled.
     * @return `true` if the activity got finished, `false` otherwise
     */
    private fun requestCancel(): Boolean {
        readAndCheckAllInput() // Read input to figure out if card has been modified (member variable hasBeenModified)

        if (AppPreferenceManager.isBackConfirmNewCardOrPassword(this) && isCreateNewCardIntent
            || AppPreferenceManager.isBackConfirmEditCardOrPassword(this) && !isCreateNewCardIntent && hasBeenModified) {

            val dialogTitle: String
            val dialogMessage: String
            if (isCreateNewCardIntent) {
                dialogTitle = getString(R.string.cancel_create_new_card_title)
                dialogMessage = getString(R.string.nothing_will_be_saved)
            } else {
                dialogTitle = getString(R.string.discard_changes)
                dialogMessage = getString(R.string.changes_are_not_saved)
            }
            AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setMessage(dialogMessage)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                    cancelDirectly()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                    hasBeenModified = false // reset to false, on next call to requestCancel hasBeenModified will be evaluated again
                }
                .show()
            return false
        }
        cancelDirectly()
        return true
    }

    /**
     * removes all preferences and deletes all image files
     */
    private fun deleteAndReturnToHome() {
        finishAndReturnToHome()
        CardPreferenceManager.removeComplete(this, ID)
        // cached front and back images will be deleted with ClearDirectoryService
    }
    //endregion


    // region activity results
    private val imageCaptureFront = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        handleImageCaptureResult(result, true)
    }

    private val imageCaptureBack = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        handleImageCaptureResult(result, false)
    }

    private val getContentFront = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        handleGetContentResult(result, true)
    }

    private val getContentBack = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        handleGetContentResult(result, false)
    }

    private val scanQRCode = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val barcodeFormat = result.data!!.getSerializableExtra(CodeScannerActivity.EXTRA_RESULT_CODE_TYPE) as BarcodeFormat
            cardCodeType = try {
                cardCodeBarcodeFormatToInt(barcodeFormat)
            } catch (e: IllegalStateException) {
                Toast.makeText(this, R.string.unsupported_type_barcode, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            cardCode = result.data!!.getStringExtra(CodeScannerActivity.EXTRA_RESULT_CODE)
            cardCodeInputEditText.setText(cardCode)
            cardCodeTypeInput.setText(codeTypeIntToString(this, cardCodeType))
            cardCodeTypeInput.setAdapter<ArrayAdapter<String>>(getNewCardCodeTypeAdapter()) // reset the adapter (otherwise there are filters)
        }
        else if (result.resultCode == CodeScannerActivity.RESULT_PERMISSION_DENIED)
            Toast.makeText(this, R.string.code_scanner_camera_permission_denied, Toast.LENGTH_SHORT).show()
    }

    private fun handleImageCaptureResult(result: ActivityResult, isFront: Boolean) {
        when (result.resultCode) {
            RESULT_OK ->
                setCardImage(isFront, result.data!!.getSerializableExtra(ImageCaptureActivity.EXTRA_RESULT_FILE) as File)

            ImageCaptureActivity.RESULT_NO_IMAGE_CAPTURE_INTENT ->
                Toast.makeText(this, R.string.no_image_capture_intent, Toast.LENGTH_SHORT).show()

            ImageCaptureActivity.RESULT_PERMISSION_DENIED ->
                Toast.makeText(this, R.string.image_capture_camera_permission_denied, Toast.LENGTH_SHORT).show()

            RESULT_CANCELED -> {}

            else ->
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGetContentResult(result: ActivityResult, isFront: Boolean) {
        when (result.resultCode) {
            RESULT_OK ->
                setCardImage(isFront, result.data!!.getSerializableExtra(GetContentImageActivity.EXTRA_RESULT_FILE) as File)

            GetContentImageActivity.RESULT_NO_GET_CONTENT_INTENT ->
                Toast.makeText(this, R.string.no_get_content_intent, Toast.LENGTH_SHORT).show()

            RESULT_CANCELED -> {}

            else -> Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
        }
    }
    // endregion


    // region action bar menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_activity_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.edit_action_bar_done) {  // finish if input can be saved (no input errors)
            saveAndShowCard()
            return true
        }
        else if (item.itemId == R.id.edit_action_bar_delete) {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_card)
                .setMessage(R.string.delete_card_dialog_message)
                .setCancelable(true)
                .setPositiveButton(R.string.delete) { dialog, _ ->
                    deleteAndReturnToHome()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                }
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return requestCancel()
    }

    override fun onBackPressed() {
        requestCancel()
    }
    // endregion


    // region start other activities
    private fun chooseImage(isFront: Boolean) {
        hideKeyboard()
        val dialog: AlertDialog
        val builder = AlertDialog.Builder(this)

        val dialogLayout = View.inflate(this, R.layout.image_chooser_dialog, null)
        val deleteImageGroup = dialogLayout.findViewById<ViewGroup>(R.id.image_chooser_dialog_delete)
        val selectImageGroup = dialogLayout.findViewById<ViewGroup>(R.id.image_chooser_dialog_storage)
        val imageCaptureGroup = dialogLayout.findViewById<ViewGroup>(R.id.image_chooser_dialog_camera)

        builder.setView(dialogLayout)
        dialog = builder.create()
        deleteImageGroup.setOnClickListener {
            dialog.dismiss()
            if (isFront)
                removeFrontImage()
            else
                removeBackImage()
        }
        selectImageGroup.setOnClickListener {
            dialog.dismiss()
            selectImage(isFront)
        }
        imageCaptureGroup.setOnClickListener {
            dialog.dismiss()
            takeImage(isFront)
        }
        dialog.show()
    }

    private fun takeImage(isFront: Boolean) {
        val imageCaptureIntent = Intent(this, ImageCaptureActivity::class.java)
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY,
            getString(R.string.fileprovider_authority)
        )
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_FOLDER_PATH,
            cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name)
        )
        imageCaptureIntent.putExtra(GetImageActivity.EXTRA_FILE_NAME, createImageName(isFront))
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE,
            calculatedLayoutWidth
        )
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE,
            calculatedLayoutHeight
        )

        if (isFront)
            imageCaptureFront.launch(imageCaptureIntent)
        else
            imageCaptureBack.launch(imageCaptureIntent)
    }

    private fun selectImage(isFront: Boolean) {
        val getContentIntent = Intent(this, GetContentImageActivity::class.java)
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY,
            getString(R.string.fileprovider_authority)
        )
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_FOLDER_PATH,
            cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name)
        )
        getContentIntent.putExtra(GetImageActivity.EXTRA_FILE_NAME, createImageName(isFront))
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE,
            calculatedLayoutWidth
        )
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE,
            calculatedLayoutHeight
        )
        getContentIntent.putExtra(GetContentImageActivity.EXTRA_TYPE, "image/*")

        if (isFront)
            getContentFront.launch(getContentIntent)
        else
            getContentBack.launch(getContentIntent)
    }

    private fun scanQRCode() {
        val scanQRCodeIntent = Intent(this, CodeScannerActivity::class.java)
        scanQRCode.launch(scanQRCodeIntent)
    }
    // endregion


    // region color and color picker
    private fun pickColor() {
        val dialogFragment = ColorPickerDialogFragment.newInstance(
            PICK_COLOR_DIALOG_ID,
            null,
            null,
            cardColor,
            false
        )
        dialogFragment.setCustomButton(resources.getString(R.string.edit_card_auto_color_button_text)) {
            autoSelectColor()
            dialogFragment.setColor(cardColor)
        }
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, 0)
        dialogFragment.show(supportFragmentManager, "d")
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        if (dialogId == PICK_COLOR_DIALOG_ID) {
            hasBeenModified = true
            cardColor = color
            updateColorButtonColor()
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}

    private fun updateColorButtonColor() {
        cardColorChip.chipBackgroundColor = ColorStateList.valueOf(cardColor)
        if (Utility.isColorDark(cardColor))
            cardColorChip.setTextColor(resources.getColor(R.color.on_dark_text_color))
        else
            cardColorChip.setTextColor(resources.getColor(R.color.on_light_text_color))

        if (Utility.areColorsSimilar(
                Utility.getDefaultBackgroundColor(this),
                cardColor
            )
        ) {
            // draw outline
            cardColorChip.chipStrokeWidth = resources.getDimension(R.dimen.outline_for_similar_colors_stroke_width)
            cardColorChip.chipStrokeColor = ColorStateList.valueOf(resources.getColor(R.color.card_view_outline_color))
        } else {
            // remove outline
            cardColorChip.chipStrokeWidth = 0F
        }
    }

    private fun autoSelectColor() {
        if (cardView.frontImage != null && cardView.backImage != null) {
            @ColorInt val color1 = Utility.getAverageColorRGB(cardView.frontImage)
            @ColorInt val color2 = Utility.getAverageColorRGB(cardView.backImage)
            cardColor = Utility.getAverageColorARGB(color1, color2)
        }
        else if (cardView.frontImage != null)
            cardColor = Utility.getAverageColorRGB(cardView.frontImage)
        else if (cardView.backImage != null)
            cardColor = Utility.getAverageColorRGB(cardView.backImage)
        else
            Toast.makeText(this, R.string.no_images_to_calc_color, Toast.LENGTH_SHORT).show()
    }
    // endregion


    // region read and check input fields
    /**
     * Reads all the user input and checks if the card has been modified ({@link #hasBeenModified} will be set true)
     * @return true if there are no errors, false otherwise
     */
    private fun readAndCheckAllInput(): Boolean {
        readAndCheckCardCodeInput()
        readAndCheckLabels()
        readAndCheckCardCodeTypeInput()
        readAndCheckCardCodeTypeTextInput()
        readAndCheckAllProperties()
        checkIfPropertyHasBeenRemoved()
        return readAndCheckNameInput()
    }

    /**
     * Checks if there are errors in the name input.
     * Updates [hasBeenModified]
     * @return true if there are no errors in the users input, false otherwise
     */
    private fun readAndCheckNameInput(): Boolean {
        cardName = cardNameInputEditText.text.toString().trim()
        if (cardNameInputLayout.error != null)
            return false

        if (cardName != CardPreferenceManager.readName(this, ID))
            hasBeenModified = true
        return true
    }

    private fun readAndCheckLabels() {
        val oldLabels = CardPreferenceManager.readLabels(this, ID)
        labels = PreferenceArrayString(editLabelsComponent.readAllLabels().iterator())
        if (!oldLabels.containsAll(labels) || !labels.containsAll(oldLabels))
            hasBeenModified = true
    }

    private fun readAndCheckCardCodeInput() {
        cardCode = cardCodeInputEditText.text.toString().trim()
        if (cardCode != CardPreferenceManager.readCode(this, ID))
            hasBeenModified = true
    }

    private fun readAndCheckCardCodeTypeInput() {
        cardCodeType = codeTypeStringToInt(this, cardCodeTypeInput.text.toString())
        if (cardCodeType != CardPreferenceManager.readCodeType(this, ID))
            hasBeenModified = true
    }

    private fun readAndCheckCardCodeTypeTextInput() {
        cardCodeTypeText = codeTypeTextStringToBool(cardCodeTypeTextInput.text.toString())
        if (cardCodeTypeText != CardPreferenceManager.readCodeTypeText(this, ID))
            hasBeenModified = true
    }

    /**
     * Checks if any property was modified. Also checks if a new property was added.
     * Then sets [hasBeenModified] to true
     */
    private fun readAndCheckAllProperties() {
        for (position in cardProperties.indices) {
            val holder = propertiesRecyclerView.findViewHolderForAdapterPosition(position) as EditPropertyAdapter.ViewHolder?
            holder?.readValue()

            val property: ItemProperty = cardProperties[position]
            if (property.name != CardPreferenceManager.readPropertyName(this, ID, property.propertyID))
                hasBeenModified = true // This accounts for newly added properties (because preference manager returns null string)
            if (property.value != CardPreferenceManager.readPropertyValue(this, ID, property.propertyID))
                hasBeenModified = true
            if (property.secret != CardPreferenceManager.readPropertySecret(this, ID, property.propertyID))
                hasBeenModified = true
        }
    }

    /**
     *  Checks if a property got removed.
     *  Then sets [hasBeenModified] to true
     */
    private fun checkIfPropertyHasBeenRemoved() {
        for (propertyID in getCardPropertyIDs()) {
            if (!doesPropertyIDExist(propertyID)) { // if propertyID doesn't exist anymore after editing
                hasBeenModified = true
                return
            }
        }
    }
    // endregion
    

    // region image helpers
    /**
     * deletes current image file (if it's not the same as last image) and replaces it with `imageFile`, while not changing anything about last image
     * @param isFront true, when setting front image, false when setting back image
     * @param imageFile file to set as image
     */
    private fun setCardImage(isFront: Boolean, imageFile: File) {
        hasBeenModified = true
        if (isFront) {
            if (currentFrontImage !== lastFrontImage)
                deleteFrontImage()
            currentFrontImage = imageFile
            updateFrontImage()
        }
        else {
            if (currentBackImage !== lastBackImage)
                deleteBackImage()
            currentBackImage = imageFile
            updateBackImage()
        }
        check(!(isFront && currentFrontImage == null || !isFront && currentBackImage == null)) {
            "EditCardActivity.setCardImage: Image was not set for whatever reason. isFront: $isFront, currentFrontImage: $currentFrontImage, currentBackImage: $currentBackImage"
        }
    }

    /**
     * deletes last front image and copies to current front image from cache to files directory, if front image has been changed. Doesn't copy the file if it's already in files directory
     */
    private fun moveFrontImageFromCacheToFiles() {
        if (currentFrontImage !== lastFrontImage && currentFrontImage != null && !isInFilesDir(currentFrontImage!!)) {
            // delete last
            deleteLastFrontImage()

            // copy current
            if (currentFrontImage != null) {
                var outputStream: OutputStream? = null
                try {
                    val imagesDirectory =
                        File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
                    if (!imagesDirectory.exists()) imagesDirectory.mkdirs()
                    val newFrontImage = File(imagesDirectory, currentFrontImage?.name)
                    val mainKey = MasterKey.Builder(this)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    val encryptedFile = EncryptedFile.Builder(
                        this,
                        newFrontImage,
                        mainKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build()
                    outputStream = encryptedFile.openFileOutput()
                    Utility.copyFile(FileInputStream(currentFrontImage), outputStream)
                    if (!currentFrontImage!!.delete()) {
                        //if (BuildConfig.DEBUG)
                        //    Log.e("EditCardActivity", "Could not delete front image file after copying it from cache to files");
                    }
                    currentFrontImage = newFrontImage
                } catch (e: IOException) {
                    /*
                if (BuildConfig.DEBUG)
                    Log.e("EditCardActivity", "couldn't copy current front image file from cache to files directory");
                 */
                    throw AssertionError(e)
                } catch (e: GeneralSecurityException) {
                    throw AssertionError(e)
                } finally {
                    // flush and close the stream
                    if (outputStream != null) {
                        try {
                            outputStream.flush()
                            outputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    /**
     * deletes last back image and copies current back image from cache to files directory, if back image has been changed. Doesn't copy the file if it's already in files directory
     */
    private fun moveBackImageFromCacheToFiles() {
        if (currentBackImage !== lastBackImage && currentBackImage != null && !isInFilesDir(currentBackImage!!)) {
            // delete last
            deleteLastBackImage()

            // copy current
            if (currentBackImage != null) {
                var outputStream: OutputStream? = null
                try {
                    val imagesDirectory =
                        File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
                    if (!imagesDirectory.exists()) imagesDirectory.mkdirs()
                    val newBackImage = File(imagesDirectory, currentBackImage?.name)
                    val mainKey = MasterKey.Builder(this)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    val encryptedFile = EncryptedFile.Builder(
                        this,
                        newBackImage,
                        mainKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build()
                    outputStream = encryptedFile.openFileOutput()
                    Utility.copyFile(FileInputStream(currentBackImage), outputStream)
                    if (!currentBackImage!!.delete()) {
                        //if (BuildConfig.DEBUG)
                        //    Log.e("EditCardActivity", "Could not delete back image file after copying it from cache to files");
                    }
                    currentBackImage = newBackImage
                } catch (e: IOException) {
                    /*
                    if (BuildConfig.DEBUG)
                        Log.e("EditCardActivity", "couldn't copy current back image file from cache to files directory");
                     */
                    throw AssertionError(e)
                } catch (e: GeneralSecurityException) {
                    throw AssertionError(e)
                } finally {
                    // flush and close the stream
                    if (outputStream != null) {
                        try {
                            outputStream.flush()
                            outputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun removeFrontImage() {
        if (currentFrontImage != null) {
            cardView.removeFrontImage()
            CardPreferenceManager.deleteFrontImage(this, ID)
            currentFrontImage = null
        }
    }

    private fun removeBackImage() {
        if (currentBackImage != null) {
            cardView.removeBackImage()
            CardPreferenceManager.deleteBackImage(this, ID)
            currentBackImage = null
        }
    }

    private fun deleteLastFrontImage() {
        if (lastFrontImage?.delete() == false) {
            /*
            if (BuildConfig.DEBUG)
                Log.e("EditCardActivity", "last front image file couldn't be deleted");
             */
        }
        lastFrontImage = null
    }

    private fun deleteLastBackImage() {
        if (lastBackImage?.delete() == false) {
            /*
            if (BuildConfig.DEBUG)
                Log.e("EditCardActivity", "last back image file couldn't be deleted");
             */
        }
        lastBackImage = null
    }
    // endregion
    

    // region helpers
    /**
     * Helper for [requestCancel]
     */
    private fun cancelDirectly() {
        if (currentFrontImage !== lastFrontImage)
            deleteFrontImage()
        if (currentBackImage !== lastBackImage)
            deleteBackImage()

        if (isCreateNewCardIntent) {
            setResult(RESULT_CANCELED)
            finish() // return to calling activity (do not launch ShowCardActivity)
        }
        else finishAndShowCard()
    }

    private fun finishAndShowCard() {
        finish() // finish first

        val intent = Intent(this, ShowCardActivity::class.java)
        intent.putExtra(EXTRA_CARD_ID, ID)
        startActivity(intent)
    }

    private fun finishAndReturnToHome() {
        finish()
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // will clear back stack until instance of HomeActivity // It looks like this is needed although HomeActivity is singleTop
        startActivity(intent)
    }

    private fun writeToPreferences() {
        moveFrontImageFromCacheToFiles()
        moveBackImageFromCacheToFiles()

        CardPreferenceManager.writeComplete(
            this,
            ID,
            cardName,
            cardColor,
            cardCreationDate, // TODO update (newCardIntent)
            cardAlterationDate,  // TODO update
            labels,
            code = cardCode ?: "",
            cardCodeType,
            cardCodeTypeText,
            currentFrontImage,
            currentBackImage,
            cardProperties
        )
    }

    private fun doesPropertyIDExist(propertyID: Int): Boolean {
        for (property in cardProperties) {
            if (propertyID == property.propertyID)
                return true
        }
        return false
    }

    private fun getCardPropertyIDs(): PreferenceArrayInt {
        // collect all property IDs from cardProperties list
        val array = PreferenceArrayInt()
        for (property in cardProperties)
            array.add(property.propertyID)

        return array
    }

    private fun getNewCardCodeTypeAdapter(): ArrayAdapter<String> {
        val codeValues = arrayOf(
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
            getString(R.string.card_code_type_value_code_93)
        )
        return ArrayAdapter(this, R.layout.dropdown_meu_popup_item, codeValues)
    }

    private fun hideCardCodeTypeAndTextLayout() {
        if (linearLayout.indexOfChild(cardCodeTypeAndTextLayout) != -1)
            linearLayout.removeView(cardCodeTypeAndTextLayout)
    }

    private fun showCardCodeTypeAndTextLayout() {
        if (linearLayout.indexOfChild(cardCodeTypeAndTextLayout) == -1)
            linearLayout.addView(cardCodeTypeAndTextLayout, linearLayout.indexOfChild(cardCodeInputLayout) + 1) // below cardCodeInputLayout
    }

    private fun createImageName(isFront: Boolean): String {
        return "JPEG_" +
                ID + "_" +
                if (isFront) "front" else "back" + "_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
    }

    private fun onPropertyRemoval() {
        hideKeyboard()
        // clear focus from all elements because otherwise the onFocusChangeListeners will get called with wrong or invalid getAdapterPosition()
        for (pos in cardProperties.indices) {
            val holder = propertiesRecyclerView.findViewHolderForAdapterPosition(pos) as? EditPropertyAdapter.ViewHolder
            holder?.clearFocus()
        }
        if (cardProperties.size == 1) {
            // the last item is going to be removed
            if (cardCodeTypeTextInput.visibility == VISIBLE)
                Utility.setImeOptionsAndRestart(cardCodeTypeTextInput, EditorInfo.IME_ACTION_DONE)
            else
                Utility.setImeOptionsAndRestart(cardCodeInputEditText, EditorInfo.IME_ACTION_DONE)
        }
        else {
            val lastViewHolder = propertiesRecyclerView.findViewHolderForAdapterPosition(cardProperties.size - 2) as? EditPropertyAdapter.ViewHolder
            lastViewHolder?.setImeOptions(EditorInfo.IME_ACTION_DONE)
        }
    }

    private fun generateNewCardID(): Int {
        val cardIDs: List<Int> = CardPreferenceManager.readAllIDs(this) // this is fine because there can't be an unsaved card with an unsaved ID at this moment
        return IDGenerator(cardIDs).generateID()
    }

    private fun makeMahlerCard() {
        if (!isMahlerCardInit && cardProperties.size == 0) {
            deleteFrontImage()

            val frontImageStream = resources.openRawResource(R.raw.front_mahler_image)

            // ensure cards images folder exists
            val cardsImagesFolder = File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
            if (!cardsImagesFolder.exists()) cardsImagesFolder.mkdirs()
            currentFrontImage = CreateExampleCardService.copyCardImage(
                this,
                frontImageStream,
                cardsImagesFolder,
                "JPEG_" + ID + "_" + getString(R.string.mahler_card_front_image_file_name)
            ) // The filename of Mahler card should not change in future versions of the app (It's checked by Utility.isMahlerFile(...))
            updateFrontImage()

            cardName = "Gustav Mahler"
            cardNameInputEditText.setText(cardName)

            cardColor = Color.rgb(221, 213, 177)
            updateColorButtonColor()

            addNewProperty()

            addNewProperty(
                name = getString(R.string.info),
                value = getString(R.string.mahler_is_cool),
                secret = false
            )

            addNewProperty(
                name = getString(R.string.important),
                value = getString(R.string.listen_to_mahler_9),
                secret = false
            )

            isMahlerCardInit = true
        }
    }
    // endregion
}
