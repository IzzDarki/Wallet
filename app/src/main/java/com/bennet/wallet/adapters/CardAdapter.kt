package com.bennet.wallet.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.bennet.wallet.utils.CardOrPasswordPreviewData
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.bennet.wallet.R
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.doOnPreDraw
import com.bennet.wallet.activities.cards.CardActivity
import com.bennet.wallet.activities.cards.ShowCardActivity
import com.bennet.wallet.utils.ScrollAnimationImageView
import com.bennet.wallet.utils.Utility

class CardAdapter(cards: List<CardOrPasswordPreviewData>)
    : RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    companion object {
        const val cardWidthToHeightRatio = 85.6 / 53.98
        var cardWidth: Double = 0.0
    }

    private var cards: List<CardOrPasswordPreviewData> = cards

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardView: MaterialCardView = itemView.findViewById(R.id.small_card_item_card_view)
        var textView: MaterialTextView = itemView.findViewById(R.id.small_card_item_text_view)
    }

    private fun showCard(context: Context, cardID: Int) {
        val intent = Intent(context, ShowCardActivity::class.java)
        intent.putExtra(CardActivity.EXTRA_CARD_ID, cardID)
        context.startActivity(intent)
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
            holder.cardView.strokeWidth = 4
            holder.cardView.strokeColor = context.resources.getColor(R.color.cardViewOutlineColor)
        } else {
            // remove outline
            holder.cardView.strokeWidth = 0
        }

        // text color
        if (Utility.isColorDark(cards[pos].color))
            holder.textView.setTextColor(context.resources.getColor(R.color.onDarkTextColor))
        else
            holder.textView.setTextColor(context.resources.getColor(R.color.onLightTextColor))

        // show card on click
        holder.cardView.setOnClickListener {
            showCard(context, cards[pos].ID)
        }
    }

    override fun getItemCount(): Int {
        return cards.size
    }

}