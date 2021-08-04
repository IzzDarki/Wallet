package com.bennet.wallet.utils

import androidx.annotation.ColorInt
import androidx.recyclerview.selection.ItemKeyProvider

class CardOrPasswordPreviewData(
    val ID: Int,
    val name: String,
    @ColorInt val color: Int,
)

/**
 * KeyProvider for selection
 */
class StableIDKeyProvider(private val list: List<CardOrPasswordPreviewData>)
    : ItemKeyProvider<Long>(SCOPE_MAPPED)
{
    override fun getKey(position: Int): Long = list[position].ID.toLong()
    override fun getPosition(key: Long): Int = list.indexOfFirst { it.ID.toLong() == key }
}