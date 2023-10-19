package com.izzdarki.wallet.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.izzdarki.wallet.data.Credential
import izzdarki.wallet.R
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.RuntimeException
import kotlin.Exception

class ImageDecodingException : RuntimeException()

object ImageStorage {

    /**
     * Decodes the given image
     * Should be called asynchronously because image decoding is slow.
     * If the image file does not exist (anymore), it is removed from the card.
     * If a OutOfMemoryError occurs, the image is deleted and it's path is removed from `card.imagePaths`.
     */
    fun decodeImage(context: Context, imageFile: File): Result<Bitmap> {
        // If image file does not exist anymore (should not happen), return failure
        if (!imageFile.exists())
            return Result.failure(FileNotFoundException())

        return try {
            // Check if image is encrypted
            if (!isInFilesDir(context, imageFile) // if image is not in files directory, it is not encrypted
                || imageFile.name == context.getString(R.string.example_card_front_image_file_name) // example images are not encrypted
                || imageFile.name == context.getString(R.string.example_card_back_image_file_name)
            )
                Result.success(BitmapFactory.decodeFile(imageFile.absolutePath) ?: throw ImageDecodingException())
            else
                decodeEncryptedImage(context, imageFile)
        } catch (e: OutOfMemoryError) {
            Result.failure(e)
        } catch (e: ImageDecodingException) {
            Result.failure(e)
        }
    }

    /**
     * Decodes an encrypted image file.
     * Does not work for images that are not encrypted.
     */
    private fun decodeEncryptedImage(context: Context, imageFile: File): Result<Bitmap> {
        val inputStream: InputStream = try {
            val mainKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedImageFile = EncryptedFile.Builder(
                context,
                imageFile,
                mainKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedImageFile.openFileInput()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return Result.success(BitmapFactory.decodeStream(inputStream))
    }

    /**
     * Deletes the image file at given `index` in `card.imagePaths` if it is in the files directory.
     * Image files outside the files directory (assumed to be in the cache directory) are not deleted, because they are deleted regularly anyway.
     * Removes the path from `card.imagePaths` if deletion was successful or if the file was not in the files directory.
     * @return true iff the deletion could be done as described (also true if the index was out of bounds)
     */
//    fun deleteImageFile(context: Context, credential: Credential, index: Int): Boolean {
//        if (index >= credential.imagePaths.size || index < 0)
//            return true
//
//        // delete the file iff it's in the files directory (cached images are deleted regularly anyway)
//        val imageFile = File(credential.imagePaths[index])
//        val success =
//            if (isInFilesDir(context, imageFile))
//                imageFile.delete()
//            else
//                true
//
//        if (success)
//            credential.imagePaths.removeAt(index)
//        return success
//    }

    @JvmStatic
    fun isInFilesDir(context: Context, file: File): Boolean {
        return file.absolutePath.contains(context.filesDir.absolutePath)
    }

}