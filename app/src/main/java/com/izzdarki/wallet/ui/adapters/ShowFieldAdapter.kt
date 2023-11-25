package com.izzdarki.wallet.ui.adapters

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDivider
import izzdarki.wallet.R
import com.izzdarki.wallet.storage.AppSettingsStorage
import com.izzdarki.wallet.data.CredentialField
import com.google.android.material.textview.MaterialTextView

class ShowFieldAdapter(
    private val properties: List<CredentialField>,
    private val onTextVisibilityChanged: (() -> Unit)?
) : RecyclerView.Adapter<ShowFieldAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var divider: MaterialDivider = v.findViewById(R.id.show_property_divider)
        var nameView: MaterialTextView = v.findViewById(R.id.show_property_name_view)
        var valueView: MaterialTextView = v.findViewById(R.id.show_property_value_view)
        var copyToClipboardButton: AppCompatImageButton = v.findViewById(R.id.show_property_copy_clipboard)
        var visibilityToggleButton: AppCompatImageButton = v.findViewById(R.id.show_property_visibility_toggle)

        private val context get() = itemView.context
        private val isTextHidden get() = valueView.text.toString() == context.getString(R.string.hidden_password_dots)

        init {
            copyToClipboardButton.setOnClickListener {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText(context.getString(R.string.secret_text), properties[adapterPosition].value)

                // Set sensitive flag
                if (properties[adapterPosition].secret && Build.VERSION.SDK_INT >= 24) { // extras are only available in API 24+
                    val sensitiveFlagName =
                        if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE
                        else "android.content.extra.IS_SENSITIVE"
                    clipData.description.extras = PersistableBundle().apply {
                        putBoolean(sensitiveFlagName, true)
                    }
                }
                clipboardManager.setPrimaryClip(clipData)

                // Only show a toast for Android 12 and lower, according to https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
            visibilityToggleButton.setOnClickListener {
                val isCurrentlyHidden: Boolean = isTextHidden
                onTextVisibilityChanged?.invoke()
                if (isCurrentlyHidden)
                    setValueHidden(false)
            }
        }

        /**
         * Sets the value view to be either hidden (password dots, not selectable) or not hidden (real text, selectable) and also sets the according visibility button drawable (visibility or visibility off)
         * @param textHidden true if the value should be displayed as hidden, false otherwise
         */
        fun setValueHidden(textHidden: Boolean) {
            if (textHidden) {
                valueView.setText(R.string.hidden_password_dots)
                valueView.setTextIsSelectable(false)
                visibilityToggleButton.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.icon_visibility_off_24dp)
                )
            }
            else {
                valueView.text = properties[adapterPosition].value
                valueView.setTextIsSelectable(true)
                visibilityToggleButton.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.icon_visibility_24dp)
                )
            }
            setValueTypeface()
        }

        /**
         * Changes the typeface.
         * If preference [AppSettingsStorage.isMonospaceInSecretFields] is true, secret properties are monospace, others are 'default' (not monospace).
         */
        fun setValueTypeface() {
            if (properties[adapterPosition].secret && AppSettingsStorage.isMonospaceInSecretFields(context))
                valueView.typeface = Typeface.MONOSPACE
            else
                valueView.typeface = Typeface.DEFAULT
        }
    }


    // region adapter override functions
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.adapter_show_field, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0)
            holder.divider.visibility = View.GONE
        else
            holder.divider.visibility = View.VISIBLE

        holder.nameView.text = properties[position].name

        val secret: Boolean = properties[position].secret
        holder.setValueTypeface() // must be called before setValueHidden because setValueHidden sets typeface to monospace if the text is actually hidden

        holder.setValueHidden(secret)
        if (!secret)
            holder.visibilityToggleButton.visibility = View.GONE
        else
            holder.visibilityToggleButton.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int {
        return properties.size
    }
    // endregion
}