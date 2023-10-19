package com.izzdarki.wallet.storage

import android.content.Context
import com.izzdarki.wallet.data.Credential

sealed interface CredentialWriteStorage {

    /**
     * Write a credential to the storage.
     * On calls to [CredentialReadStorage.readAllIds], the id of this credential will be included.
     * @return `true` on success, `false` on failure,
     *  `null` when success is not checked e. g. on asynchronous writes
     */
    fun writeCredential(context: Context, credential: Credential): Boolean?

    /**
     * Remove a credential from the storage.
     * On calls to [CredentialReadStorage.readAllIds], the id of this credential will not be included anymore.
     * Does not delete the image files.
     * @return `true` on success, `false` on failure,
     *  `null` when success is not checked e. g. on asynchronous writes
     */
    fun removeCredential(context: Context, id: Int): Boolean?

}