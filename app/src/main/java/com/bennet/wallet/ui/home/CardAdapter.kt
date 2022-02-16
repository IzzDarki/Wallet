package com.bennet.wallet.ui.home

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.bennet.wallet.R
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import com.bennet.wallet.ui.cards.CardActivity
import com.bennet.wallet.ui.cards.ShowCardActivity
import com.bennet.wallet.utils.*

class CardAdapter(
    private var cards: List<CardOrPasswordPreviewData>
) : RecyclerView.Adapter<CardAdapter.ViewHolder>()
{

    companion object {
        const val cardWidthToHeightRatio = 85.6 / 53.98
        var cardWidth: Double = 0.0
    }

    lateinit var selectionTracker: SelectionTracker<Long>

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), MultiSelectAdapterViewHolder {
        var cardView: MaterialCardView = itemView.findViewById(R.id.small_card_item_card_view)
        var textView: MaterialTextView = itemView.findViewById(R.id.small_card_item_text_view)

        init {
            cardView.setOnClickListener {
                // Show card on click
                showCard(cards[adapterPosition].ID)
            }
        }

        private fun showCard(cardID: Int) {
            val intent = Intent(cardView.context, ShowCardActivity::class.java)
            intent.putExtra(CardActivity.EXTRA_CARD_ID, cardID)
            cardView.context.startActivity(intent)
        }

        // needed for selection
        override val itemDetails: ItemDetailsLookup.ItemDetails<Long>
            get() = object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long = cards[adapterPosition].ID.toLong()
            }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_card, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val context = holder.cardView.context

        holder.textView.text = cards[pos].name
        holder.cardView.doOnPreDraw {
            holder.cardView.minimumHeight = (holder.cardView.width / cardWidthToHeightRatio).toInt()
            holder.cardView.radius = (holder.cardView.width / ScrollAnimationImageView.widthToCornerRadiusRatio).toFloat()
        }

        // outline if color is similar to background color
        holder.cardView.setCardBackgroundColor(cards[pos].color)
        if (Utility.areColorsSimilar(
                Utility.getDefaultBackgroundColor(context),
                cards[pos].color
            )
        ) {
            // draw outline
            holder.cardView.strokeWidth = context.resources.getDimension(R.dimen.outline_for_similar_colors_stroke_width).toInt()
            holder.cardView.strokeColor = context.resources.getColor(R.color.card_view_outline_color)
        } else {
            // remove outline
            holder.cardView.strokeWidth = 0
        }

        // text color
        if (Utility.isColorDark(cards[pos].color))
            holder.textView.setTextColor(context.resources.getColor(R.color.on_dark_text_color))
        else
            holder.textView.setTextColor(context.resources.getColor(R.color.on_light_text_color))

        if (selectionTracker.isSelected(cards[pos].ID.toLong()))
            AppUtility.makeCardViewSelected(holder.cardView)  // if not selected, outline has already been drawn (or not, if it has no outline)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

}