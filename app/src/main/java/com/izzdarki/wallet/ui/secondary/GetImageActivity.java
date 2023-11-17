package com.izzdarki.wallet.ui.secondary;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import izzdarki.wallet.R;

import com.izzdarki.wallet.logic.authentication.AuthenticatedAppCompatActivity;
import com.izzdarki.wallet.utils.Utility;

import java.io.File;

/**
 * <h><b>Intent extras (parameters)</b></h>
 * <p>GetImageActivity.EXTRA_FILE_PROVIDER_AUTHORITY (String): file provider authority, that is used for generating Uri to the file that gets created in app-specific storage (see comment at the bottom of this file) -> no default</p>
 * <p>GetImageActivity.EXTRA_FOLDER_PATH (String): path of the folder, that the file should be saved in (base directory + sub directory (see comment at the bottom of this file ([sub directory]))) -> no default</p>
 * <p>GetImageActivity.EXTRA_FILE_NAME (String): name of the file, that selected file gets copied to without extension (extension gets copied from selected file) -> no default</p>
 * <p>GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE (int): maximum useful length of the short side of the image. The image gets scaled down to that size -> optional</p>
 * <p>GetImageActivity.EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE (int): maximum useful length of the long side of the image. The image gets scaled down to that size -> optional</p>
 * <p></p>
 * <h><b>Intent extras (results)</b></h>
 * <p>GetImageActivity.EXTRA_RESULT_FILE (Serializable): file with copy of selected file</p>
 * <p>GetImageActivity.EXTRA_RESULT_URI (Parcelable): uri of result file</p>
 */
public class GetImageActivity extends AuthenticatedAppCompatActivity {

    public static final String EXTRA_FILE_PROVIDER_AUTHORITY = "com.izzdarki.get_image_activity.file_provider_authority";
    public static final String EXTRA_FOLDER_PATH = "com.izzdarki.get_image_activity.extra_folder_path";

    /** file name without extension, extension gets copied from selected file */
    public static final String EXTRA_FILE_NAME = "com.izzdarki.get_image_activity.extra_file_name";
    public static final String EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE = "com.izzdarki.get_image_activity.extra_image_max_short_side";
    public static final String EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE = "com.izzdarki.get_image_activity.extra_image_max_long_side";

    public static final String EXTRA_RESULT_URI = "com.izzdarki.get_image_activity.extra_result_uri";
    public static final String EXTRA_RESULT_FILE = "com.izzdarki.get_image_activity.extra_result_file";

    protected static final int REQUEST_CROP_IMAGE = 7834;

    // variables
    protected String fileProviderAuthority;
    protected String folderPath;
    protected String fileName;
    protected File imageFile = null;
    protected Uri imageUri = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileProviderAuthority = getIntent().getStringExtra(EXTRA_FILE_PROVIDER_AUTHORITY);
        if (fileProviderAuthority == null) throw new AssertionError();

        folderPath = getIntent().getStringExtra(EXTRA_FOLDER_PATH);
        if (folderPath == null) throw new AssertionError();

        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        if (fileName == null) throw new AssertionError();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CROP_IMAGE) {
            if (resultCode == RESULT_CANCELED)
                setResult(RESULT_CANCELED);
            else {
                if (resultCode == CropActivity.RESULT_ERROR) {
                    Exception error = (Exception) data.getSerializableExtra(CropActivity.EXTRA_RESULT_ERROR_EXCEPTION);
                    Toast.makeText(this, getString(R.string.error_occurred) + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                Intent result = new Intent();
                result.putExtra(EXTRA_RESULT_FILE, imageFile);
                result.putExtra(EXTRA_RESULT_URI, imageUri);
                setResult(RESULT_OK, result);
            }
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void cropImageAndFinish() {
        Intent intent = new Intent(this, CropActivity.class);
        intent.putExtra(CropActivity.EXTRA_IMAGE_URI, imageUri);
        intent.putExtra(CropActivity.EXTRA_IMAGE_FILE_PATH, imageFile.getAbsolutePath());
        startActivityForResult(intent, REQUEST_CROP_IMAGE);
    }

    protected void scaleImageFile() {
        int maxShortSide = getIntent().getIntExtra(EXTRA_IMAGE_MAX_NEEDED_SHORT_SIDE, -1);
        int maxLongSide = getIntent().getIntExtra(EXTRA_IMAGE_MAX_NEEDED_LONG_SIDE, -1);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        int shortSide = Math.min(options.outWidth, options.outHeight);
        int longSide = Math.max(options.outWidth, options.outHeight);

        double scale = Utility.getScaleForMaxSize(maxShortSide, maxLongSide, shortSide, longSide);

        if (scale != 1.0d) {
            Bitmap bitmap;
            try {
                //Utility.Timer timer = new Utility.Timer("GetImageActivity: decode file");
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                //timer.end();
            } catch (OutOfMemoryError e) {
                return;
            }
            Utility.scaleBitmapToFile(scale, bitmap, imageFile);
        }
    }
}
