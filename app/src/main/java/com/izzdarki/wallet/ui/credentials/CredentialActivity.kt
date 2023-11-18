package com.izzdarki.wallet.ui.credentials

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.widget.LinearLayoutCompat
import com.izzdarki.wallet.utils.ScrollAnimationImageView
import android.util.Log
import izzdarki.wallet.R
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.izzdarki.wallet.data.Barcode
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.logic.authentication.AuthenticatedAppCompatActivity
import com.izzdarki.wallet.storage.AppSettingsStorage
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.storage.ImageDecodingException
import com.izzdarki.wallet.storage.ImageStorage
import com.izzdarki.wallet.utils.removeBitmapAt
import com.izzdarki.wallet.utils.updateBitmapAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException
import kotlin.IllegalStateException

open class CredentialActivity : AuthenticatedAppCompatActivity() {

    // UI
    protected lateinit var cardViewLayout: FrameLayout
    protected lateinit var scrollView: NestedScrollView
    protected lateinit var linearLayout: LinearLayoutCompat
    protected lateinit var cardView: ScrollAnimationImageView

    // data
    protected lateinit var credential: Credential

    protected fun createCardView() {
        cardView = ScrollAnimationImageView(this)
        cardViewLayout.addView(cardView, 0)
        cardView.addToScrollView(scrollView)
        cardView.post { // call when UI is ready
            asynchronouslyLoadAndDisplayAllImages()
        }
    }

    protected open fun initCredentialFromStorage() {
        val id = intent.getLongExtra(EXTRA_CREDENTIAL_ID, -1)
        if (id == -1L) { // Start intent without id
            finish()
            return
        }

        credential = CredentialPreferenceStorage.readCredential(this, id) ?: run {
            // Error reading credential from storage
            finish()
            Toast.makeText(this, R.string.could_not_read_entry_from_storage, Toast.LENGTH_SHORT).show()
            return
        }
    }

    protected fun asynchronouslyLoadAndDisplayAllImages() {
        lifecycleScope.launch(Dispatchers.Default) { // Dispatchers.Default seems to be good for CPU-bound tasks
            val deferredResults = credential.imageFiles.map { imageFile ->
                async {
                    Log.d("asd", "Starting to decode image ${credential.imageFiles.indexOf(imageFile)}")
                    val result = ImageStorage.decodeImage(this@CredentialActivity, imageFile)
                    Log.d("asd", "Finished decoding image ${credential.imageFiles.indexOf(imageFile)}")
                    result
                }
            }

            deferredResults.awaitAll().let { results -> // Wait until all images are decoded
                withContext(Dispatchers.Main) { // Run on main (UI) thread
                    for ((index, result) in results.withIndex().reversed()) {
                        if (result.isSuccess && result.getOrNull() != null) {
                            Log.d("asd", "successfully decoded image $index")
                            cardView.updateBitmapAt(index, result.getOrNull())
                        }
                        else {
                            Log.d("asd", "decoding image $index failed")
                            showDecodingErrorToast(result.exceptionOrNull())
                            removeImageAndUpdateCardView(index) // remove indices in reverse
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes image at given `index` from `credential.imagePaths` and removes it from the credential view.
     */
    protected open fun removeImageAndUpdateCardView(index: Int) {
        // Check if index is valid
        if (index < 0 || index >= credential.imagePaths.size)
            return
        cardView.removeBitmapAt(index, totalImagesBefore = credential.imagePaths.size)
        credential.imagePaths.removeAt(index)
    }

    protected open fun showDecodingErrorToast(throwable: Throwable?, additionalMessage: String = "") {
        val errorMessage = getString(
            when (throwable) {
                is FileNotFoundException -> R.string.image_file_got_lost
                is OutOfMemoryError -> R.string.file_too_big
                is IOException, is GeneralSecurityException -> R.string.image_decryption_failed
                is ImageDecodingException -> R.string.unknown_error_while_decoding_image
                else -> R.string.unknown_error_while_decoding_image
            }
        )
        val detailedErrorMessage =
            if (AppSettingsStorage.isDetailedErrors(this@CredentialActivity))
                throwable?.localizedMessage ?: ""
            else ""
        val beforeAdditionalMessage = if (additionalMessage.isNotEmpty()) ". " else ""
        val beforeDetailedMessage = if (detailedErrorMessage.isNotEmpty()) "\n" else ""
        val length =
            if (additionalMessage.isNotEmpty() && detailedErrorMessage.isNotEmpty()) Toast.LENGTH_SHORT
            else Toast.LENGTH_LONG
        Toast.makeText(
            this@CredentialActivity,
            errorMessage + beforeAdditionalMessage + additionalMessage + beforeDetailedMessage + detailedErrorMessage,
            length
        ).show()
    }

    protected fun codeIs1D(barcodeType: Int): Boolean {
        return barcodeType == Barcode.TYPE_CODABAR
                || barcodeType == Barcode.TYPE_CODE_39
                || barcodeType == Barcode.TYPE_CODE_93
                || barcodeType == Barcode.TYPE_CODE_128
                || barcodeType == Barcode.TYPE_EAN_8
                || barcodeType == Barcode.TYPE_EAN_13
                || barcodeType == Barcode.TYPE_ITF
                || barcodeType == Barcode.TYPE_UPC_A
                || barcodeType == Barcode.TYPE_UPC_E
    }

    protected fun barcodeShowTextToBoolean(barcodeShowText: String): Boolean {
        return when (barcodeShowText) {
            getString(R.string.barcode_show_text_value_true) -> true
            getString(R.string.barcode_show_text_value_false) -> false
            else -> throw IllegalStateException("barcodeShowTextToBoolean unexpected value: $barcodeShowText")
        }
    }

    protected fun barcodeShowTextBoolToString(barcodeShowText: Boolean): String {
        return if (barcodeShowText) getString(R.string.barcode_show_text_value_true) else getString(
            R.string.barcode_show_text_value_false
        )
    }

    protected val calculatedLayoutWidth: Float
        get() = resources.displayMetrics.widthPixels - 2 * resources.getDimension(R.dimen.default_padding)
    protected val calculatedLayoutHeight: Float
        get() = resources.displayMetrics.heightPixels - 2 * resources.getDimension(R.dimen.default_padding)

    companion object {
        // intent extras
        const val EXTRA_CREDENTIAL_ID = "com.izzdarki.wallet.cards.extra_card_id"

        /**
         * Deletes the credential from storage, including all image files that are currently in `credential.imagePaths`.
         */
        fun deleteCredentialWithImages(context: Context, credential: Credential) {
            for (imageFile in credential.imageFiles) {
                imageFile.delete() // Delete images both in cache and in files
            }
            credential.imagePaths.clear() // not really necessary

            CredentialPreferenceStorage.removeCredential(context, credential.id)
        }

        @JvmStatic
        fun barcodeTypeStringToInt(context: Context, barcodeType: String): Int {
            return when (barcodeType) {
                context.getString(R.string.barcode_type_value_aztec) -> Barcode.TYPE_AZTEC
                context.getString(R.string.barcode_type_value_data_matrix) -> Barcode.TYPE_DATA_MATRIX
                context.getString(R.string.barcode_type_value_pdf_417) -> Barcode.TYPE_PDF_417
                context.getString(R.string.barcode_type_value_qr) -> Barcode.TYPE_QR
                context.getString(R.string.barcode_type_value_codabar) -> Barcode.TYPE_CODABAR
                context.getString(R.string.barcode_type_value_code_39) -> Barcode.TYPE_CODE_39
                context.getString(R.string.barcode_type_value_code_93) -> Barcode.TYPE_CODE_93
                context.getString(R.string.barcode_type_value_code_128) -> Barcode.TYPE_CODE_128
                context.getString(R.string.barcode_type_value_ean_8) -> Barcode.TYPE_EAN_8
                context.getString(R.string.barcode_type_value_ean_13) -> Barcode.TYPE_EAN_13
                context.getString(R.string.barcode_type_value_itf) -> Barcode.TYPE_ITF
                context.getString(R.string.barcode_type_value_upc_a) -> Barcode.TYPE_UPC_A
                context.getString(R.string.barcode_type_value_upc_e) -> Barcode.TYPE_UPC_E
                else -> throw IllegalStateException("Unexpected value: $barcodeType")
            }
        }

        @JvmStatic
        fun codeTypeIntToString(context: Context, barcodeType: Int): String {
            return when (barcodeType) {
                Barcode.TYPE_AZTEC -> context.getString(R.string.barcode_type_value_aztec)
                Barcode.TYPE_DATA_MATRIX -> context.getString(R.string.barcode_type_value_data_matrix)
                Barcode.TYPE_PDF_417 -> context.getString(R.string.barcode_type_value_pdf_417)
                Barcode.TYPE_QR -> context.getString(R.string.barcode_type_value_qr)
                Barcode.TYPE_CODABAR -> context.getString(R.string.barcode_type_value_codabar)
                Barcode.TYPE_CODE_39 -> context.getString(R.string.barcode_type_value_code_39)
                Barcode.TYPE_CODE_93 -> context.getString(R.string.barcode_type_value_code_93)
                Barcode.TYPE_CODE_128 -> context.getString(R.string.barcode_type_value_code_128)
                Barcode.TYPE_EAN_8 -> context.getString(R.string.barcode_type_value_ean_8)
                Barcode.TYPE_EAN_13 -> context.getString(R.string.barcode_type_value_ean_13)
                Barcode.TYPE_ITF -> context.getString(R.string.barcode_type_value_itf)
                Barcode.TYPE_UPC_A -> context.getString(R.string.barcode_type_value_upc_a)
                Barcode.TYPE_UPC_E -> context.getString(R.string.barcode_type_value_upc_e)
                else -> throw IllegalStateException("Unexpected value: $barcodeType")
            }
        }

        @JvmStatic
        protected fun barcodeIntToBarcodeFormat(barcodeType: Int): BarcodeFormat {
            return when (barcodeType) {
                Barcode.TYPE_AZTEC -> BarcodeFormat.AZTEC
                Barcode.TYPE_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
                Barcode.TYPE_PDF_417 -> BarcodeFormat.PDF_417
                Barcode.TYPE_QR -> BarcodeFormat.QR_CODE
                Barcode.TYPE_CODABAR -> BarcodeFormat.CODABAR
                Barcode.TYPE_CODE_39 -> BarcodeFormat.CODE_39
                Barcode.TYPE_CODE_93 -> BarcodeFormat.CODE_93
                Barcode.TYPE_CODE_128 -> BarcodeFormat.CODE_128
                Barcode.TYPE_EAN_8 -> BarcodeFormat.EAN_8
                Barcode.TYPE_EAN_13 -> BarcodeFormat.EAN_13
                Barcode.TYPE_ITF -> BarcodeFormat.ITF
                Barcode.TYPE_UPC_A -> BarcodeFormat.UPC_A
                Barcode.TYPE_UPC_E -> BarcodeFormat.UPC_E
                else -> throw IllegalStateException("Unexpected value: $barcodeType")
            }
        }

        @JvmStatic
        protected fun barcodeFormatToInt(barcodeType: BarcodeFormat): Int {
            return when (barcodeType) {
                BarcodeFormat.AZTEC -> Barcode.TYPE_AZTEC
                BarcodeFormat.DATA_MATRIX -> Barcode.TYPE_DATA_MATRIX
                BarcodeFormat.PDF_417 -> Barcode.TYPE_PDF_417
                BarcodeFormat.QR_CODE -> Barcode.TYPE_QR
                BarcodeFormat.CODABAR -> Barcode.TYPE_CODABAR
                BarcodeFormat.CODE_39 -> Barcode.TYPE_CODE_39
                BarcodeFormat.CODE_93 -> Barcode.TYPE_CODE_93
                BarcodeFormat.CODE_128 -> Barcode.TYPE_CODE_128
                BarcodeFormat.EAN_8 -> Barcode.TYPE_EAN_8
                BarcodeFormat.EAN_13 -> Barcode.TYPE_EAN_13
                BarcodeFormat.ITF -> Barcode.TYPE_ITF
                BarcodeFormat.UPC_A -> Barcode.TYPE_UPC_A
                BarcodeFormat.UPC_E -> Barcode.TYPE_UPC_E
                else -> throw IllegalStateException("Unexpected value: $barcodeType")
            }
        }
    }
}