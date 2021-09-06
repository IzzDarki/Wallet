package com.bennet.wallet.activities.passwords

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.allViews
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bennet.colorpickerview.dialog.ColorPickerDialogFragment
import com.bennet.wallet.R
import com.bennet.wallet.activities.HomeActivity
import com.bennet.wallet.adapters.EditPropertyAdapter
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.PasswordPreferenceManager
import com.bennet.wallet.utils.ItemProperty
import com.bennet.wallet.utils.Utility
import com.bennet.wallet.utils.Utility.IDGenerator
import com.bennet.wallet.utils.Utility.PreferenceArrayInt
import com.bennet.wallet.utils.Utility.PreferenceArrayString
import com.bennet.wallet.utils.Utility.hideKeyboard
import com.bennet.wallet.utils.Utility.isViewHitByTouchEvent
import com.bennet.wallet.utils.Utility.setImeOptionsAndRestart
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class EditPasswordActivity
    : AppCompatActivity(), ColorPickerDialogFragment.ColorPickerDialogListener {

    // region static cast val
    companion object {
        const val EXTRA_PASSWORD_ID = ShowPasswordActivity.EXTRA_PASSWORD_ID // int
        const val EXTRA_CREATE_NEW_PASSWORD = "edit_password.create_new_password" // boolean
        const val PICK_COLOR_DIALOG_ID = 0
    }
    // endregion


    // region UI
    private lateinit var mainLinearLayout: LinearLayoutCompat
    private lateinit var labelsChipGroup: ChipGroup
    private lateinit var labelsAddChip: Chip
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var nameInputEditText: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInputEditText: TextInputEditText
    private lateinit var passwordColorButton: MaterialButton
    private lateinit var propertiesRecyclerView: RecyclerView
    private lateinit var createNewPasswordPropertyButton: LinearLayoutCompat
    // endregion


    // region password properties
    private var ID = 0
    private lateinit var passwordName: String
    private lateinit var passwordValue: String
    private lateinit var passwordLabels: PreferenceArrayString // will not be kept up to date (only readAndCheckLabels updates passwordLabels)
    private var passwordColor: Int = 0
    private var passwordProperties: MutableList<ItemProperty> = mutableListOf()
    // endregion


    // region variables
    private var hasBeenModified = false
    private var isCreateNewPasswordIntent = false
    // endregion


    // region overrides
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_password)

        // hooks
        mainLinearLayout = findViewById(R.id.edit_password_main_linear_layout)
        labelsChipGroup = findViewById(R.id.edit_password_labels_chip_group)
        labelsAddChip = findViewById(R.id.edit_password_labels_add_chip)
        nameInputLayout = findViewById(R.id.edit_password_name_input_layout)
        nameInputEditText = findViewById(R.id.edit_password_name_edit_text)
        passwordInputLayout = findViewById(R.id.edit_password_password_input_layout)
        passwordInputEditText = findViewById(R.id.edit_password_password_edit_text)
        passwordColorButton = findViewById(R.id.edit_password_color_button)
        propertiesRecyclerView = findViewById(R.id.edit_password_recycler_view)
        createNewPasswordPropertyButton = findViewById(R.id.edit_password_create_new_password_property_button)

        // toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // init
        isCreateNewPasswordIntent = intent.getBooleanExtra(EXTRA_CREATE_NEW_PASSWORD, false)
        if (isCreateNewPasswordIntent) {
            initNewPassword()
            supportActionBar?.setTitle(R.string.new_password)
        }
        else {
            initFromPreferences()
            supportActionBar?.setTitle(R.string.edit_password)
        }

        // name input
        nameInputEditText.setText(passwordName)
        nameInputEditText.addTextChangedListener(object : TextWatcher {
            // this callback is able to set the error immediately
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val newName = s.toString()
                if (newName == "")
                    nameInputLayout.error = getString(R.string.password_name_empty_error)
                else
                    nameInputLayout.error = null
                passwordName = newName
            }
        })
        nameInputEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                passwordName = passwordName.trim()
                nameInputEditText.setText(passwordName)
            }
        }

        // password value input
        passwordInputEditText.setText(passwordValue)
        passwordInputEditText.inputType = Utility.inputTypeTextHiddenPassword // works better than xml "textPassword" (monospace + keyboard)
        passwordInputEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                passwordValue = passwordInputEditText.text.toString().trim { it <= ' ' }
                passwordInputEditText.setText(passwordValue)
                passwordInputEditText.inputType = Utility.inputTypeTextHiddenPassword
            }
        }


        // labels chip group
        updateLabelViews()

        // labels add chip
        labelsAddChip.setOnClickListener {
            addNewLabel()
        }

        // create new password property button
        createNewPasswordPropertyButton.setOnClickListener { addNewProperty() }

        // password color
        passwordColorButton.setOnClickListener { pickColor() }
        updateColorButtonColor()

        // recycler view
        propertiesRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = EditPropertyAdapter(passwordProperties) {
            onPropertyRemoval()
        }
        propertiesRecyclerView.adapter = adapter
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Every touch event goes through this function
        // This function finishes editing of a label in certain situations

        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {

            // Check if touch event hits the edited label => don't finish editing it (the user wants to interact with the edited label)
            val editText = getEditTextFromChipGroup()
                ?: return super.dispatchTouchEvent(ev) // if there is no EditText, touch events can be dispatched as usual

            if (!isViewHitByTouchEvent(editText, ev)) {
                getEditTextHitByTouchEvent(ev)?.requestFocus() // request focus to EditText if the touch event hits any EditText (before the focus gets cleared by finishEditingChip)
                finishEditingChip(editText)
            }

            // Check if touch event hits one of the chips => consume the touch event
            for (view in labelsChipGroup.allViews) {
                if (view is Chip && isViewHitByTouchEvent(view, ev)) {
                    return true // consume the touch event (finishing editing while also triggering other chip related UI is too much for a single touch)
                }
            }
        }
        return super.dispatchTouchEvent(ev) // dispatch touch event as usual
    }
    // endregion


    // region main functions
    private fun initFromPreferences() {
        ID = intent.getIntExtra(EXTRA_PASSWORD_ID, -1)
        check(ID != -1) { "PasswordActivity: missing intent extra: ID" }
        passwordName = PasswordPreferenceManager.readPasswordName(this, ID)
        passwordValue = PasswordPreferenceManager.readPasswordValue(this, ID)
        passwordLabels = PasswordPreferenceManager.readPasswordLabels(this, ID)
        passwordColor = PasswordPreferenceManager.readPasswordColor(this, ID)

        val passwordPropertyIDs: List<Int> = PasswordPreferenceManager.readPasswordPropertyIds(this, ID)
        for (propertyID in passwordPropertyIDs) {
            passwordProperties.add(ItemProperty(
                propertyID = propertyID,
                name = PasswordPreferenceManager.readPasswordPropertyName(this, ID, propertyID),
                value = PasswordPreferenceManager.readPasswordPropertyValue(this, ID, propertyID),
                secret = PasswordPreferenceManager.readPasswordPropertySecret(this, ID, propertyID),
            ))
        }
        if (passwordPropertyIDs.isEmpty())
            passwordInputEditText.imeOptions =  EditorInfo.IME_ACTION_DONE
    }

    private fun initNewPassword() {
        ID = generateNewPasswordID()
        passwordName = getString(R.string.new_password)
        passwordValue = "" // init as empty
        passwordLabels = PreferenceArrayString()
        passwordColor = resources.getColor(R.color.card_default_color)

        // init default properties
        passwordProperties.add(
            ItemProperty(
                name = getString(R.string.username),
                value = "",
                secret = false,
                propertiesNeededToCreateNewID = passwordProperties
            )
        )
        // password username
        passwordProperties.add(
            ItemProperty(
                name = getString(R.string.email_address),
                value = "",
                secret = false,
                propertiesNeededToCreateNewID = passwordProperties
            )
        ) // email address
    }

    private fun addNewProperty() {
        passwordProperties.add(
            ItemProperty(
                name = getString(R.string.new_password_property_name),
                value = "",
                secret = false,
                propertiesNeededToCreateNewID = passwordProperties
            )
        )
        propertiesRecyclerView.adapter?.notifyItemInserted(passwordProperties.size - 1)
        val secondLastViewHolder = propertiesRecyclerView.findViewHolderForAdapterPosition(passwordProperties.size - 2) as? EditPropertyAdapter.ViewHolder
        secondLastViewHolder?.setImeOptions(EditorInfo.IME_ACTION_NEXT)

        setImeOptionsAndRestart(passwordInputEditText, EditorInfo.IME_ACTION_NEXT)
    }

    private fun addNewLabel() {
        addEditTextToLabels("New label")
    }

    private fun saveAndShowPassword() {
        val propertyIDs: List<Int> = PasswordPreferenceManager.readPasswordPropertyIds(this, ID)

        // Check if the password was modified (hasBeenModified will be set true)
        if (!readAndCheckAllInput(propertyIDs)) {
            // When there are errors, saving will be aborted and Toast will be shown to user
            Toast.makeText(this, R.string.there_are_still_errors, Toast.LENGTH_SHORT).show()
            return
        }
        writeToPreferences()
        finishAndShowPassword()
    }

    /**
     * If possible cancels the editing activity.
     * If there are errors in user input, a Toast is shown and the activity will not be canceled.
     * If the back confirm preference is true, an AlertDialog is shown and the activity will not be canceled.
     * @return true if the activity got finished, false otherwise
     */
    private fun requestCancel(): Boolean {
        readAndCheckAllInput(
            PasswordPreferenceManager.readPasswordPropertyIds(this, ID)
        ) // Read input to figure out if password has been modified (member variable hasBeenModified)

        if (AppPreferenceManager.isBackConfirmNewCardOrPassword(this) && isCreateNewPasswordIntent
            || AppPreferenceManager.isBackConfirmEditCardOrPassword(this) && !isCreateNewPasswordIntent && hasBeenModified) {

            val dialogTitle: String
            val dialogMessage: String

            if (isCreateNewPasswordIntent) {
                dialogTitle = getString(R.string.cancel_create_new_password_title)
                dialogMessage = getString(R.string.nothing_will_be_saved)
            }
            else {
                dialogTitle = getString(R.string.discard_changes)
                dialogMessage = getString(R.string.changes_are_not_saved)
            }

            AlertDialog.Builder(ContextThemeWrapper(this, R.style.RoundedCornersDialog))
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

    private fun deleteAndReturnToHome() {
        PasswordPreferenceManager.removePassword(this, ID)
        finishAndReturnToHome()
    }
    // endregion


    // region action bar and menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_activity_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.edit_action_bar_done) {
            saveAndShowPassword()
            return true
        }
        else if (item.itemId == R.id.edit_action_bar_delete) {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_password)
                .setMessage(R.string.delete_password_dialog_message)
                .setCancelable(true)
                .setPositiveButton(R.string.delete) { _, _ ->
                    deleteAndReturnToHome()
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


    // region color and color picker
    private fun pickColor() {
        val dialogFragment = ColorPickerDialogFragment.newInstance(
            PICK_COLOR_DIALOG_ID,
            null,
            null,
            passwordColor,
            false
        )
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, 0)
        dialogFragment.show(supportFragmentManager, "d")
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        if (dialogId == PICK_COLOR_DIALOG_ID) {
            hasBeenModified = true
            passwordColor = color
            updateColorButtonColor()
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}

    private fun updateColorButtonColor() {
        passwordColorButton.setBackgroundColor(passwordColor)
        if (Utility.isColorDark(passwordColor))
            passwordColorButton.setTextColor(resources.getColor(R.color.on_dark_text_color))
        else
            passwordColorButton.setTextColor(resources.getColor(R.color.on_light_text_color))
    }
    // endregion


    // region helpers
    private fun cancelDirectly() {
        if (isCreateNewPasswordIntent) {
            finish()
            setResult(RESULT_CANCELED)
        }
        else finishAndShowPassword()
    }

    private fun finishAndShowPassword() {
        finish() // finish first
        val intent = Intent(this, ShowPasswordActivity::class.java)
        intent.putExtra(ShowPasswordActivity.EXTRA_PASSWORD_ID, ID)
        startActivity(intent)
    }

    private fun finishAndReturnToHome() {
        finish()
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // will clear back stack until instance of HomeActivity // It looks like this is needed although HomeActivity is singleTop
        startActivity(intent)
    }

    private fun writeToPreferences() {
        PasswordPreferenceManager.addToAllPasswordIDs(this, ID) // This should be fine. At this moment no other process should modify this preference list (Note that this only adds the ID if it is not yet contained in the list)
        PasswordPreferenceManager.writePasswordName(this, ID, passwordName)
        PasswordPreferenceManager.writePasswordValue(this, ID, passwordValue)
        PasswordPreferenceManager.writePasswordLabels(this, ID, passwordLabels)
        PasswordPreferenceManager.writePasswordColor(this, ID, passwordColor)

        val currentPropertyIDs = PreferenceArrayInt() // Collects all current propertyIDs to write into preferences
        PasswordPreferenceManager.removePasswordProperties(this, ID)
        for (property in passwordProperties) {
            currentPropertyIDs.add(property.propertyID)
            PasswordPreferenceManager.writePasswordPropertyName(this, ID, property.propertyID, property.name)
            PasswordPreferenceManager.writePasswordPropertyValue(this, ID, property.propertyID, property.value)
            PasswordPreferenceManager.writePasswordPropertySecret(this, ID, property.propertyID, property.secret)
        }
        PasswordPreferenceManager.writePasswordPropertyIds(this, ID, currentPropertyIDs)
    }

    private fun onPropertyRemoval() {
        hideKeyboard()
        // clear focus from all elements because otherwise the onFocusChangeListeners will get called with wrong or invalid getAdapterPosition()
        for (pos in passwordProperties.indices) {
            val holder = propertiesRecyclerView.findViewHolderForAdapterPosition(pos) as? EditPropertyAdapter.ViewHolder?
            holder?.clearFocus()
        }
        if (passwordProperties.size == 1)
            setImeOptionsAndRestart(passwordInputEditText, EditorInfo.IME_ACTION_DONE)
        else {
            val lastViewHolder = propertiesRecyclerView.findViewHolderForAdapterPosition(passwordProperties.size - 2) as? EditPropertyAdapter.ViewHolder
            lastViewHolder?.setImeOptions(EditorInfo.IME_ACTION_DONE)
        }
    }

    private fun updateLabelViews() {
        for (label in passwordLabels) {
            addChipToLabels(label)
        }
    }

    private fun addChipToLabels(text: String, index: Int = 0) {
        val chip = Chip(this)
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            labelsChipGroup.removeView(chip)
        }
        chip.setOnLongClickListener {
            startEditingChip(chip)
            return@setOnLongClickListener true // consumed long click
        }
        labelsChipGroup.addView(chip, index)
    }

    private fun addEditTextToLabels(text: String, index: Int = 0) {
        val editText = AutoCompleteTextView(this)
        editText.isSingleLine = true
        editText.setText(text)
        editText.setSelectAllOnFocus(true)
        editText.imeOptions = EditorInfo.IME_ACTION_NEXT
        editText.setOnEditorActionListener { _, _, _ ->
            passwordInputEditText.requestFocus() // focus should jump to the next field when pressing done
            // when action (done) triggered, finish editing
            finishEditingChip(editText)
            return@setOnEditorActionListener true // consumed the action
        }

        labelsChipGroup.addView(editText, index)
        editText.requestFocus()
        Utility.showKeyboard(editText)
    }

    private fun startEditingChip(chip: Chip) {
        val index = labelsChipGroup.indexOfChild(chip)
        labelsChipGroup.removeView(chip)
        addEditTextToLabels(chip.text.toString(), index)
    }

    private fun finishEditingChip(editText: AutoCompleteTextView) {
        // clear focus and remove editText
        editText.clearFocus()
        val index = labelsChipGroup.indexOfChild(editText)
        labelsChipGroup.removeView(editText)

        editText.setText(editText.text.toString().trim())

        val newLabel = editText.text.toString()
        if (newLabel == "") {
            Toast.makeText(this, R.string.error_label_cant_be_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (newLabel.contains(Utility.PreferenceArray.DEFAULT_SEPARATOR)) {
            val errorMessage = String.format(
                getString(R.string.error_label_cant_contain_x),
                Utility.PreferenceArray.DEFAULT_SEPARATOR
            )
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            return
        }
        if (newLabel in getAllLabels()) {
            Toast.makeText(this, R.string.error_label_already_added, Toast.LENGTH_SHORT).show()
            return
        }

        // label is ok => add it as chip
        addChipToLabels(editText.text.toString(), index)
    }

    /**
     * Reads all the user input and checks if the password has been modified ([.hasBeenModified] will be set true)
     * @param propertyIDs List of all property Ids
     * @return true if there are no errors, false otherwise
     */
    private fun readAndCheckAllInput(propertyIDs: List<Int>): Boolean {
        readAndCheckPasswordValue()
        readAndCheckLabels()
        readAndCheckAllProperties()
        checkIfPropertyWasRemoved(propertyIDs)
        return readAndCheckName()
    }

    /**
     * Checks if there are errors in the name input.
     * Updates [.hasBeenModified]
     * @return true if there are no errors in the users input, false otherwise
     */
    private fun readAndCheckName(): Boolean {
        passwordName = nameInputEditText.text.toString().trim()
        if (nameInputLayout.error != null)
            return false
        if (passwordName != PasswordPreferenceManager.readPasswordName(this, ID))
            hasBeenModified = true
        return true
    }

    private fun readAndCheckPasswordValue() {
        passwordValue = passwordInputEditText.text.toString().trim()
        if (passwordValue != PasswordPreferenceManager.readPasswordValue(this, ID))
            hasBeenModified = true
    }

    private fun readAndCheckLabels() {
        val oldLabels = PasswordPreferenceManager.readPasswordLabels(this, ID)
        passwordLabels = PreferenceArrayString(getAllLabels().iterator())
        if (!oldLabels.containsAll(passwordLabels) || !passwordLabels.containsAll(oldLabels))
            hasBeenModified = true
    }

    /**
     * Checks if any property was modified. Also checks if a new property was added.
     * Then sets [.hasBeenModified] to true
     */
    private fun readAndCheckAllProperties() {
        for (position in passwordProperties.indices) {
            val holder = propertiesRecyclerView.findViewHolderForAdapterPosition(position) as? EditPropertyAdapter.ViewHolder
            holder?.readValue()

            val property = passwordProperties[position]
            if (property.name != PasswordPreferenceManager.readPasswordPropertyName(this, ID, property.propertyID))
                hasBeenModified = true
            if (property.value != PasswordPreferenceManager.readPasswordPropertyValue(this, ID, property.propertyID))
                hasBeenModified = true
            if (property.secret != PasswordPreferenceManager.readPasswordPropertySecret(this, ID, property.propertyID))
                hasBeenModified = true
        }
        // Note: PasswordPreferenceManager.readPasswordPropertyName will return null if property.propertyID is not in preferences = if the property was added (String.equals accepts null and returns false)
    }

    /**
     * Get all labels by reading text from the views in labelsChipGroup
     */
    private fun getAllLabels(): Sequence<String> {
        return labelsChipGroup.allViews
            .filter { view -> view is Chip && view !== labelsAddChip as View }
            .map {
                val chip = it as Chip
                chip.text.toString()
            }
            .sortedByDescending { it }
    }

    /**
     * Checks if a property got removed.
     * Then sets [.hasBeenModified] to true
     * @param propertyIDs The list of all property IDs of this password currently in preferences
     */
    private fun checkIfPropertyWasRemoved(propertyIDs: List<Int>) {
        for (propertyID in propertyIDs) {
            if (!doesPropertyIDExist(propertyID)) { // if propertyID doesn't exist anymore after editing
                hasBeenModified = true
                return
            }
        }
    }

    private fun doesPropertyIDExist(propertyID: Int): Boolean {
        for (property in passwordProperties) {
            if (propertyID == property.propertyID)
                return true
        }
        return false
    }

    private fun generateNewPasswordID(): Int {
        val passwordIDs = PasswordPreferenceManager.readAllPasswordIDs(this) // this is fine because there can't be an unsaved password with an unsaved ID at this moment
        return IDGenerator(passwordIDs).generateID()
    }

    private fun getEditTextFromChipGroup(): AutoCompleteTextView? {
        return labelsChipGroup.allViews.firstOrNull { it is AutoCompleteTextView } as? AutoCompleteTextView
    }

    private fun getEditTextHitByTouchEvent(ev: MotionEvent): EditText? {
        return mainLinearLayout.allViews.firstOrNull { it is EditText && isViewHitByTouchEvent(it, ev) } as? EditText
    }
    // endregion
}
