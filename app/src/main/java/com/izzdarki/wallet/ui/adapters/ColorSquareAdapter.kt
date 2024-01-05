package com.izzdarki.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import izzdarki.wallet.databinding.AdapterColorSquareBinding

class ColorSquareAdapter(
    var colorList: List<Int>,
    var onColorClicked: (color: Int, position: Int) -> Unit
) : RecyclerView.Adapter<ColorSquareAdapter.ColorSquareViewHolder>() {

    class ColorSquareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorSquare = itemView as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorSquareViewHolder {
        val colorSquareBinding = AdapterColorSquareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorSquareViewHolder(colorSquareBinding.root)
    }

    override fun getItemCount() = colorList.size

    override fun onBindViewHolder(holder: ColorSquareViewHolder, position: Int) {
        holder.colorSquare.setCardBackgroundColor(colorList[position])
        holder.colorSquare.setOnClickListener {
            onColorClicked(colorList[position], position)
        }
    }

}