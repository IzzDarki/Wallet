package com.bennet.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.bennet.wallet.Utility.PreferenceArrayInt;

public class CreateExampleCardService extends JobIntentService {
    private static final int JOB_ID = 4912;
    private static final String EXTRA_RESULT_RECEIVER = "com.bennet.wallet.create_example_card_service.extra_result_receiver";

    /**
     * Enqueues work for creating example card
     * @param context context
     * @param intent intent to start service (no extras needed)
     */
    public static void enqueueWork(Context context, Intent intent, ResultReceiver resultReceiver) {
        intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
        enqueueWork(context, CreateExampleCardService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final int ID = 0;
        final String cardName = getString(R.string.example_card_name);
        final String cardCode = getString(R.string.example_card_code);
        final int cardCodeType = CardPreferenceManager.CARD_CODE_TYPE_QR;
        final boolean cardCodeTypeText = true;
        final String cardID = getString(R.string.example_card_card_id);
        final @ColorInt int cardColor = getResources().getColor(R.color.exampleCardColor);

        // delete old images if present
        if (CardPreferenceManager.readCardFrontImagePath(this, ID) != null)
            CardPreferenceManager.deleteCardFrontImage(this, ID);
        if (CardPreferenceManager.readCardBackImagePath(this, ID) != null)
            CardPreferenceManager.deleteCardBackImage(this, ID);

        // card images from res/raw
        InputStream frontImageStream = getResources().openRawResource(R.raw.example_card_front_image);
        InputStream backImageStream = getResources().openRawResource(R.raw.example_card_back_image);

        // ensure cards images folder exists
        File cardsImagesFolder = new File(getFilesDir() + "/" + getString(R.string.cards_images_folder_name));
        if (!cardsImagesFolder.exists())
            cardsImagesFolder.mkdirs();

        File frontImageFile = copyCardImage(this, frontImageStream, cardsImagesFolder, "example_card_front_image.jpg");
        File backImageFile = copyCardImage(this, backImageStream, cardsImagesFolder, "example_card_back_image.jpg");

        // save in preferences
        CardPreferenceManager.addToAllCardIDs(this, ID);
        CardPreferenceManager.writeCardName(this, ID, cardName);
        CardPreferenceManager.writeCardCode(this, ID, cardCode);
        CardPreferenceManager.writeCardCodeType(this, ID, cardCodeType);
        CardPreferenceManager.writeCardCodeTypeText(this, ID, cardCodeTypeText);
        CardPreferenceManager.writeCardID(this, ID, cardID);
        CardPreferenceManager.writeCardColor(this, ID, cardColor);

        CardPreferenceManager.writeCardFrontImage(this, ID, frontImageFile.getAbsolutePath());
        CardPreferenceManager.writeCardBackImage(this, ID, backImageFile.getAbsolutePath());

        // send result
        ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        receiver.send(Activity.RESULT_OK, null);

        stopSelf();
    }

    static protected File copyCardImage(Context context, InputStream imageStream, File cardsImagesFolder, String fileName) {
        // create file
        File imageFile;
        try {
            imageFile = new File(cardsImagesFolder, fileName);
            imageFile.createNewFile();
        } catch (IOException e) {
            /*
            if (BuildConfig.DEBUG)
                Log.e("CreateExamplecardServ", "Couldn't create file for raw card image");
             */
            return null;
        }

        // decode bitmaps
        //Utility.Timer decodeBitmapsTimer = new Utility.Timer("CreateExamplecardServ: decode raw card image");
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(imageStream);
        } catch (OutOfMemoryError e) {
            /*
            if (BuildConfig.DEBUG)
                Log.e("CreateExamplecardServ", "Couldn't copy raw card images, out of memory, " + e);
             */
            return null;
        } finally {
            //decodeBitmapsTimer.end();
            try {
                imageStream.close();
            } catch (IOException e) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CreateExamplecardServ", "Couldn't close raw card image input streams, " + e);
                 */
            }
        }

        // scale and copy
        int maxShortSide = context.getResources().getDisplayMetrics().widthPixels;
        int maxLongSide = context.getResources().getDisplayMetrics().heightPixels;

        Utility.scaleBitmapToFile(Utility.getScaleForMaxSize(maxShortSide, maxLongSide, bitmap), bitmap, imageFile);
        return imageFile;
    }
}
