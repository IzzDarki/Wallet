package com.izzdarki.wallet.ui.cards

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import androidx.appcompat.widget.LinearLayoutCompat
import com.izzdarki.wallet.utils.ScrollAnimationImageView
import androidx.annotation.ColorInt
import android.os.AsyncTask
import android.graphics.Bitmap
import androidx.security.crypto.MasterKey
import androidx.security.crypto.EncryptedFile
import android.graphics.BitmapFactory
import izzdarki.wallet.R
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.izzdarki.wallet.preferences.CardPreferenceManager
import com.google.zxing.BarcodeFormat
import com.izzdarki.wallet.utils.ItemProperty
import com.izzdarki.wallet.utils.Utility
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import java.security.GeneralSecurityException
import java.util.*

open class CardActivity : AppCompatActivity() {
    // UI
    protected lateinit var cardViewLayout: FrameLayout
    protected lateinit var scrollView: NestedScrollView
    protected lateinit var linearLayout: LinearLayoutCompat
    protected lateinit var cardView: ScrollAnimationImageView

    // card properties
    protected var ID = -1
    protected lateinit var cardName: String
    protected var cardCode: String? = null
    protected var cardCodeType = 0
    protected var cardCodeTypeText = false
    protected var cardCreationDate = Date(0)
    protected var cardAlterationDate = Date(0)
    protected var cardProperties: MutableList<ItemProperty> = mutableListOf()
    protected lateinit var labels: Utility.PreferenceArrayString  // will not be kept up to date in edit activity

    @ColorInt
    protected var cardColor = 0
    protected var currentFrontImage: File? = null
    protected var currentBackImage: File? = null

    // decode bitmap task
    protected class DecodeBitmapTask(
        parentActivity: CardActivity?,
        imageFile: File?,
        isFront: Boolean
    ) : AsyncTask<Void?, Void?, Bitmap?>() {
        private val parentActivityReference: WeakReference<CardActivity?> = WeakReference(parentActivity)
        private val imageFile: File? = imageFile
        private val isFront: Boolean = isFront

        private enum class Error {
            NoError, FileTooBig, DecryptionFailed
        }

        private var error = Error.NoError
        private fun decodeEncryptedFile(): Bitmap? {
            // stop if parent activity has been killed
            if (parentActivityReference.get() == null) {
                error = Error.DecryptionFailed
                return null
            }
            val inputStream: InputStream = try {
                val context: Context? = parentActivityReference.get()
                val mainKey = MasterKey.Builder(context!!)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val encryptedImageFile = EncryptedFile.Builder(
                    context,
                    imageFile!!,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                encryptedImageFile.openFileInput()
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: GeneralSecurityException) {
                throw RuntimeException(e)
            }
            return BitmapFactory.decodeStream(inputStream)
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Bitmap? {
            return try {
                //val timer = Utility.Timer("decode bitmap " + (if (isFront) "front" else "back")); // debug timer
                val context: Context? = parentActivityReference.get()

                // check if file is in files directory, which means it is encrypted
                val bitmap: Bitmap? = if (parentActivityReference.get()!!.isInFilesDir(imageFile!!)
                    && imageFile.name != context!!.getString(R.string.example_card_front_image_file_name)
                    && imageFile.name != context.getString(R.string.example_card_back_image_file_name)
                ) { // these files don't need to be encrypted
                    decodeEncryptedFile()
                }
                else BitmapFactory.decodeFile(imageFile.absolutePath)

                // timer.end(bitmap.toString()) // debug timer
                bitmap
            } catch (e: OutOfMemoryError) {
                error = Error.FileTooBig
                null
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(bitmap: Bitmap?) {
            val parentActivity = parentActivityReference.get()
            if (error == Error.NoError) {
                if (parentActivity != null) {
                    if (isFront)
                        parentActivity.cardView.frontImage = bitmap
                    else
                        parentActivity.cardView.backImage = bitmap
                }
            } else if (error == Error.FileTooBig) {
                // if file was too big, it has to be deleted
                if (parentActivity != null)
                    Toast.makeText(
                        parentActivity,
                        R.string.file_too_big,
                        Toast.LENGTH_SHORT
                    ).show()
                if (isFront) {
                    if (imageFile != null && !imageFile.delete()) {
                        /*
                        if (BuildConfig.DEBUG)
                            Log.e("DecodeBitmapTask", "front image file couldn't be deleted")
                         */
                    }
                    if (parentActivity != null)
                        parentActivity.currentFrontImage = null
                } else {
                    if (imageFile != null && !imageFile.delete()) {
                        /*
                        if (BuildConfig.DEBUG)
                            Log.e("DecodeBitmapTask", "back image file couldn't be deleted")
                         */
                    }
                    if (parentActivity != null)
                        parentActivity.currentBackImage = null
                }
            } else if (error == Error.DecryptionFailed) {
                if (parentActivity != null)
                        Toast.makeText(
                        parentActivity,
                        R.string.image_decryption_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                if (parentActivity != null) {
                    if (isFront)
                        parentActivity.currentFrontImage = null
                    else
                        parentActivity.currentBackImage = null
                }
            }
        }
    }

    protected fun createCardView() {
        cardView = ScrollAnimationImageView(this)
        cardViewLayout.addView(cardView, 0)
        cardView.addToScrollView(scrollView)
        cardView.post {
            // call when UI is ready
            updateFrontImage()
            updateBackImage()
        }
    }

    protected open fun initFromPreferences() {
        ID = intent.getIntExtra(EXTRA_CARD_ID, -1)
        check(ID != -1) { "CardActivity: missing intent extra: ID" }

        cardName = CardPreferenceManager.readName(this, ID)
        check(cardName != "") { "CardActivity: missing preference: card name" } // necessary

        cardCode = CardPreferenceManager.readCode(this, ID)
        cardCodeType = CardPreferenceManager.readCodeType(this, ID)
        check(cardCodeType != -1) { "CardActivity: missing preference: card code type" }

        cardCodeTypeText = CardPreferenceManager.readCodeTypeText(this, ID)
        cardColor = CardPreferenceManager.readColor(this, ID)
        currentFrontImage = CardPreferenceManager.readFrontImageFile(this, ID)
        currentBackImage = CardPreferenceManager.readBackImageFile(this, ID)
        cardCreationDate = CardPreferenceManager.readCreationDate(this, ID)
        cardAlterationDate = CardPreferenceManager.readAlterationDate(this, ID)
        cardProperties = CardPreferenceManager.readProperties(this, ID)
        labels = CardPreferenceManager.readLabels(this, ID)
    }

    protected fun updateFrontImage() {
        if (currentFrontImage != null)
            DecodeBitmapTask(this, currentFrontImage, true).execute()
        else cardView.removeFrontImage()
    }

    protected fun updateBackImage() {
        if (currentBackImage != null)
            DecodeBitmapTask(this, currentBackImage, false).execute()
        else cardView.removeBackImage()
    }

    protected fun deleteFrontImage() {
        if (currentFrontImage != null) {
            if (!currentFrontImage!!.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardActivity", "front image file couldn't be deleted");
                 */
            }
            currentFrontImage = null
        }
    }

    protected fun deleteBackImage() {
        if (currentBackImage != null) {
            if (!currentBackImage!!.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardActivity", "back image file couldn't be deleted");
                 */
            }
            currentBackImage = null
        }
    }

    protected fun isInFilesDir(file: File): Boolean {
        return file.absolutePath.contains(filesDir.absolutePath)
    }

    protected fun codeIs1D(cardCodeType: Int): Boolean {
        return cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODABAR
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODE_39
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODE_93
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODE_128
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_EAN_8
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_EAN_13
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_ITF
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_UPC_A
                || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_UPC_E
    }

    protected fun codeTypeTextStringToBool(cardCodeTypeText: String): Boolean {
        return when (cardCodeTypeText) {
            getString(R.string.card_code_type_text_value_show_text) -> true
            getString(R.string.card_code_type_text_value_dont_show_text) -> false
            else -> throw IllegalStateException("Unexpected value: $cardCodeType")
        }
    }

    protected fun codeTypeTextBoolToString(cardCodeTypeText: Boolean): String {
        return if (cardCodeTypeText) getString(R.string.card_code_type_text_value_show_text) else getString(
            R.string.card_code_type_text_value_dont_show_text
        )
    }

    protected val calculatedLayoutWidth: Float
        get() = resources.displayMetrics.widthPixels - 2 * resources.getDimension(R.dimen.default_padding)
    protected val calculatedLayoutHeight: Float
        get() = resources.displayMetrics.heightPixels - 2 * resources.getDimension(R.dimen.default_padding)

    companion object {
        // intent extras
        const val EXTRA_CARD_ID = "com.izzdarki.wallet.cards.extra_card_id"

        @JvmStatic
        fun codeTypeStringToInt(context: Context, cardCodeType: String): Int {
            return when (cardCodeType) {
                context.getString(R.string.card_code_type_value_aztec) -> CardPreferenceManager.CARD_CODE_TYPE_AZTEC
                context.getString(R.string.card_code_type_value_data_matrix) -> CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX
                context.getString(R.string.card_code_type_value_pdf_417) -> CardPreferenceManager.CARD_CODE_TYPE_PDF_417
                context.getString(R.string.card_code_type_value_qr) -> CardPreferenceManager.CARD_CODE_TYPE_QR
                context.getString(R.string.card_code_type_value_codabar) -> CardPreferenceManager.CARD_CODE_TYPE_CODABAR
                context.getString(R.string.card_code_type_value_code_39) -> CardPreferenceManager.CARD_CODE_TYPE_CODE_39
                context.getString(R.string.card_code_type_value_code_93) -> CardPreferenceManager.CARD_CODE_TYPE_CODE_93
                context.getString(R.string.card_code_type_value_code_128) -> CardPreferenceManager.CARD_CODE_TYPE_CODE_128
                context.getString(R.string.card_code_type_value_ean_8) -> CardPreferenceManager.CARD_CODE_TYPE_EAN_8
                context.getString(R.string.card_code_type_value_ean_13) -> CardPreferenceManager.CARD_CODE_TYPE_EAN_13
                context.getString(R.string.card_code_type_value_itf) -> CardPreferenceManager.CARD_CODE_TYPE_ITF
                context.getString(R.string.card_code_type_value_upc_a) -> CardPreferenceManager.CARD_CODE_TYPE_UPC_A
                context.getString(R.string.card_code_type_value_upc_e) -> CardPreferenceManager.CARD_CODE_TYPE_UPC_E
                else -> throw IllegalStateException("Unexpected value: $cardCodeType")
            }
        }

        @JvmStatic
        fun codeTypeIntToString(context: Context, cardCodeType: Int): String {
            return when (cardCodeType) {
                CardPreferenceManager.CARD_CODE_TYPE_AZTEC -> context.getString(R.string.card_code_type_value_aztec)
                CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX -> context.getString(R.string.card_code_type_value_data_matrix)
                CardPreferenceManager.CARD_CODE_TYPE_PDF_417 -> context.getString(R.string.card_code_type_value_pdf_417)
                CardPreferenceManager.CARD_CODE_TYPE_QR -> context.getString(R.string.card_code_type_value_qr)
                CardPreferenceManager.CARD_CODE_TYPE_CODABAR -> context.getString(R.string.card_code_type_value_codabar)
                CardPreferenceManager.CARD_CODE_TYPE_CODE_39 -> context.getString(R.string.card_code_type_value_code_39)
                CardPreferenceManager.CARD_CODE_TYPE_CODE_93 -> context.getString(R.string.card_code_type_value_code_93)
                CardPreferenceManager.CARD_CODE_TYPE_CODE_128 -> context.getString(R.string.card_code_type_value_code_128)
                CardPreferenceManager.CARD_CODE_TYPE_EAN_8 -> context.getString(R.string.card_code_type_value_ean_8)
                CardPreferenceManager.CARD_CODE_TYPE_EAN_13 -> context.getString(R.string.card_code_type_value_ean_13)
                CardPreferenceManager.CARD_CODE_TYPE_ITF -> context.getString(R.string.card_code_type_value_itf)
                CardPreferenceManager.CARD_CODE_TYPE_UPC_A -> context.getString(R.string.card_code_type_value_upc_a)
                CardPreferenceManager.CARD_CODE_TYPE_UPC_E -> context.getString(R.string.card_code_type_value_upc_e)
                else -> throw IllegalStateException("Unexpected value: $cardCodeType")
            }
        }

        @JvmStatic
        protected fun cardCodeIntToBarcodeFormat(cardCodeType: Int): BarcodeFormat {
            return when (cardCodeType) {
                CardPreferenceManager.CARD_CODE_TYPE_AZTEC -> BarcodeFormat.AZTEC
                CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
                CardPreferenceManager.CARD_CODE_TYPE_PDF_417 -> BarcodeFormat.PDF_417
                CardPreferenceManager.CARD_CODE_TYPE_QR -> BarcodeFormat.QR_CODE
                CardPreferenceManager.CARD_CODE_TYPE_CODABAR -> BarcodeFormat.CODABAR
                CardPreferenceManager.CARD_CODE_TYPE_CODE_39 -> BarcodeFormat.CODE_39
                CardPreferenceManager.CARD_CODE_TYPE_CODE_93 -> BarcodeFormat.CODE_93
                CardPreferenceManager.CARD_CODE_TYPE_CODE_128 -> BarcodeFormat.CODE_128
                CardPreferenceManager.CARD_CODE_TYPE_EAN_8 -> BarcodeFormat.EAN_8
                CardPreferenceManager.CARD_CODE_TYPE_EAN_13 -> BarcodeFormat.EAN_13
                CardPreferenceManager.CARD_CODE_TYPE_ITF -> BarcodeFormat.ITF
                CardPreferenceManager.CARD_CODE_TYPE_UPC_A -> BarcodeFormat.UPC_A
                CardPreferenceManager.CARD_CODE_TYPE_UPC_E -> BarcodeFormat.UPC_E
                else -> throw IllegalStateException("Unexpected value: $cardCodeType")
            }
        }

        @JvmStatic
        protected fun cardCodeBarcodeFormatToInt(cardCodeType: BarcodeFormat): Int {
            return when (cardCodeType) {
                BarcodeFormat.AZTEC -> CardPreferenceManager.CARD_CODE_TYPE_AZTEC
                BarcodeFormat.DATA_MATRIX -> CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX
                BarcodeFormat.PDF_417 -> CardPreferenceManager.CARD_CODE_TYPE_PDF_417
                BarcodeFormat.QR_CODE -> CardPreferenceManager.CARD_CODE_TYPE_QR
                BarcodeFormat.CODABAR -> CardPreferenceManager.CARD_CODE_TYPE_CODABAR
                BarcodeFormat.CODE_39 -> CardPreferenceManager.CARD_CODE_TYPE_CODE_39
                BarcodeFormat.CODE_93 -> CardPreferenceManager.CARD_CODE_TYPE_CODE_93
                BarcodeFormat.CODE_128 -> CardPreferenceManager.CARD_CODE_TYPE_CODE_128
                BarcodeFormat.EAN_8 -> CardPreferenceManager.CARD_CODE_TYPE_EAN_8
                BarcodeFormat.EAN_13 -> CardPreferenceManager.CARD_CODE_TYPE_EAN_13
                BarcodeFormat.ITF -> CardPreferenceManager.CARD_CODE_TYPE_ITF
                BarcodeFormat.UPC_A -> CardPreferenceManager.CARD_CODE_TYPE_UPC_A
                BarcodeFormat.UPC_E -> CardPreferenceManager.CARD_CODE_TYPE_UPC_E
                else -> throw IllegalStateException("Unexpected value: $cardCodeType")
            }
        }
    }
}