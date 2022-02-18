package com.izzdarki.wallet.utils

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView

interface MultiSelectAdapterViewHolder {
    val itemDetails: ItemDetailsLookup.ItemDetails<Long>
}

class MultiSelectItemDetailsLookup(private val recyclerView: RecyclerView)
    : ItemDetailsLookup<Long>()
{
    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(e.x, e.y)
        view?.let {
            return (recyclerView.getChildViewHolder(view) as MultiSelectAdapterViewHolder).itemDetails
        }
        return null
    }
}
