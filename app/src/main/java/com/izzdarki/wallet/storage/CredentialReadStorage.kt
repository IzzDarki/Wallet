package com.izzdarki.wallet.storage

import android.content.Context
import com.izzdarki.wallet.data.Credential
import java.util.SortedSet

sealed interface CredentialReadStorage {

    /**
     * Read all credential ids from storage.
     */
    fun readAllIds(context: Context): List<Long>


    /**
     * Read credential with given `id` from storage.
     * @return `null` if such a credential could not be found in the storage
     */
    fun readCredential(context: Context, id: Long): Credential?

    fun readAllCredentials(context: Context): List<Credential> {
        return readAllIds(context).mapNotNull { readCredential(context, it) }
    }

    /**
     * Read all labels from all credentials.
     * @return a sorted set of all labels (sorted first be length, then alphabetically)
     */
    fun readAllLabels(context: Context): SortedSet<String> {
        return readAllCredentials(context)
            .flatMap { it.labels }
            .toSortedSet(labelComparator)
    }

    companion object {
        /** Comparator for sorting labels first by length, then alphabetically. */
        val labelComparator = Comparator<String> { label1, label2 ->
            val lengthCompare = label1.length.compareTo(label2.length)
            if (lengthCompare != 0) lengthCompare
            else label1.compareTo(label2)
        }
    }
}
