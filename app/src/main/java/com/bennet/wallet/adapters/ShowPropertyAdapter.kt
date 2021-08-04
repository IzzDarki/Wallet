package com.bennet.wallet.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bennet.wallet.R
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.utils.ItemProperty
import com.bennet.wallet.utils.Utility
import com.google.android.material.textview.MaterialTextView

class ShowPropertyAdapter(properties: List<ItemProperty>, onTextVisibilityChanged: (() -> Unit)?)
    : RecyclerView.Adapter<ShowPropertyAdapter.ViewHolder>() {

    private var properties = properties
    private var onTextVisibilityChanged = onTextVisibilityChanged

    inner class ViewHolder(v: View)
        : RecyclerView.ViewHolder(v) {
        var divider: View = v.findViewById(R.id.show_property_divider)
        var nameView: MaterialTextView = v.findViewById(R.id.show_property_name_view)
        var valueView: MaterialTextView = v.findViewById(R.id.show_property_value_view)
        var visibilityToggleButton: AppCompatImageButton = v.findViewById(R.id.show_property_visibility_toggle)

        private val isTextHidden get() = valueView.text.toString().contains("\u2022")
        private val context get() = itemView.context

        init {
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
                valueView.setTypeface(Typeface.MONOSPACE)
                if (AppPreferenceManager.isLengthHiddenInSecretFields(context))
                    valueView.setText(R.string.hidden_password_dots)
                else {
                    val valueLength: Int = properties[adapterPosition].value.length
                    val hiddenValue = Utility.createStringNCopies(valueLength, "\u2022")
                    valueView.text = hiddenValue
                }
                valueView.setTextIsSelectable(false)
                visibilityToggleButton.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.icon_visibility_off_30dp)
                )
            }
            else {
                setValueTypeface()
                valueView.text = properties[adapterPosition].value
                valueView.setTextIsSelectable(true)
                visibilityToggleButton.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.icon_visibility_30dp)
                )
            }
        }

        /**
         * Changes the typeface.
         * If preference [AppPreferenceManager.isMonospaceInSecretFields] is true, secret properties are monospace, others are 'default' (not monospace).
         */
        fun setValueTypeface() {
            if (properties[adapterPosition].secret && AppPreferenceManager.isMonospaceInSecretFields(context))
                valueView.typeface = Typeface.MONOSPACE
            else
                valueView.typeface = Typeface.DEFAULT
        }
    }


    // region adapter override functions
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.adapter_show_property, parent, false)
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