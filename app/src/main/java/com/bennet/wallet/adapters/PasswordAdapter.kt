package com.bennet.wallet.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.bennet.wallet.R
import com.bennet.wallet.activities.passwords.ShowPasswordActivity
import com.bennet.wallet.utils.*
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView

class PasswordAdapter(passwords: List<CardOrPasswordPreviewData>)
    : RecyclerView.Adapter<PasswordAdapter.ViewHolder>()
{

    val passwords: List<CardOrPasswordPreviewData> = passwords
    lateinit var selectionTracker: SelectionTracker<Long>

    inner class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView), MultiSelectAdapterViewHolder
    {
        val cardView: MaterialCardView = itemView.findViewById(R.id.adapter_password_card_view)
        val textView: MaterialTextView = itemView.findViewById(R.id.adapter_password_text_view)
        //val iconView: AppCompatImageView = itemView.findViewById(R.id.adapter_password_icon_view)

        init {
            cardView.setOnClickListener {
                // show password on click
                showPassword(passwords[adapterPosition].ID)
            }
        }

        private fun showPassword(passwordID: Int) {
            val intent = Intent(cardView.context, ShowPasswordActivity::class.java)
            intent.putExtra(ShowPasswordActivity.EXTRA_PASSWORD_ID, passwordID)
            cardView.context.startActivity(intent)
        }

        fun makeSelected() {
            cardView.strokeWidth = 6
            cardView.strokeColor = TODO("")
        }

        // needed for selection
        override val itemDetails: ItemDetailsLookup.ItemDetails<Long>
            get() = object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long = passwords[adapterPosition].ID.toLong()
            }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_password, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val context = holder.cardView.context

        holder.textView.text = passwords[pos].name
        holder.cardView.setCardBackgroundColor(passwords[pos].color)
        holder.cardView.doOnPreDraw {
            holder.cardView.radius = (holder.cardView.width / 2 * CardAdapter.cardWidthToHeightRatio / ScrollAnimationImageView.widthToCornerRadiusRatio).toFloat()
        }

        // outline if color is similar to background color
        if (Utility.areColorsSimilar(
                Utility.getDefaultBackgroundColor(context),
                passwords[pos].color)
        ) {
            // draw outline
            holder.cardView.strokeWidth = 4
            holder.cardView.strokeColor = context.resources.getColor(R.color.cardViewOutlineColor)
        } else {
            // remove outline
            holder.cardView.strokeWidth = 0
        }

        // text color
        if (Utility.isColorDark(passwords[pos].color))
            holder.textView.setTextColor(context.resources.getColor(R.color.onDarkTextColor))
        else
            holder.textView.setTextColor(context.resources.getColor(R.color.onLightTextColor))

        if (selectionTracker.isSelected(passwords[pos].ID.toLong()))
            holder.makeSelected()
    }

    override fun getItemCount(): Int = passwords.size
}