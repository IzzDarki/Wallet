package com.bennet.wallet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.zxing.BarcodeFormat;

import java.io.File;
import java.lang.ref.WeakReference;

import com.bennet.wallet.Utility.PreferenceArrayInt;

public class CardActivity extends AppCompatActivity {
    // intent extras
    static public final String EXTRA_CARD_ID = "com.bennet.wallet.cards.extra_card_id";

    // UI
    protected FrameLayout cardViewLayout;
    protected NestedScrollView scrollView;
    protected LinearLayout linearLayout;
    protected ScrollAnimationImageView cardView;

    // card properties
    protected int ID = -1;
    protected String cardName;
    protected String cardCode;
    protected int cardCodeType;
    protected boolean cardCodeTypeText;
    protected String cardID;
    protected @ColorInt int cardColor;
    protected File currentFrontImage;
    protected File currentBackImage;
    /**
     * contains all the ids, that correspond to the properties of this card
     */
    protected PreferenceArrayInt cardPropertyIDs;

    // decode bitmap task
    protected static class DecodeBitmapTask extends AsyncTask<Void, Void, Bitmap> {
        private WeakReference<CardActivity> parentActivityReference;
        private File imageFile;
        private boolean isFront;

        public DecodeBitmapTask(CardActivity parentActivity, File imageFile, boolean isFront) {
            this.parentActivityReference = new WeakReference<>(parentActivity);
            this.imageFile = imageFile;
            this.isFront = isFront;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                //Utility.Timer timer = new Utility.Timer("decode bitmap");
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                //timer.end(bitmap.toString());
                return bitmap;
            } catch (OutOfMemoryError e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            CardActivity parentActivity = parentActivityReference.get();
            if (bitmap != null) {
                if (parentActivity != null) {
                    if (isFront)
                        parentActivity.cardView.setFrontImage(bitmap);
                    else
                        parentActivity.cardView.setBackImage(bitmap);
                }
            }
            else {
                if (parentActivity != null)
                    Toast.makeText(parentActivity, R.string.file_too_big, Toast.LENGTH_SHORT).show();

                if (isFront) {
                    if (!imageFile.delete()) {
                        /*
                        if (BuildConfig.DEBUG)
                            Log.e("DecodeBitmapTask", "front image file couldn't be deleted");
                         */
                    }

                    if (parentActivity != null)
                        parentActivity.currentFrontImage = null;
                }
                else {
                    if (!imageFile.delete()) {
                        /*
                        if (BuildConfig.DEBUG)
                            Log.e("DecodeBitmapTask", "back image file couldn't be deleted");
                         */
                    }

                    if (parentActivity != null)
                        parentActivity.currentBackImage = null;
                }
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void createCardView() {
        cardView = new ScrollAnimationImageView(this);
        cardViewLayout.addView(cardView, 0);
        cardView.addToScrollView(scrollView);

        cardView.post(() -> { // call when UI is ready
                updateFrontImage();
                updateBackImage();
        });
    }

    protected void initFromPreferences() {
        ID = getIntent().getIntExtra(EXTRA_CARD_ID, -1);
        if (ID == -1)
            throw new IllegalStateException("CardActivity: missing preference: ID");

        cardName = CardPreferenceManager.readCardName(this, ID); // necessary
        if (cardName == null)
            throw new IllegalStateException("CardActivity: missing preference: card name");

        cardCode = CardPreferenceManager.readCardCode(this, ID);

        cardCodeType = CardPreferenceManager.readCardCodeType(this, ID);
        if (cardCodeType == -1)
            throw new IllegalStateException("CardActivity: missing preference: card code type");
        
        cardCodeTypeText = CardPreferenceManager.readCardCodeTypeText(this, ID);
        cardID = CardPreferenceManager.readCardID(this, ID);
        cardColor = CardPreferenceManager.readCardColor(this, ID);
        currentFrontImage = CardPreferenceManager.readCardFrontImageFile(this, ID);
        currentBackImage = CardPreferenceManager.readCardBackImageFile(this, ID);
        cardPropertyIDs = CardPreferenceManager.readCardPropertyIds(this, ID);

        getSupportActionBar().setTitle(cardName);
    }


    protected void updateFrontImage() {
        if (currentFrontImage != null)
            new DecodeBitmapTask(this, currentFrontImage, true).execute();
        else
            cardView.removeFrontImage();
    }

    protected void updateBackImage() {
        if (currentBackImage != null)
            new DecodeBitmapTask(this, currentBackImage, false).execute();
        else
            cardView.removeBackImage();
    }

    protected void deleteFrontImage() {
        if  (currentFrontImage != null) {
            if (!currentFrontImage.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardActivity", "front image file couldn't be deleted");
                 */
            }
            currentFrontImage = null;
        }
    }

    protected void deleteBackImage() {
        if (currentBackImage != null) {
            if (!currentBackImage.delete()) {
                /*
                if (BuildConfig.DEBUG)
                    Log.e("CardActivity", "back image file couldn't be deleted");
                 */
            }
            currentBackImage = null;
        }
    }

    static public int codeTypeStringToInt(Context context, String cardCodeType) {
        // (can't switch)
        if (cardCodeType.equals(context.getString(R.string.card_code_type_value_aztec)))
            return CardPreferenceManager.CARD_CODE_TYPE_AZTEC;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_data_matrix)))
            return CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_pdf_417)))
            return CardPreferenceManager.CARD_CODE_TYPE_PDF_417;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_qr)))
            return CardPreferenceManager.CARD_CODE_TYPE_QR;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_codabar)))
            return CardPreferenceManager.CARD_CODE_TYPE_CODABAR;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_code_39)))
            return CardPreferenceManager.CARD_CODE_TYPE_CODE_39;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_code_93)))
            return CardPreferenceManager.CARD_CODE_TYPE_CODE_93;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_code_128)))
            return CardPreferenceManager.CARD_CODE_TYPE_CODE_128;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_ean_8)))
            return CardPreferenceManager.CARD_CODE_TYPE_EAN_8;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_ean_13)))
            return CardPreferenceManager.CARD_CODE_TYPE_EAN_13;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_itf)))
            return CardPreferenceManager.CARD_CODE_TYPE_ITF;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_upc_a)))
            return CardPreferenceManager.CARD_CODE_TYPE_UPC_A;
        else if (cardCodeType.equals(context.getString(R.string.card_code_type_value_upc_e)))
            return CardPreferenceManager.CARD_CODE_TYPE_UPC_E;
        else
            throw new IllegalStateException("Unexpected value: " + cardCodeType);
    }

    static public String codeTypeIntToString(Context context, int cardCodeType) {
        switch (cardCodeType) {
            case CardPreferenceManager.CARD_CODE_TYPE_AZTEC:
                return context.getString(R.string.card_code_type_value_aztec);
            case CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX:
                return context.getString(R.string.card_code_type_value_data_matrix);
            case CardPreferenceManager.CARD_CODE_TYPE_PDF_417:
                return context.getString(R.string.card_code_type_value_pdf_417);
            case CardPreferenceManager.CARD_CODE_TYPE_QR:
                return context.getString(R.string.card_code_type_value_qr);
            case CardPreferenceManager.CARD_CODE_TYPE_CODABAR:
                return context.getString(R.string.card_code_type_value_codabar);
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_39:
                return context.getString(R.string.card_code_type_value_code_39);
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_93:
                return context.getString(R.string.card_code_type_value_code_93);
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_128:
                return context.getString(R.string.card_code_type_value_code_128);
            case CardPreferenceManager.CARD_CODE_TYPE_EAN_8:
                return context.getString(R.string.card_code_type_value_ean_8);
            case CardPreferenceManager.CARD_CODE_TYPE_EAN_13:
                return context.getString(R.string.card_code_type_value_ean_13);
            case CardPreferenceManager.CARD_CODE_TYPE_ITF:
                return context.getString(R.string.card_code_type_value_itf);
            case CardPreferenceManager.CARD_CODE_TYPE_UPC_A:
                return context.getString(R.string.card_code_type_value_upc_a);
            case CardPreferenceManager.CARD_CODE_TYPE_UPC_E:
                return context.getString(R.string.card_code_type_value_upc_e);
            default:
                throw new IllegalStateException("Unexpected value: " + cardCodeType);
        }
    }

    static protected BarcodeFormat cardCodeIntToBarcodeFormat(int cardCodeType) {
        switch (cardCodeType) {
            case CardPreferenceManager.CARD_CODE_TYPE_AZTEC:
                return BarcodeFormat.AZTEC;
            case CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX:
                return BarcodeFormat.DATA_MATRIX;
            case CardPreferenceManager.CARD_CODE_TYPE_PDF_417:
                return BarcodeFormat.PDF_417;
            case CardPreferenceManager.CARD_CODE_TYPE_QR:
                return BarcodeFormat.QR_CODE;
            case CardPreferenceManager.CARD_CODE_TYPE_CODABAR:
                return BarcodeFormat.CODABAR;
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_39:
                return BarcodeFormat.CODE_39;
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_93:
                return BarcodeFormat.CODE_93;
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_128:
                return BarcodeFormat.CODE_128;
            case CardPreferenceManager.CARD_CODE_TYPE_EAN_8:
                return BarcodeFormat.EAN_8;
            case CardPreferenceManager.CARD_CODE_TYPE_EAN_13:
                return BarcodeFormat.EAN_13;
            case CardPreferenceManager.CARD_CODE_TYPE_ITF:
                return BarcodeFormat.ITF;
            case CardPreferenceManager.CARD_CODE_TYPE_UPC_A:
                return BarcodeFormat.UPC_A;
            case CardPreferenceManager.CARD_CODE_TYPE_UPC_E:
                return BarcodeFormat.UPC_E;
            default:
                throw new IllegalStateException("Unexpected value: " + cardCodeType);
        }
    }

    static protected int cardCodeBarcodeFormatToInt(BarcodeFormat cardCodeType) {
        switch (cardCodeType) {
            case AZTEC:
                return CardPreferenceManager.CARD_CODE_TYPE_AZTEC;
            case DATA_MATRIX:
                return CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX;
            case PDF_417:
                return CardPreferenceManager.CARD_CODE_TYPE_PDF_417;
            case QR_CODE:
                return CardPreferenceManager.CARD_CODE_TYPE_QR;
            case CODABAR:
                return CardPreferenceManager.CARD_CODE_TYPE_CODABAR;
            case CODE_39:
                return CardPreferenceManager.CARD_CODE_TYPE_CODE_39;
            case CODE_93:
                return CardPreferenceManager.CARD_CODE_TYPE_CODE_93;
            case CODE_128:
                return CardPreferenceManager.CARD_CODE_TYPE_CODE_128;
            case EAN_8:
                return CardPreferenceManager.CARD_CODE_TYPE_EAN_8;
            case EAN_13:
                return CardPreferenceManager.CARD_CODE_TYPE_EAN_13;
            case ITF:
                return CardPreferenceManager.CARD_CODE_TYPE_ITF;
            case UPC_A:
                return CardPreferenceManager.CARD_CODE_TYPE_UPC_A;
            case UPC_E:
                return CardPreferenceManager.CARD_CODE_TYPE_UPC_E;
            default:
                throw new IllegalStateException("Unexpected value: " + cardCodeType);
        }
    }

    protected boolean codeIs1D(int cardCodeType) {
        return cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODABAR || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODE_39 ||
                cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODE_93 || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_CODE_128 ||
                cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_EAN_8 || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_EAN_13 ||
                cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_ITF || cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_UPC_A ||
                cardCodeType == CardPreferenceManager.CARD_CODE_TYPE_UPC_E;
        // TODO if one 2D code is accidentally in 1D there will easily be a bug
    }

    protected boolean codeTypeTextStringToBool(String cardCodeTypeText) {
        if (cardCodeTypeText.equals(getString(R.string.card_code_type_text_value_show_text)))
            return true;
        else if (cardCodeTypeText.equals(getString(R.string.card_code_type_text_value_dont_show_text)))
            return false;
        else
            throw new IllegalStateException("Unexpected value: " + cardCodeType);
    }

    protected String codeTypeTextBoolToString(boolean cardCodeTypeText) {
        if (cardCodeTypeText)
            return getString(R.string.card_code_type_text_value_show_text);
        else
            return getString(R.string.card_code_type_text_value_dont_show_text);
    }

    protected void hideKeyboard() {
        Utility.hideKeyboard(this);
    }

    protected void hideScrollBarForDefault() {
        // scroll view hides scroll bar until scroll changes
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                scrollView.setVerticalScrollBarEnabled(true);
                scrollView.getViewTreeObserver().removeOnScrollChangedListener(this);
            }
        });
    }

    protected float getCalculatedLayoutWidth() {
        return getResources().getDisplayMetrics().widthPixels - 2 * getResources().getDimension(R.dimen.card_padding);
    }

    protected float getCalculatedLayoutHeight() {
        return getResources().getDisplayMetrics().heightPixels - 2 * getResources().getDimension(R.dimen.card_padding);
    }
}
