package com.bennet.wallet.utils

import android.util.Log
import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.bennet.wallet.adapters.PasswordAdapter

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
