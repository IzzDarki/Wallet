package com.izzdarki.wallet.services

import com.izzdarki.wallet.utils.Utility.scaleBitmapToFile
import com.izzdarki.wallet.utils.Utility.getScaleForMaxSize
import android.content.Intent
import izzdarki.wallet.R
import androidx.annotation.ColorInt
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ResultReceiver
import androidx.core.app.JobIntentService
import com.izzdarki.wallet.data.Barcode
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.logic.generateNewId
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Creates an example card and password and saves them in [CredentialPreferenceStorage]
 */
class CreateExampleCredentialService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        val id = generateNewId(CredentialPreferenceStorage.readAllIds(this))
        val cardName = getString(R.string.example_card_name)
        val barcode = getString(R.string.example_barcode)
        val barcodeType = Barcode.TYPE_QR
        val barcodeShowText = false
        @ColorInt val cardColor = getColor(R.color.example_card_color)
        val cardIDValue = getString(R.string.example_card_card_id)

        // card images from res/raw
        val frontImageStream = resources.openRawResource(R.raw.example_card_front_image)
        val backImageStream = resources.openRawResource(R.raw.example_card_back_image)

        // ensure cards images folder exists
        val cardsImagesFolder =
            File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
        if (!cardsImagesFolder.exists()) cardsImagesFolder.mkdirs()
        val frontImageFile =
            copyImage(this, frontImageStream, cardsImagesFolder, "example_card_front_image.jpg")
        val backImageFile =
            copyImage(this, backImageStream, cardsImagesFolder, "example_card_back_image.jpg")

        // save in preferences
        val exampleCard = Credential(
            id,
            cardName,
            cardColor,
            creationDate = Calendar.getInstance().time,
            alterationDate = Calendar.getInstance().time,
            labels = mutableSetOf(),
            fields = mutableListOf(
                CredentialField(
                    name = getString(R.string.card_id),
                    value = cardIDValue,
                    secret = false
                )
            ),
            barcode = Barcode(
                code = barcode,
                type = barcodeType,
                showText = barcodeShowText
            ),
            imagePaths = listOfNotNull(
                frontImageFile?.absolutePath,
                backImageFile?.absolutePath
            ).toMutableList(),
        )

        val examplePassword = Credential(
            id = generateNewId(CredentialPreferenceStorage.readAllIds(this) + listOf(id)),
            name = getString(R.string.example_password),
            color = getColor(R.color.example_card_color),
            creationDate = Calendar.getInstance().time,
            alterationDate = Calendar.getInstance().time,
            labels = mutableSetOf(),
            fields = mutableListOf(
                CredentialField(
                    name = getString(R.string.username),
                    value = getString(R.string.example_password_username),
                    secret = false,
                ),
                CredentialField(
                    name = getString(R.string.password),
                    value = getString(R.string.example_password_value),
                    secret = true
                )
            ),
            barcode = null,
            imagePaths = mutableListOf(),
        )

        CredentialPreferenceStorage.writeCredential(this, exampleCard)
        CredentialPreferenceStorage.writeCredential(this, examplePassword)

        // send result
        val receiver = intent.getParcelableExtra<ResultReceiver>(EXTRA_RESULT_RECEIVER)
        receiver!!.send(Activity.RESULT_OK, null)
        stopSelf()
    }

    companion object {
        private const val JOB_ID = 4912
        private const val EXTRA_RESULT_RECEIVER =
            "com.izzdarki.wallet.create_example_card_service.extra_result_receiver"

        /**
         * Enqueues work for creating example card
         * @param context context
         * @param intent intent to start service (no extras needed)
         */
        fun enqueueWork(context: Context?, intent: Intent, resultReceiver: ResultReceiver?) {
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
            enqueueWork(context!!, CreateExampleCredentialService::class.java, JOB_ID, intent)
        }

        fun copyImage(
            context: Context,
            imageStream: InputStream,
            cardsImagesFolder: File?,
            fileName: String?
        ): File? {
            // create file
            val imageFile: File
            try {
                imageFile = File(cardsImagesFolder, fileName!!)
                imageFile.createNewFile()
            } catch (e: IOException) {
                /*
            if (BuildConfig.DEBUG)
                Log.e("CreateExamplecardServ", "Couldn't create file for raw card image");
             */
                return null
            }

            // decode bitmaps
            //Utility.Timer decodeBitmapsTimer = new Utility.Timer("CreateExamplecardServ: decode raw card image");
            val bitmap: Bitmap
            bitmap = try {
                BitmapFactory.decodeStream(imageStream)
            } catch (e: OutOfMemoryError) {
                /*
            if (BuildConfig.DEBUG)
                Log.e("CreateExamplecardServ", "Couldn't copy raw card images, out of memory, " + e);
             */
                return null
            } finally {
                //decodeBitmapsTimer.end();
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    /*
                if (BuildConfig.DEBUG)
                    Log.e("CreateExamplecardServ", "Couldn't close raw card image input streams, " + e);
                 */
                }
            }

            // scale and copy
            val maxShortSide = context.resources.displayMetrics.widthPixels
            val maxLongSide = context.resources.displayMetrics.heightPixels
            scaleBitmapToFile(
                getScaleForMaxSize(maxShortSide, maxLongSide, bitmap),
                bitmap,
                imageFile
            )
            return imageFile
        }
    }
}