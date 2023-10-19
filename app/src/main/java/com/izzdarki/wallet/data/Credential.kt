package com.izzdarki.wallet.data

import androidx.recyclerview.selection.ItemKeyProvider
import java.io.File
import java.util.Date

data class Credential(
    var id: Int,
    var name: String,
    var color: Int,
    var creationDate: Date,
    var alterationDate: Date,
    var labels: MutableSet<String>,
    val fields: MutableList<CredentialField>,
    var barcode: Barcode?,
    val imagePaths: MutableList<String>,
) {
    val imageFiles get() = imagePaths.map { File(it) }
}

/**
 * This class is used to provide the key of a Credential to the SelectionTracker.
 */
class CredentialStableIDKeyProvider(private val list: List<Credential>) : ItemKeyProvider<Long>(SCOPE_MAPPED)
{
    override fun getKey(position: Int): Long = list[position].id.toLong()
    override fun getPosition(key: Long): Int = list.indexOfFirst { it.id.toLong() == key }
}
