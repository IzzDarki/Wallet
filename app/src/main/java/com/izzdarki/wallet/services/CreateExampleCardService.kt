package com.izzdarki.wallet.services

import com.izzdarki.wallet.preferences.CardPreferenceManager.readFrontImagePath
import com.izzdarki.wallet.preferences.CardPreferenceManager.deleteFrontImage
import com.izzdarki.wallet.preferences.CardPreferenceManager.readBackImagePath
import com.izzdarki.wallet.preferences.CardPreferenceManager.deleteBackImage
import com.izzdarki.wallet.utils.Utility.scaleBitmapToFile
import com.izzdarki.wallet.utils.Utility.getScaleForMaxSize
import android.content.Intent
import izzdarki.wallet.R
import com.izzdarki.wallet.preferences.CardPreferenceManager
import androidx.annotation.ColorInt
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ResultReceiver
import androidx.core.app.JobIntentService
import com.izzdarki.wallet.utils.ItemProperty
import com.izzdarki.wallet.utils.Utility
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

class CreateExampleCardService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        val ID = 0
        val cardName = getString(R.string.example_card_name)
        val cardCode = getString(R.string.example_card_code)
        val cardCodeType = CardPreferenceManager.CARD_CODE_TYPE_QR
        val cardCodeTypeText = true
        @ColorInt val cardColor = resources.getColor(R.color.example_card_color)
        val cardIDValue = getString(R.string.example_card_card_id)

        // delete old images if present
        if (readFrontImagePath(this, ID) != null) deleteFrontImage(this, ID)
        if (readBackImagePath(this, ID) != null) deleteBackImage(this, ID)

        // card images from res/raw
        val frontImageStream = resources.openRawResource(R.raw.example_card_front_image)
        val backImageStream = resources.openRawResource(R.raw.example_card_back_image)

        // ensure cards images folder exists
        val cardsImagesFolder =
            File(filesDir.toString() + "/" + getString(R.string.cards_images_folder_name))
        if (!cardsImagesFolder.exists()) cardsImagesFolder.mkdirs()
        val frontImageFile =
            copyCardImage(this, frontImageStream, cardsImagesFolder, "example_card_front_image.jpg")
        val backImageFile =
            copyCardImage(this, backImageStream, cardsImagesFolder, "example_card_back_image.jpg")

        // save in preferences
        CardPreferenceManager.writeComplete(
            this,
            ID,
            cardName,
            cardColor,
            creationDate = Calendar.getInstance().time,
            alterationDate = Calendar.getInstance().time,
            labels = Utility.PreferenceArrayString(null),
            cardCode,
            cardCodeType,
            cardCodeTypeText,
            frontImageFile,
            backImageFile,
            properties = listOf(
                ItemProperty(
                    propertyID = 1, // This is the only property id for the example card
                    name = getString(R.string.card_id),
                    value = cardIDValue,
                    secret = false
                )
            )
        )

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
            enqueueWork(context!!, CreateExampleCardService::class.java, JOB_ID, intent)
        }

        fun copyCardImage(
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