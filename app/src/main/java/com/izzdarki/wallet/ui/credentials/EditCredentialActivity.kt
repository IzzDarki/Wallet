package com.izzdarki.wallet.ui.credentials

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.izzdarki.colorpickerview.dialog.ColorPickerDialogFragment
import izzdarki.wallet.R
import com.izzdarki.wallet.ui.adapters.EditFieldAdapter
import com.izzdarki.wallet.storage.AppSettingsStorage
import com.izzdarki.wallet.services.CreateExampleCredentialService
import com.izzdarki.wallet.ui.*
import com.izzdarki.wallet.ui.secondary.CodeScannerActivity
import com.izzdarki.wallet.ui.secondary.GetContentImageActivity
import com.izzdarki.wallet.ui.secondary.GetImageActivity
import com.izzdarki.wallet.ui.secondary.ImageCaptureActivity
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.utils.Utility
import com.izzdarki.wallet.utils.Utility.hideKeyboard
import com.izzdarki.editlabelscomponent.EditLabelsComponent
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.izzdarki.wallet.data.Barcode
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.logic.generateNewId
import com.izzdarki.wallet.services.ClearDirectoryService
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.storage.CredentialReadStorage
import com.izzdarki.wallet.storage.ImageStorage
import com.izzdarki.wallet.storage.ImageStorage.isInFilesDir
import com.izzdarki.wallet.utils.Utility.getAttributeColor
import com.izzdarki.wallet.utils.insertBitmapAt
import com.izzdarki.wallet.utils.updateBitmapAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*

class EditCredentialActivity
    : CredentialActivity(), ColorPickerDialogFragment.ColorPickerDialogListener {

    // UI
    private lateinit var credentialNameInputLayout: TextInputLayout
    private lateinit var credentialNameInputEditText: TextInputEditText
    private lateinit var barcodeInputLayout: TextInputLayout
    private lateinit var barcodeInputEditText: TextInputEditText
    private lateinit var barcodeDetailsLayout: LinearLayoutCompat
    private lateinit var barcodeTypeInput: MaterialAutoCompleteTextView
    private lateinit var barcodeShowTextInput: MaterialAutoCompleteTextView
    private lateinit var propertiesRecyclerView: RecyclerView
    private lateinit var addNewPropertyButton: MaterialButton
    private lateinit var credentialColorButton: MaterialButton
    private lateinit var credentialFrontImageButton: MaterialButton
    private lateinit var credentialBackImageButton: MaterialButton
    private lateinit var editLabelsComponent: EditLabelsComponent
    private lateinit var spaceForKeyboard: View

    // variables
    private var isCreateNewCredentialIntent = false
    private var isMahlerCardInit = false
    private var lastGetImageActivityImageIndex: Int? = null // used to determine which image to insert when returning from GetContentImageActivity

    // activity result launchers
    private val imageCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            handleImageCaptureResult(result)
        }
    private val getContentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            handleGetContentResult(result)
        }
    private val scanBarcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            handleScanBarcodeResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreateNewCredentialIntent = intent.getBooleanExtra(EXTRA_CREATE_NEW_CREDENTIAL, false)

        setContentView(R.layout.activity_edit_credential)

        // hooks
        cardViewLayout = findViewById(R.id.card_view_layout)
        scrollView = findViewById(R.id.scroll_view)
        linearLayout = findViewById(R.id.linear_layout)
        credentialNameInputLayout = findViewById(R.id.credential_name_layout)
        credentialNameInputEditText = findViewById(R.id.credential_name_input)
        barcodeInputLayout = findViewById(R.id.barcode_layout)
        barcodeInputEditText = findViewById(R.id.barcode_input)
        barcodeDetailsLayout = findViewById(R.id.barcode_details_layout)
        barcodeTypeInput = findViewById(R.id.barcode_type_input)
        barcodeShowTextInput = findViewById(R.id.barcode_show_text_input)
        propertiesRecyclerView = findViewById(R.id.recycler_view)
        addNewPropertyButton = findViewById(R.id.add_new_property_button)
        credentialColorButton = findViewById(R.id.credential_color_button)
        credentialFrontImageButton = findViewById(R.id.front_image_button)
        credentialBackImageButton = findViewById(R.id.back_image_button)
        spaceForKeyboard = findViewById(R.id.space_for_keyboard)
        editLabelsComponent = EditLabelsComponent(
            findViewById(R.id.credential_labels_chip_group),
            findViewById(R.id.credential_labels_add_chip),
            allLabels = CredentialPreferenceStorage.readAllLabels(this)
        )

        // toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // back pressed
        onBackPressedDispatcher.addCallback(owner = this, enabled = true) {
            requestCancel()
        }

        // init
        if (isCreateNewCredentialIntent) {
            initNewCredential() // create new credential
            supportActionBar!!.setTitle(R.string.new_entry)
        } else {
            initCredentialFromStorage() // load existing credential
            supportActionBar!!.setTitle(R.string.edit_entry)
        }

        // credential name
        credentialNameInputEditText.setText(credential.name)
        credentialNameInputEditText.addTextChangedListener(object : TextWatcher {
            // this callback is able to set the error immediately
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val newName = s.toString()
                if (newName == "")
                    credentialNameInputLayout.error = getString(R.string.entry_name_empty_error)
                else
                    credentialNameInputLayout.error = null
                credential.name = newName
            }
        })
        credentialNameInputEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                credential.name = credential.name.trim()
                credentialNameInputEditText.setText(credential.name)

                // Mahler easter egg
                if (isCreateNewCredentialIntent && credential.name == getString(R.string.mahler_is_cool))
                    makeMahlerCard()
            }
        }
        if (isCreateNewCredentialIntent) {
            // ! Keyboard doesn't show up. In EditPasswordActivity the exact same code works an keyboard shows up
            // When creating a new credential start with name selected
            credentialNameInputEditText.requestFocus()
            credentialNameInputEditText.selectAll()
        }

        // barcode
        barcodeInputEditText.setText(credential.barcode?.code ?: "")
        barcodeInputEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                credential.barcode = Barcode(
                    code = barcodeInputEditText.text.toString().trim(),
                    type = credential.barcode?.type ?: AppSettingsStorage.getDefaultBarcodeType(this),
                    showText = credential.barcode?.showText ?: AppSettingsStorage.getDefaultWithText(this)
                )
                barcodeInputEditText.setText(credential.barcode!!.code) // removes leading and trailing spaces
            }
        }
        barcodeInputEditText.addTextChangedListener(object : TextWatcher {
            // hide or show text fields for credential code type and credential code text type
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString() == "")
                    hideBarcodeDetailsLayout()
                else
                    showBarcodeDetailsLayout()
            }
        })
        barcodeInputLayout.setEndIconOnClickListener { scanQRCode() }

        // barcode type
        if (credential.barcode == null || credential.barcode?.code == "")
            hideBarcodeDetailsLayout()
        barcodeTypeInput.setText(codeTypeIntToString(this,
            credential.barcode?.type ?: AppSettingsStorage.getDefaultBarcodeType(this)
        ))
        barcodeTypeInput.setAdapter<ArrayAdapter<String>>(getNewBarcodeTypeAdapter())
        barcodeTypeInput.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus && credential.barcode != null)
                credential.barcode!!.type = barcodeTypeStringToInt(this, barcodeTypeInput.text.toString())
        }
        barcodeTypeInput.width = ((calculatedLayoutWidth - resources.getDimension(R.dimen.text_input_padding_bottom)) / 2).toInt()

        // barcode show text
        barcodeShowTextInput.setText(barcodeShowTextBoolToString(credential.barcode?.showText ?: AppSettingsStorage.getDefaultWithText(this)))
        barcodeShowTextInput.setAdapter(
            ArrayAdapter(
                this,
                R.layout.dropdown_meu_popup_item,
                arrayOf(
                    getString(R.string.barcode_show_text_value_true),
                    getString(R.string.barcode_show_text_value_false)
                )
            )
        )
        barcodeShowTextInput.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus && credential.barcode != null)
                credential.barcode!!.showText = barcodeShowTextToBoolean(barcodeShowTextInput.text.toString())
        }
        barcodeShowTextInput.width = ((calculatedLayoutWidth - resources.getDimension(R.dimen.text_input_padding_bottom)) / 2).toInt()

        // labels chip group
        editLabelsComponent.displayLabels(credential.labels)

        // create new property button
        addNewPropertyButton.setOnClickListener { addNewProperty() }

        // credential color
        credentialColorButton.setOnClickListener { pickColor() }
        updateColorButtonColor()

        // credential images
        credentialFrontImageButton.setOnClickListener {
            chooseImage(0)
        }

        credentialBackImageButton.setOnClickListener {
            chooseImage(1)
        }
        hideBackImageButtonIffNoFrontImage()

        // card view
        createCardView()
        cardView.frontText = getString(R.string.front_image)
        cardView.backText = getString(R.string.back_image)

        // recycler view
        propertiesRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = EditFieldAdapter(credential.fields) {
            onPropertyRemoval()
        }
        propertiesRecyclerView.adapter = adapter

        updateSpaceForKeyboard()
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


    // region main functions
    override fun initCredentialFromStorage() {
        super.initCredentialFromStorage()

        if (credential.fields.isEmpty()) {
            // set IME options on last input field to done
            if (barcodeInputEditText.visibility != View.GONE)
                Utility.setImeOptionsAndRestart(barcodeInputEditText, EditorInfo.IME_ACTION_DONE)
            else
                Utility.setImeOptionsAndRestart(barcodeShowTextInput, EditorInfo.IME_ACTION_DONE)
        }
    }

    private fun initNewCredential() {
        credential = Credential(
            id = generateNewId(CredentialPreferenceStorage.readAllIds(this)),
            name = getString(R.string.new_entry),
            labels = mutableSetOf(),
            barcode = null,
            color = getAttributeColor(R.attr.colorPrimaryContainer),
            creationDate = Calendar.getInstance().time,
            alterationDate = Calendar.getInstance().time,
            fields = mutableListOf(
                CredentialField(
                    name = getString(R.string.email_address),
                    value = "",
                    secret = false,
                ),
                CredentialField(
                    name = getString(R.string.password),
                    value = "",
                    secret = true,
                ),
                CredentialField(
                    name = getString(R.string.username),
                    value = "",
                    secret = false,
                ),
            ),
            imagePaths = mutableListOf()
        )
    }

    private fun addNewProperty(
        name: String = getString(R.string.new_field_name),
        value: String = "",
        secret: Boolean = false
    ) {
        credential.fields.add(
            CredentialField(name = name, value = value, secret = secret)
        )
        propertiesRecyclerView.adapter?.notifyItemInserted(credential.fields.size - 1)
        val secondLastViewHolder = propertiesRecyclerView.findViewHolderForAdapterPosition(
            credential.fields.size - 2) as? EditFieldAdapter.ViewHolder
        secondLastViewHolder?.setImeOptions(EditorInfo.IME_ACTION_NEXT)

        // set IME options on other input fields to next (only these 2 could be set to done)
        Utility.setImeOptionsAndRestart(barcodeInputEditText, EditorInfo.IME_ACTION_NEXT)
        Utility.setImeOptionsAndRestart(barcodeShowTextInput, EditorInfo.IME_ACTION_NEXT)
    }

    private fun saveAndShowCredential() {
        // When there are errors, saving will be aborted and toast will be shown to user
        if (!readAndCheckAllInput()) {
            Toast.makeText(this, R.string.there_are_still_errors, Toast.LENGTH_SHORT).show()
            return
        }

        // Delete old images from files directory
        val oldImages: Set<String> = CredentialPreferenceStorage.readCredential(this, credential.id)?.imagePaths?.toSet()
            ?: setOf() // if credential is new, there are no old images
        val removedImages = oldImages - credential.imagePaths.toSet()
        for (image in removedImages)
            File(image).delete()

        // Move new images from cache to files directory
        moveImagesFromCacheToFiles()

        // Write credential to storage
        CredentialPreferenceStorage.writeCredential(this, credential)

        // Finish and show credential
        finishAndShowCredential()
    }

    /**
     * If possible cancels the editing activity.
     * If there are errors in user input, a Toast is shown and the activity will not be canceled.
     * If the back confirm preference is true, an AlertDialog is shown and the activity will not be canceled.
     * @return `true` if the activity got finished, `false` otherwise
     */
    private fun requestCancel(): Boolean {
        // Helper function that implements the actual canceling
        fun cancelDirectly() {
            // New images are all in cache and will be deleted automatically by ClearDirectoryService
            if (isCreateNewCredentialIntent) {
                setResult(RESULT_CANCELED)
                finish() // return to calling activity (do not launch ShowCredentialActivity)
            }
            else finishAndShowCredential()

            // Start service to clear the cache directory (because the unencrypted images are stored there)
            ClearDirectoryService.enqueueWork(this, "$cacheDir/${getString(R.string.cards_images_folder_name)}")
        }

        readAndCheckAllInput() // Read input

        if (AppSettingsStorage.isBackConfirmNewCredential(this) && isCreateNewCredentialIntent
            || AppSettingsStorage.isBackConfirmEditCredential(this) && !isCreateNewCredentialIntent
                && CredentialPreferenceStorage.readCredential(this, credential.id) != credential
        ) // Check if credential has been modified
        {
            val dialogTitle: String
            val dialogMessage: String
            if (isCreateNewCredentialIntent) {
                dialogTitle = getString(R.string.cancel_create_new_entry_title)
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
        // Finish and return to home screen
        finish()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // will clear back stack until instance of HomeActivity // It looks like this is needed although HomeActivity is singleTop
        startActivity(intent)

        // Delete credential
        deleteCredentialWithImages(this, credential)
    }
    //endregion


    // region activity results
    private fun handleImageCaptureResult(result: ActivityResult) {
        if (lastGetImageActivityImageIndex == null)
            return
        when (result.resultCode) {
            RESULT_OK ->
                replaceImage(lastGetImageActivityImageIndex!!, result.data!!.getSerializableExtra(ImageCaptureActivity.EXTRA_RESULT_FILE) as File)

            ImageCaptureActivity.RESULT_NO_IMAGE_CAPTURE_INTENT ->
                Toast.makeText(this, R.string.no_image_capture_intent, Toast.LENGTH_SHORT).show()

            ImageCaptureActivity.RESULT_PERMISSION_DENIED ->
                Toast.makeText(this, R.string.image_capture_camera_permission_denied, Toast.LENGTH_SHORT).show()

            RESULT_CANCELED -> {}

            else ->
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGetContentResult(result: ActivityResult) {
        if (lastGetImageActivityImageIndex == null)
            return
        when (result.resultCode) {
            RESULT_OK ->
                replaceImage(lastGetImageActivityImageIndex!!, result.data!!.getSerializableExtra(GetContentImageActivity.EXTRA_RESULT_FILE) as File)

            GetContentImageActivity.RESULT_NO_GET_CONTENT_INTENT ->
                Toast.makeText(this, R.string.no_get_content_intent, Toast.LENGTH_SHORT).show()

            RESULT_CANCELED -> {}

            else -> Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleScanBarcodeResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val barcodeFormat = result.data!!.getSerializableExtra(CodeScannerActivity.EXTRA_RESULT_CODE_TYPE) as BarcodeFormat
            val codeType = try {
                barcodeFormatToInt(barcodeFormat)
            } catch (e: IllegalStateException) {
                Toast.makeText(this, R.string.unsupported_type_barcode, Toast.LENGTH_LONG).show()
                return
            }
            val code = result.data!!.getStringExtra(CodeScannerActivity.EXTRA_RESULT_CODE) ?: ""
            credential.barcode = Barcode(
                code,
                codeType,
                credential.barcode?.showText ?: AppSettingsStorage.getDefaultWithText(this)
            )
            barcodeInputEditText.setText(code)
            barcodeTypeInput.setText(codeTypeIntToString(this, codeType))
            barcodeTypeInput.setAdapter<ArrayAdapter<String>>(getNewBarcodeTypeAdapter()) // reset the adapter (otherwise there are filters)
        }
        else if (result.resultCode == CodeScannerActivity.RESULT_PERMISSION_DENIED)
            Toast.makeText(this, R.string.code_scanner_camera_permission_denied, Toast.LENGTH_SHORT).show()
    }

    // endregion


    // region action bar menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_activity_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.edit_action_bar_done) {  // finish if input can be saved (no input errors)
            saveAndShowCredential()
            return true
        }
        else if (item.itemId == R.id.edit_action_bar_delete) {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_entry)
                .setMessage(R.string.delete_entry_dialog_message)
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
    // endregion


    // region start other activities
    private fun chooseImage(index: Int) {
        if (index >= 2 || index < 0)
            return // UI currently only supports 2 images

        hideKeyboard()
        val dialog: AlertDialog
        val builder = AlertDialog.Builder(this)

        val dialogLayout = View.inflate(this, R.layout.image_chooser_dialog, null)
        val deleteImageGroup = dialogLayout.findViewById<ViewGroup>(R.id.image_chooser_dialog_delete)
        val selectImageGroup = dialogLayout.findViewById<ViewGroup>(R.id.image_chooser_dialog_storage)
        val imageCaptureGroup = dialogLayout.findViewById<ViewGroup>(R.id.image_chooser_dialog_camera)

        // Hide delete group if no image set
        if (index >= credential.imagePaths.size)
            deleteImageGroup.visibility = View.GONE

        builder.setView(dialogLayout)
        dialog = builder.create()
        deleteImageGroup.setOnClickListener {
            dialog.dismiss()
            removeImageAndUpdateCardView(index)
        }
        selectImageGroup.setOnClickListener {
            dialog.dismiss()
            selectImage(index)
        }
        imageCaptureGroup.setOnClickListener {
            dialog.dismiss()
            takeImage(index)
        }
        dialog.show()
    }

    private fun takeImage(imageIndex: Int) {
        val imageCaptureIntent = Intent(this, ImageCaptureActivity::class.java)
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY,
            getString(R.string.fileprovider_authority)
        )
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_FOLDER_PATH,
            cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name)
        )
        imageCaptureIntent.putExtra(GetImageActivity.EXTRA_FILE_NAME, createImageName())
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE,
            calculatedLayoutWidth.toInt()
        )
        imageCaptureIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE,
            calculatedLayoutHeight.toInt()
        )

        lastGetImageActivityImageIndex = imageIndex
        imageCaptureLauncher.launch(imageCaptureIntent)
    }

    private fun selectImage(imageIndex: Int) {
        val getContentIntent = Intent(this, GetContentImageActivity::class.java)
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY,
            getString(R.string.fileprovider_authority)
        )
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_FOLDER_PATH,
            cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name)
        )
        getContentIntent.putExtra(GetImageActivity.EXTRA_FILE_NAME, createImageName())
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE,
            calculatedLayoutWidth.toInt()
        )
        getContentIntent.putExtra(
            GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE,
            calculatedLayoutHeight.toInt()
        )
        getContentIntent.putExtra(GetContentImageActivity.EXTRA_TYPE, "image/*")

        lastGetImageActivityImageIndex = imageIndex
        getContentLauncher.launch(getContentIntent)
    }

    private fun scanQRCode() {
        val scanQRCodeIntent = Intent(this, CodeScannerActivity::class.java)
        scanBarcodeLauncher.launch(scanQRCodeIntent)
    }
    // endregion


    // region color and color picker
    private fun pickColor() {
        val dialogFragment = ColorPickerDialogFragment.newInstance(
            PICK_COLOR_DIALOG_ID,
            getString(R.string.select_color),
            null,
            credential.color,
            false
        )
        dialogFragment.setCustomButton(resources.getString(R.string.auto_color_button_text)) {
            autoSelectColor()
            dialogFragment.setColor(credential.color)
        }
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, 0)
        dialogFragment.show(supportFragmentManager, "d")
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        if (dialogId == PICK_COLOR_DIALOG_ID) {
            credential.color = color
            updateColorButtonColor()
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}

    private fun updateColorButtonColor() {
        credentialColorButton.setBackgroundColor(credential.color)
        if (Utility.isColorDark(credential.color))
            credentialColorButton.setTextColor(ResourcesCompat.getColor(resources, R.color.on_dark_text_color,theme))
        else
            credentialColorButton.setTextColor(ResourcesCompat.getColor(resources, R.color.on_light_text_color, theme))

        if (Utility.areColorsSimilar(
                Utility.getDefaultBackgroundColor(this),
                credential.color
            )
        ) {
            // draw outline
            credentialColorButton.strokeWidth = resources.getDimension(R.dimen.outline_for_similar_colors_stroke_width).toInt()
            credentialColorButton.strokeColor = ColorStateList.valueOf(getColor(R.color.card_view_outline_color))
        } else {
            // remove outline
            credentialColorButton.strokeWidth = 0
        }
    }

    private fun autoSelectColor() {
        if (cardView.frontImage != null && cardView.backImage != null) {
            @ColorInt val color1 = Utility.getAverageColorRGB(cardView.frontImage)
            @ColorInt val color2 = Utility.getAverageColorRGB(cardView.backImage)
            credential.color = Utility.getAverageColorARGB(color1, color2)
        }
        else if (cardView.frontImage != null)
            credential.color = Utility.getAverageColorRGB(cardView.frontImage)
        else if (cardView.backImage != null)
            credential.color = Utility.getAverageColorRGB(cardView.backImage)
        else
            Toast.makeText(this, R.string.no_images_to_calc_color, Toast.LENGTH_SHORT).show()
    }
    // endregion


    // region read and check input fields
    /**
     * Reads all the user input
     * @return true if there are no errors, false otherwise
     */
    private fun readAndCheckAllInput(): Boolean {
        credential.name = credentialNameInputEditText.text.toString().trim()
        if (credentialNameInputLayout.error != null)
            return false

        credential.labels = editLabelsComponent.currentLabels.toSortedSet(CredentialReadStorage.labelComparator)

        credential.barcode = Barcode(
            code = barcodeInputEditText.text.toString().trim(),
            type = barcodeTypeStringToInt(this, barcodeTypeInput.text.toString()),
            showText = barcodeShowTextToBoolean(barcodeShowTextInput.text.toString())
        )

        readAllProperties()

        return true
    }


    /**
     * Read all properties
     */
    private fun readAllProperties() {
        for (position in credential.fields.indices) {
            val holder = propertiesRecyclerView.findViewHolderForAdapterPosition(position) as EditFieldAdapter.ViewHolder?
            holder?.readPropertyValue()
        }
    }
    // endregion
    

    // region image helpers
    private fun replaceImage(index: Int, imageFile: File) {
        removeImageAndUpdateCardView(index) // remove old image (instead of removing and inserting, one could also just replace)
        credential.imagePaths.add(index, imageFile.absolutePath)

        // Update UI
        lifecycleScope.launch(Dispatchers.Main) {
            val bitmap = ImageStorage.decodeImage(this@EditCredentialActivity, imageFile)
                .getOrElse { throwable ->
                    showDecodingErrorToast(throwable)
                    removeImageAndUpdateCardView(index)
                    return@launch
                }

            // Update UI on main thread
            withContext(Dispatchers.Main) {
                cardView.insertBitmapAt(index, bitmap, totalImagesBefore = credential.imagePaths.size) // insert new image
                hideBackImageButtonIffNoFrontImage()
                updateSpaceForKeyboard()
            }
        }
    }

    /**
     * Moves all `credential.imagePaths` from cache (not encrypted) to files directory (encrypted).
     * Images that are already in files directory are not moved.
     * Also updates `credential.imagePaths` to new paths.
     */
    private fun moveImagesFromCacheToFiles() {
        val imagesToMove = credential.imageFiles
            .withIndex()
            .filter { (_, file) -> !isInFilesDir(this, file) }

        for ((index, oldFile) in imagesToMove) {
            var outputStream: OutputStream? = null
            var newImage: File? = null
            try {
                // Copy old file in cache directory to a new encrypted file in files directory
                if (!imagesDir.exists()) imagesDir.mkdirs()
                newImage = File(imagesDir, oldFile.name)
                val mainKey = MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val encryptedFile = EncryptedFile.Builder(
                    this,
                    newImage,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                outputStream = encryptedFile.openFileOutput()
                // outputStream = newImage.outputStream() // Unencrypted file storage ONLY for debugging
                Utility.copyFile(FileInputStream(oldFile), outputStream)

                // Deleting old file is not necessary because files in cache are deleted regularly

                // Update credential.imagePaths
                credential.imagePaths[index] = newImage.absolutePath

            } catch (e: IOException) {
                // Remove image and show toast
                Toast.makeText(this, String.format(getString(R.string.image_x_could_not_be_saved), index), Toast.LENGTH_SHORT).show()
                newImage?.delete() // delete new image if it was created
                removeImageAndUpdateCardView(index)
            } catch (e: GeneralSecurityException) {
                // Remove image and show toast
                Toast.makeText(this, String.format(getString(R.string.image_x_could_not_be_saved), index), Toast.LENGTH_SHORT).show()
                newImage?.delete() // delete new image if it was created
                removeImageAndUpdateCardView(index)
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

        // Start service to clear the cache directory (because the unencrypted images are stored there)
        ClearDirectoryService.enqueueWork(this, "$cacheDir/${getString(R.string.cards_images_folder_name)}")
    }

    /**
     * If no images are there, extra space is added so that the keyboard does not cover
     * editable input fields
     */
    private fun updateSpaceForKeyboard() {
        if (credential.imagePaths.isEmpty())
            spaceForKeyboard.visibility = View.INVISIBLE  // invisible but takes space
        else
            spaceForKeyboard.visibility = View.GONE
    }
    // endregion
    

    // region helpers
    private fun finishAndShowCredential() {
        finish() // finish first

        val intent = Intent(this, ShowCredentialActivity::class.java)
        intent.putExtra(EXTRA_CREDENTIAL_ID, credential.id)
        startActivity(intent)
    }

    override fun removeImageAndUpdateCardView(index: Int) {
        super.removeImageAndUpdateCardView(index)
        hideBackImageButtonIffNoFrontImage()
    }

    override fun showDecodingErrorToast(throwable: Throwable?, additionalMessage: String) {
        // Add image will be removed message in edit activity
        val removeImageMessage = getString(R.string.the_image_will_be_removed_when_you_save_the_entry)
        val beforeAdditionalMessage =
            if (additionalMessage.isNotEmpty()) " "
            else ""
        super.showDecodingErrorToast(throwable, additionalMessage = removeImageMessage + beforeAdditionalMessage + additionalMessage)
    }

    private fun getNewBarcodeTypeAdapter(): ArrayAdapter<String> {
        val codeValues = arrayOf(
            getString(R.string.barcode_type_value_qr),
            getString(R.string.barcode_type_value_code_39),
            getString(R.string.barcode_type_value_code_128),
            getString(R.string.barcode_type_value_upc_a),
            getString(R.string.barcode_type_value_ean_13),
            getString(R.string.barcode_type_value_ean_8),
            getString(R.string.barcode_type_value_itf),
            getString(R.string.barcode_type_value_upc_e),
            getString(R.string.barcode_type_value_data_matrix),
            getString(R.string.barcode_type_value_pdf_417),
            getString(R.string.barcode_type_value_aztec),
            getString(R.string.barcode_type_value_codabar),
            getString(R.string.barcode_type_value_code_93)
        )
        return ArrayAdapter(this, R.layout.dropdown_meu_popup_item, codeValues)
    }

    private fun hideBarcodeDetailsLayout() {
        if (linearLayout.indexOfChild(barcodeDetailsLayout) != -1)
            linearLayout.removeView(barcodeDetailsLayout)
    }

    private fun showBarcodeDetailsLayout() {
        if (linearLayout.indexOfChild(barcodeDetailsLayout) == -1)
            linearLayout.addView(barcodeDetailsLayout, linearLayout.indexOfChild(barcodeInputLayout) + 1) // below barcodeInputLayout
    }

    private fun createImageName(): String {
        return "${credential.id}_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
    }

    private fun onPropertyRemoval() {
        hideKeyboard()
        // clear focus from all elements because otherwise the onFocusChangeListeners will get called with wrong or invalid getAdapterPosition()
        for (pos in credential.fields.indices) {
            val holder = propertiesRecyclerView.findViewHolderForAdapterPosition(pos) as? EditFieldAdapter.ViewHolder
            holder?.clearFocus()
        }
        if (credential.fields.size == 1) { // the last item is going to be removed
            if (barcodeShowTextInput.visibility == View.VISIBLE)
                Utility.setImeOptionsAndRestart(barcodeShowTextInput, EditorInfo.IME_ACTION_DONE)
            else
                Utility.setImeOptionsAndRestart(barcodeInputEditText, EditorInfo.IME_ACTION_DONE)
        }
        else {
            val lastViewHolder = propertiesRecyclerView.findViewHolderForAdapterPosition(credential.fields.size - 2) as? EditFieldAdapter.ViewHolder
            lastViewHolder?.setImeOptions(EditorInfo.IME_ACTION_DONE)
        }
    }

    private fun hideBackImageButtonIffNoFrontImage() {
        credentialBackImageButton.visibility =
            if (credential.imagePaths.isEmpty()) View.GONE
            else View.VISIBLE
    }

    private fun makeMahlerCard() {
        // Mahler easter egg
        if (!isMahlerCardInit) {
            val mahlerImageStream = resources.openRawResource(R.raw.front_mahler_image)

            // ensure cards images folder exists
            val cacheCardsImagesFolder = File(cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name))
            if (!cacheCardsImagesFolder.exists()) cacheCardsImagesFolder.mkdirs()

            // Create file
            val mahlerFileName = "${credential.id}_" + getString(R.string.mahler_card_front_image_file_name)
            val mahlerFile = CreateExampleCredentialService.copyImage(
                this,
                mahlerImageStream,
                cacheCardsImagesFolder,
                mahlerFileName
            ) ?: return

            // Set image to credential
            val oldImageCount = credential.imagePaths.size
            credential.imagePaths.clear()
            credential.imagePaths.add(mahlerFile.absolutePath)

            // Update images in UI
            for (i in 1 until oldImageCount) { // Remove all images except the first one
                cardView.updateBitmapAt(i, null)
            }
            lifecycleScope.launch(Dispatchers.IO) { // Asynchronously load image
                val bitmap = ImageStorage.decodeImage(this@EditCredentialActivity, credential.imageFiles[0])
                    .getOrElse { throwable ->
                        showDecodingErrorToast(throwable)
                        removeImageAndUpdateCardView(0)
                        null
                    }

                // Update UI on main thread
                withContext(Dispatchers.Main){
                    cardView.updateBitmapAt(0, bitmap)
                    hideBackImageButtonIffNoFrontImage()
                    updateSpaceForKeyboard() // Don't know if this is needed, but it doesn't hurt
                }
            }


            credential.name = "Gustav Mahler"
            credentialNameInputEditText.setText(credential.name)

            credential.color = Color.rgb(221, 213, 177)
            updateColorButtonColor()

            credential.fields.clear()
            propertiesRecyclerView.adapter?.notifyDataSetChanged()

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

    private val imagesDir get() = File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
    // endregion

    companion object {
        const val EXTRA_CREATE_NEW_CREDENTIAL = "com.izzdarki.wallet.edit_card_activity.create_new_card"
        const val PICK_COLOR_DIALOG_ID = 0
    }
}
