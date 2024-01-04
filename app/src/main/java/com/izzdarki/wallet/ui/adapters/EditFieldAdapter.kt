package com.izzdarki.wallet.ui.adapters

import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import izzdarki.wallet.R
import com.izzdarki.wallet.storage.AppSettingsStorage
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.utils.Utility
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.izzdarki.wallet.logic.passwordgeneration.PasswordGeneration
import com.izzdarki.wallet.ui.credentials.CharacterSet
import com.izzdarki.wallet.ui.credentials.PasswordGenerationDialog

class EditFieldAdapter(
    private val fields: MutableList<CredentialField>,
    private val onPropertyRemovalListener: (() -> Unit)?
) : RecyclerView.Adapter<EditFieldAdapter.ViewHolder>() {

    // properties
    private var cursorToReset = Pair(-1, -1) // first: position in adapter (-1 = none), second: cursor position

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textInputLayout: TextInputLayout = itemView.findViewById(R.id.edit_property_text_input_layout)
        var textInputEditText: TextInputEditText = itemView.findViewById(R.id.edit_property_text_input_edit_text)

        init {
            textInputLayout.setStartIconOnClickListener {
                onStartIconClick()
            }
            textInputLayout.setEndIconOnClickListener { view: View ->
                onEndIconClick(view)
            } // gets called only for endIconMode="custom"

            textInputEditText.onFocusChangeListener =
                OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    if (adapterPosition == -1)
                        return@OnFocusChangeListener

                    if (!hasFocus) {
                        readPropertyValue()
                        if (fields[adapterPosition].secret)
                            textInputEditText.inputType = Utility.inputTypeTextHiddenPassword
                    }
                }
        }

        /** Reads the user input for the property value  */
        fun readPropertyValue() {
            val newValue = textInputEditText.text.toString().trim { it <= ' ' }
            fields[adapterPosition].value = newValue
        }

        private fun onStartIconClick() {
            readPropertyValue() // Could get lost otherwise
            val dialog: AlertDialog
            val builder = AlertDialog.Builder(
                context
            )
            val dialogView = View.inflate(context, R.layout.edit_property_input_dialog, null)
            val editText: TextInputEditText = dialogView.findViewById(R.id.edit_property_input_dialog_name_input_edit_text)
            val visibilitySwitch: MaterialSwitch = dialogView.findViewById(R.id.edit_property_input_dialog_visibility_switch)

            editText.setText(fields[adapterPosition].name)
            editText.requestFocus()
            visibilitySwitch.isChecked = fields[adapterPosition].secret
            builder.setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    // read name input
                    val newPropertyName = editText.text.toString().trim { it <= ' ' }
                    fields[adapterPosition].name = newPropertyName
                    textInputLayout.hint = newPropertyName

                    // read visibility input
                    fields[adapterPosition].secret = visibilitySwitch.isChecked
                    cursorToReset = Pair(adapterPosition, textInputEditText.selectionStart) // onBindViewHolder uses this to reset the cursor
                    notifyItemChanged(adapterPosition)
                }
                .setNegativeButton(R.string.cancel) { dialog1: DialogInterface, _: Int ->
                    dialog1.cancel()
                }
                .setOnDismissListener { Utility.hideKeyboard(editText) }
            dialog = builder.create()
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE) // Without this keyboard closes when alert dialog opens
            dialog.show()
        }

        private fun onEndIconClick(view: View) {
            if (fields[layoutPosition].secret) {
                Utility.hideKeyboard(itemView) // hide keyboard, because otherwise it closes and reopens when popup window appears (idk why)
                textInputEditText.requestFocus()
                textInputLayout.setEndIconDrawable(R.drawable.icon_expand_less_30dp)


                // Inflate popup window
                val popupView: View =
                    LayoutInflater.from(view.context).inflate(R.layout.edit_field_end_icon_popup_window_layout,null)
                val diceButton = popupView.findViewById<ImageButton>(R.id.edit_property_end_icon_popup_window_dice_button)
                val visibilityButton = popupView.findViewById<ImageButton>(R.id.edit_property_end_icon_popup_window_visibility_button)
                val deleteButton = popupView.findViewById<ImageButton>(R.id.edit_property_end_icon_popup_window_delete_button)


                // Create popup window
                val popupWindow = PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                )
                popupWindow.setOnDismissListener {
                    textInputLayout.setEndIconDrawable(R.drawable.icon_expand_more_30dp)
                }

                // Password generation button
                diceButton.setOnClickListener {
                    val passwordGenerationDialog = PasswordGenerationDialog(
                        context,
                        listOf(
                            CharacterSet(
                                name = context.getString(R.string.lowercase),
                                characters = PasswordGeneration.LOWERCASE,
                            ),
                            CharacterSet(
                                name = context.getString(R.string.uppercase),
                                characters = PasswordGeneration.UPPERCASE,
                            ),
                            CharacterSet(
                                name = context.getString(R.string.digits),
                                characters = PasswordGeneration.DIGITS,
                            ),
                            CharacterSet(
                                name = context.getString(R.string.easy_symbols),
                                characters = PasswordGeneration.EASY_SPECIAL_CHARACTERS,
                            ),
                            CharacterSet(
                                name = context.getString(R.string.hard_symbols),
                                characters = PasswordGeneration.DIFFICULT_SPECIAL_CHARACTERS,
                            ),
                            CharacterSet(
                                name = context.getString(R.string.crazy_symbols),
                                characters = PasswordGeneration.CRAZY_CHARACTERS,
                            ),
                        ),
                        onPasswordGenerated = { password ->
                            toggleTextVisibility()
                            textInputEditText.setText(password)
                            textInputEditText.selectAll()
                        },
                    )
                    passwordGenerationDialog.setOnDismissListener { popupWindow.dismiss() }
                    passwordGenerationDialog.show()
                }

                // Visibility button
                setVisibilityImageButton(visibilityButton)
                visibilityButton.setOnClickListener {
                    toggleTextVisibility()
                    popupWindow.dismiss()
                }

                // Delete button
                deleteButton.setOnClickListener {
                    deleteProperty()
                    popupWindow.dismiss()
                }

                // Show popup window
                popupWindow.showAsDropDown(view)
            }
            else
                deleteProperty()
        }

        private fun deleteProperty() {
            val position = adapterPosition
            onPropertyRemovalListener?.invoke()
            fields.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, fields.size - position)
        }

        // helpers
        private fun setVisibilityImageButton(visibilityButton: ImageButton) {
            if (isTextHidden)
                visibilityButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_visibility_30dp))
            else
                visibilityButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_visibility_off_30dp))
        }

        private fun toggleTextVisibility() {
            val cursor = textInputEditText.selectionStart
            if (!isTextHidden)
                textInputEditText.inputType = Utility.inputTypeTextHiddenPassword
            else {
                textInputEditText.inputType = Utility.inputTypeTextVisiblePassword
                if (AppSettingsStorage.isMonospaceInSecretFields(context))
                    textInputEditText.typeface = Typeface.MONOSPACE
            }
            textInputEditText.setSelection(cursor) // Otherwise cursor will be reset to start
        }

        fun clearFocus() {
            textInputEditText.clearFocus()
        }

        private val isTextHidden: Boolean
            get() = textInputEditText.inputType == Utility.inputTypeTextHiddenPassword

        private val context: Context
            get() = itemView.context

        fun setImeOptions(imeOptions: Int) {
            Utility.setImeOptionsAndRestart(textInputEditText, imeOptions)
        }
    }

    // override functions
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.adapter_edit_field, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        /*  These things need to be updated:
            - input type (normal text, password text)
            - end icon (delete, expand)
         */

        // text hidden mode, end icon expand (dropdown) for both visibility and delete icons
        if (fields[position].secret) {
            holder.textInputEditText.inputType = Utility.inputTypeTextHiddenPassword
            holder.textInputLayout.setEndIconDrawable(R.drawable.icon_expand_more_30dp)
        }
        else {
            holder.textInputEditText.inputType = Utility.inputTypeTextNormal
            holder.textInputLayout.setEndIconDrawable(R.drawable.icon_delete_30dp)
        }
        holder.textInputLayout.hint = fields[position].name
        holder.textInputEditText.setText(fields[position].value)
        if (position == cursorToReset.first) {
            holder.textInputEditText.setSelection(cursorToReset.second)
            cursorToReset = Pair(-1, -1) // reset to default state
        }
        if (position == fields.size - 1)
            holder.setImeOptions(EditorInfo.IME_ACTION_DONE)
        else
            holder.setImeOptions(EditorInfo.IME_ACTION_NEXT)
    }

    override fun getItemCount(): Int {
        return fields.size
    }
}