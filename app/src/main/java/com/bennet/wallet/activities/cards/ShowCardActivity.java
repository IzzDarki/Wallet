package com.bennet.wallet.activities.cards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.preferences.AppPreferenceManager;
import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.services.DeleteCardService;
import com.bennet.wallet.R;
import com.bennet.wallet.adapters.ShowCardPropertyListItemAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import com.bennet.wallet.utils.Utility.StringPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowCardActivity extends CardActivity {
    // UI
    protected ImageView cardCodeImageView;
    protected MaterialTextView cardCodePlainTextView;
    protected RecyclerView cardPropertiesView;
    protected View dividerCardProperties;
    protected View dividerCardImages;
    protected Space extraSpaceCardImages;

    // variables
    protected @ColorInt
    int codeForegroundColor;
    protected @ColorInt
    int codeBackgroundColor;
    protected List<StringPair> cardProperties = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_card);

        // hooks
        cardViewLayout = findViewById(R.id.show_card_card_view_layout);
        scrollView = findViewById(R.id.show_card_scroll_view);
        linearLayout = findViewById(R.id.show_card_linear_layout);
        cardCodeImageView = findViewById(R.id.show_card_code_image_view);
        cardCodePlainTextView = findViewById(R.id.show_card_code_plain_text);
        cardPropertiesView = findViewById(R.id.show_card_property_list_recycler_view);
        dividerCardProperties = findViewById(R.id.show_card_divider_card_properties);
        dividerCardImages = findViewById(R.id.show_card_divider_card_images);
        extraSpaceCardImages = findViewById(R.id.show_card_extra_space_card_images);

        // init
        initFromPreferences();

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(cardName);

        // variables
        codeForegroundColor = getResources().getColor(R.color.barcodeForegroundColor);
        codeBackgroundColor = getResources().getColor(R.color.barcodeBackgroundColor);

        // card properties recyclerview
        cardPropertiesView.setLayoutManager(new LinearLayoutManager(this));
        updateProperties();
        cardPropertiesView.setAdapter(new ShowCardPropertyListItemAdapter(cardProperties));

        init();

        // card view
        createCardView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        scrollView.setScrollY(0); // refreshes scroll position

        cardView.removeFrontImage(); // hides old image, new image will be loaded later
        cardView.removeBackImage(); // hides old image, new image will be loaded later

        initFromPreferences();
        updateProperties();

        init();

        // card view
        updateFrontImage();
        updateBackImage();
    }

    protected void init() {
        // scroll view
        hideScrollbar();

        // card properties
        showCardProperties();
    }


    // handling action bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_card_or_password_action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.show_card_or_password_action_bar_edit) {
            Intent editIntent = new Intent(this, EditCardActivity.class);
            editIntent.putExtra(EditCardActivity.EXTRA_CARD_ID, ID);
            startActivity(editIntent);
            return true;
        }
        else if (itemId == R.id.show_card_or_password_action_bar_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
            builder.setTitle(R.string.delete_card);
            builder.setMessage(R.string.delete_card_dialog_message);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                finish(); // ALWAYS FINISH BEFORE STARTING OTHER ACTIVITY
                deleteCard();
                dialog.dismiss();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


    // functions
    protected void deleteCard() {
        CardPreferenceManager.removeFromAllCardIDs(this, ID); // to make sure that HomeActivity doesn't display the card in the case DeleteCardService hasn't already finished deleting it

        Intent intent = new Intent(this, DeleteCardService.class);
        DeleteCardService.enqueueWork(this, intent, ID);
    }

    protected void showCardProperties() {
        // code
        if (!cardCode.equals("")) {

            if (!cardCodeTypeText) {
                cardCodePlainTextView.setVisibility(View.GONE);
            }
            else {
                cardCodePlainTextView.setVisibility(View.VISIBLE);
                cardCodePlainTextView.setText(cardCode);
            }

            cardCodeImageView.setVisibility(View.VISIBLE);
            Bitmap codeBitmap;
            try {
                if (codeIs1D(cardCodeType))
                    codeBitmap = createCode1D();
                else
                    codeBitmap = createCodeDefault();
            } catch (IllegalArgumentException | WriterException | ArrayIndexOutOfBoundsException e) {

                String detailedErrorMessage = "";
                if (AppPreferenceManager.isDetailedErrors(this))
                    detailedErrorMessage = System.getProperty("line.separator") + e.getLocalizedMessage();

                Toast.makeText(this, String.format(getString(R.string.show_card_visual_code_cannot_be_displayed), codeTypeIntToString(this, cardCodeType)) + detailedErrorMessage, Toast.LENGTH_LONG).show();
                cardCodeImageView.setVisibility(View.GONE);
                return;
            }
            cardCodeImageView.setImageBitmap(codeBitmap);
        } else {
            cardCodePlainTextView.setVisibility(View.GONE);
            cardCodeImageView.setVisibility(View.GONE);
        }

        // properties
        cardPropertiesView.getAdapter().notifyDataSetChanged(); // reload
        if (cardProperties.size() > 0 && !cardCode.equals(""))
            dividerCardProperties.setVisibility(View.VISIBLE);
        else
            dividerCardProperties.setVisibility(View.GONE);

        // images
        if ((currentFrontImage != null || currentBackImage != null) && (cardProperties.size() > 0 || !cardCode.equals(""))) {
            dividerCardImages.setVisibility(View.VISIBLE);
            if (cardProperties.size() > 0)
                extraSpaceCardImages.setVisibility(View.GONE);
            else
                extraSpaceCardImages.setVisibility(View.VISIBLE);
        }
        else
            dividerCardImages.setVisibility(View.GONE);
    }

    protected void updateProperties() {
        cardProperties.clear();

        if (!cardID.equals(""))
            cardProperties.add(new StringPair(getString(R.string.card_id), cardID));

        for (Integer propertyID : cardPropertyIDs) {
            String name = CardPreferenceManager.readCardPropertyName(this, ID, propertyID);
            String value = CardPreferenceManager.readCardPropertyValue(this, ID, propertyID);
            cardProperties.add(new StringPair(name, value));
        }
    }

    private Bitmap createCodeDefault() throws IllegalArgumentException, WriterException {
        final WidthAndHeight widthAndHeight = getBarcodeSize(cardCodeType);

        BitMatrix bitMatrix;
        bitMatrix = new MultiFormatWriter().encode(cardCode, cardCodeIntToBarcodeFormat(cardCodeType), widthAndHeight.width, widthAndHeight.height);

        final int bitMatrixWidth = bitMatrix.getWidth();
        final int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];
        for (int y = 0; y < bitMatrixHeight; y++) {
            final int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++)
                pixels[offset + x] = bitMatrix.get(x, y) ? codeForegroundColor : codeBackgroundColor;
        }

        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    private Bitmap createCode1D() throws IllegalArgumentException, WriterException {
        final WidthAndHeight widthAndHeight = getBarcodeSize(cardCodeType);

        BitMatrix bitMatrix;
        bitMatrix = new MultiFormatWriter().encode(cardCode, cardCodeIntToBarcodeFormat(cardCodeType), widthAndHeight.width, widthAndHeight.height, null);

        final int bitMatrixWidth = bitMatrix.getWidth();
        final int bitMatrixHeight = bitMatrix.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < bitMatrixWidth; i++) {
            int[] column = new int[bitMatrixHeight];
            Arrays.fill(column, bitMatrix.get(i, 0) ? codeForegroundColor : codeBackgroundColor);
            bitmap.setPixels(column, 0, 1, i, 0, 1, bitMatrixHeight);
        }
        return bitmap;
    }

    protected static class WidthAndHeight {
        int width;
        int height;

        public enum StdFormats {
            square,
            horizontal,
        }

        WidthAndHeight(float width, float height) {
            this.width = (int) width;
            this.height = (int) height;
        }

        WidthAndHeight(float layoutWidth, StdFormats format) {
            switch (format) {
                case square:
                    width = (int) (layoutWidth / 2);
                    //noinspection SuspiciousNameCombination
                    height = width;
                    break;
                case horizontal:
                    width = (int) layoutWidth;
                    height = width / 3;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + format);
            }
        }
    }

    private WidthAndHeight getBarcodeSize(int cardCodeType) {
        switch (cardCodeType) {
            case CardPreferenceManager.CARD_CODE_TYPE_AZTEC:
            case CardPreferenceManager.CARD_CODE_TYPE_QR:
            case CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX:
                return new WidthAndHeight(getCalculatedLayoutWidth(), WidthAndHeight.StdFormats.square);

            case CardPreferenceManager.CARD_CODE_TYPE_PDF_417:
            case CardPreferenceManager.CARD_CODE_TYPE_CODABAR:
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_39:
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_93:
            case CardPreferenceManager.CARD_CODE_TYPE_CODE_128:
            case CardPreferenceManager.CARD_CODE_TYPE_EAN_8:
            case CardPreferenceManager.CARD_CODE_TYPE_EAN_13:
            case CardPreferenceManager.CARD_CODE_TYPE_ITF:
            case CardPreferenceManager.CARD_CODE_TYPE_UPC_A:
            case CardPreferenceManager.CARD_CODE_TYPE_UPC_E:
                return new WidthAndHeight(getCalculatedLayoutWidth(), WidthAndHeight.StdFormats.horizontal);

            default:
                throw new IllegalStateException("Unexpected value: " + cardCodeType);
        }
    }

}
