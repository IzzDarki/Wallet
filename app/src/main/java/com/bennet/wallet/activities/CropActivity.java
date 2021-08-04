package com.bennet.wallet.activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bennet.wallet.preferences.AppPreferenceManager;
import com.bennet.wallet.R;
import com.bennet.wallet.adapters.CardAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import com.theartofdev.edmodo.cropper.CropImageView; // Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0.txt



public class CropActivity extends AppCompatActivity implements CropImageView.OnCropImageCompleteListener {
    static public final String EXTRA_IMAGE_URI = "com.bennet.wallet.crop_activity.extra_image_uri"; // Parcelable
    static public final String EXTRA_IMAGE_FILE_PATH = "com.bennet.wallet.crop_activity.extra_image_file_path"; // String
    static public final String EXTRA_RESULT_ERROR_EXCEPTION = "com.bennet.wallet.crop_activity.extra_result_error_exception"; // Serializable
    static public final int RESULT_ERROR = RESULT_FIRST_USER + 0;

    // UI
    CropImageView cropImageView;

    // variables
    protected Uri imageUri;
    protected boolean ratioLocked;
    protected int longSide;

    // lifecycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        // init
        imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        String imageFilePath = getIntent().getStringExtra(EXTRA_IMAGE_FILE_PATH);

        // hooks
        cropImageView = findViewById(R.id.crop_crop_image_view);

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // crop image view
        cropImageView.setImageUriAsync(imageUri);
        cropImageView.setOnCropImageCompleteListener(this);
        cropImageView.setGuidelines(CropImageView.Guidelines.ON_TOUCH);
        cropImageView.setAutoZoomEnabled(false);
        cropImageView.setShowProgressBar(false);
        lockCardRatio(true);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFilePath, options);
        int width = options.outWidth;
        int height = options.outHeight;
        longSide = Math.max(width, height);

        setCropRectFullSize();
    }

    // handling action bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crop_activity_crop_image_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.crop_image_menu_crop) {
            cropImageView.saveCroppedImageAsync(imageUri);
            return true;
        }
        else if (itemId == R.id.crop_image_menu_rotate_right) {
            cropImageView.rotateImage(90);
            setCropRectFullSize();
            return true;
        }
        else if (itemId == R.id.crop_image_menu_flip_horizontally) {
            cropImageView.flipImageHorizontally();
            return true;
        }
        else if (itemId == R.id.crop_image_menu_flip_vertically) {
            cropImageView.flipImageVertically();
            return true;
        }
        else if (itemId == R.id.crop_image_menu_lock_card_ration) {
            lockCardRatio(!ratioLocked); // toggle lock/unlock
            setCropRectFullSize();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return requestCancel();
    }

    @Override
    public void onBackPressed() {
        requestCancel();
    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
        if (result.isSuccessful())
            setResult(RESULT_OK);
        else {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_RESULT_ERROR_EXCEPTION, result.getError());
            setResult(RESULT_ERROR);
        }
        finish();
    }

    // helpers

    /**
     * Either cancels or shows an alert box to cancel
     * @return true if the activity has been finished, false otherwise
     */
    protected boolean requestCancel() {
        if (AppPreferenceManager.isBackConfirmCrop(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
            builder.setTitle(R.string.cancel_image_selection_title);
            builder.setMessage(R.string.cancel_image_selection_message);
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                cancelDirectly();
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel());
            builder.show();
            return false;
        }
        else {
            cancelDirectly();
            return true;
        }
    }

    protected void cancelDirectly() {
        setResult(RESULT_CANCELED);
        finish();
    }

    protected void lockCardRatio(boolean shouldLock) {
        if (shouldLock) {
            cropImageView.setAspectRatio(1000, (int) (1000 * CardAdapter.cardWidthToHeightRatio));
            cropImageView.setFixedAspectRatio(true);
            ratioLocked = true;
        }
        else {
            cropImageView.setFixedAspectRatio(false);
            ratioLocked = false;
        }
    }

    protected void setCropRectFullSize() {
        cropImageView.setCropRect(new Rect(0, 0, longSide, longSide));
    }
}