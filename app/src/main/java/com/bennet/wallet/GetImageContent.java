package com.bennet.wallet;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <h><b>Intent extras (parameters)</b></h>
 * <p>GetImageContent.EXTRA_TYPE (String): MIME type restricts what users can select -> no default</p>
 */
public class GetImageContent extends GetImageActivity {

    protected static final int REQUEST_GET_CONTENT = 1;

    public static final int RESULT_NO_GET_CONTENT_INTENT = RESULT_FIRST_USER;
    public static final int RESULT_ERROR_OCCURRED = RESULT_FIRST_USER + 1;

    public static final String EXTRA_TYPE = "com.bennet.get_content.extra_type";

    protected String type;
    protected String selectedFileTypeExtension;
    protected Uri selectedFileUri;
    protected InputStream selectedFileStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type == null) throw new AssertionError();

        getContent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_GET_CONTENT) {
            switch (resultCode) {
                case RESULT_OK:
                    selectedFileUri = data.getData();
                    String selectedFileType = getContentResolver().getType(selectedFileUri);
                    selectedFileTypeExtension = "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(selectedFileType);

                    saveContent();
                    scaleImageFile();
                    cropImageAndFinish();
                    break;

                case RESULT_CANCELED:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    protected void getContent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        try {
            startActivityForResult(intent, REQUEST_GET_CONTENT);
        } catch (ActivityNotFoundException e) {
            setResult(RESULT_NO_GET_CONTENT_INTENT);
            finish();
            return;
        }
    }

    protected void saveContent() {
        //Utility.Timer timer = new Utility.Timer("GetImageContent: save content");
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdirs();

        imageFile = new File(folderPath, fileName + selectedFileTypeExtension);
        imageUri = FileProvider.getUriForFile(this, fileProviderAuthority, imageFile);

        copyFile();
        //timer.end();
    }

    protected void copyFile() {
        //Utility.Timer timer = new Utility.Timer("GetImageContent: copy file");

        OutputStream savedFileStream;
        try {
            savedFileStream = getContentResolver().openOutputStream(imageUri);
            //Utility.Timer timerOutStr = new Utility.Timer("GetImageContent: copy file: open output stream");
            selectedFileStream = getContentResolver().openInputStream(selectedFileUri);
            //timerOutStr.end();
        } catch (FileNotFoundException e) {
            /*
            if (BuildConfig.DEBUG)
                Log.e("GetImageContent", "couldn't find files to open streams, " + e);
             */
            throw new AssertionError(e);
        }

        try {
            Utility.copyFile(selectedFileStream, savedFileStream);
        } catch (IOException e) {
            /*
            if (BuildConfig.DEBUG)
                Log.e("GetImageContent", "file couldn't be copied, " + e);
             */
            throw new AssertionError(e);
        }
        //timer.end();
    }

}





/* weird idea
package com.bennet.wallet;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <h><b>Intent extras (parameters)</b></h>
 * <p>GetImageContent.EXTRA_TYPE: MIME type restricts what users can select</p>
*/
/*
public class GetImageContent extends GetImageActivity {

    protected static final int REQUEST_GET_CONTENT = 1;

    public static final int RESULT_NO_GET_CONTENT_INTENT = RESULT_FIRST_USER;
    public static final int RESULT_ERROR_OCCURRED = RESULT_FIRST_USER + 1;

    public static final String EXTRA_TYPE = "com.bennet.get_content.extra_type";

    protected ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri resultUri) {

        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getContent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_GET_CONTENT) {
            switch (resultCode) {
                case RESULT_OK:
                    if (data != null) {
                        copyFileToCache(data.getData());

                        Intent result = new Intent();
                        result.putExtra(EXTRA_RESULT_URI, imageUri);
                        result.putExtra(EXTRA_RESULT_FILE, imageFile);
                        setResult(RESULT_OK, result);
                        break;
                    }
                    else {
                        Log.e("GetImageContent", "data is null");
                        setResult(RESULT_ERROR_OCCURRED);
                        finish();
                        return;
                    }

                case RESULT_CANCELED:
                    setResult(RESULT_CANCELED);
                    break;
            }
            finish();
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    protected void getContent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(intent, REQUEST_GET_CONTENT);
        } catch (ActivityNotFoundException e) {
            setResult(RESULT_NO_GET_CONTENT_INTENT);
            finish();
            return;
        }
    }

    protected void copyFileToCache(Uri resultUri) {
        InputStream resultFileStream;
        try {
            resultFileStream = getContentResolver().openInputStream(resultUri);
        } catch (FileNotFoundException e) {
            Log.e("GetImageContent", "selected file not found, couldn't resolve Uri");
            setResult(RESULT_ERROR_OCCURRED);
            finish();
            return;
        }

        imageFile = createFileInCache();
        imageUri = FileProvider.getUriForFile(this, getResources().getString(R.string.fileprovider_authority), imageFile);

        OutputStream imageFileStream = null;
        try {
            imageFileStream = getContentResolver().openOutputStream(imageUri);
        } catch (FileNotFoundException e) {
            Log.e("GetImageContent", "cached file not found, couldn't resolve Uri");
            setResult(RESULT_ERROR_OCCURRED);
            finish();
            return;
        }

        // copy result file to image file (transfer bytes)
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = resultFileStream.read(buf)) > 0)
                imageFileStream.write(buf, 0, len);
        } catch (IOException e) {
            throw new AssertionError(e);
        }

    }

    /*
    protected void saveContent() {
        String folderPath = getFilesDir() + "/" + folderName;
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdirs();

        currentPhotoFile = new File(folderPath, fileName + selectedFileTypeExtension);
        currentPhotoUri = FileProvider.getUriForFile(this, fileProviderAuthority, currentPhotoFile);


        OutputStream savedFileStream = null;
        try {
            savedFileStream = getContentResolver().openOutputStream(currentPhotoUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // copy selected file (transfer bytes)
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = selectedFileStream.read(buf)) > 0)
                savedFileStream.write(buf, 0, len);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
     */
/*
}
*/
