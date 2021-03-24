package com.bennet.wallet;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextThemeWrapper;

import java.io.File;

/**
 * <h><b>Intent extras (parameters)</b></h>
 * <p>ImageCaptureActivity.EXTRA_FILE_EXTENSION (String): file extension of image file -> ".jpg"</p>
 */
public class ImageCaptureActivity extends GetImageActivity {

    protected static final int REQUEST_IMAGE_CAPTURE = 1;

    public static final int RESULT_NO_IMAGE_CAPTURE_INTENT = RESULT_FIRST_USER;
    public static final int RESULT_PERMISSION_DENIED = RESULT_FIRST_USER + 1;

    public static final String EXTRA_FILE_EXTENSION = "com.bennet.image_capture.extra_file_extension";

    protected String fileExtension;

    // permission
    private ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        startCamera();
                    } else {
                        setResult(RESULT_PERMISSION_DENIED);
                        finish();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileExtension = getIntent().getStringExtra(EXTRA_FILE_EXTENSION);
        if (fileExtension == null)
            fileExtension = ".jpg";

        takePhoto();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            switch (resultCode) {
                case RESULT_OK:
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

    protected void takePhoto() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) // Ensure that the app has CAMERA permission
                startCamera();
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                AlertDialog.Builder build = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
                build.setTitle(R.string.camera_permission_dialogue_title)
                        .setMessage(R.string.camera_permission_dialogue_image_capture_message)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                setResult(RESULT_PERMISSION_DENIED);
                            }
                        })
                        .setPositiveButton(R.string.go_ahead, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .show();
            }
            else
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
        catch (ActivityNotFoundException e) { // Ensure that there's a camera activity to handle the intent
            setResult(RESULT_NO_IMAGE_CAPTURE_INTENT);
            finish();
        }
    }

    protected void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdirs();

        imageFile = new File(folderPath, fileName + fileExtension);
        imageUri = FileProvider.getUriForFile(this, fileProviderAuthority, imageFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

}










/*package com.bennet.wallet;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.File;

/**
 * <h><b>Intent extras (parameters)</b></h>
 * <p>ImageCaptureActivity.EXTRA_FILE_EXTENSION: file extension of image file</p>
 */
/*
public class ImageCaptureActivity extends GetImageActivity {

    public static final String EXTRA_FILE_PROVIDER_AUTHORITY = "com.bennet.image_capture.file_provider_authority";

    protected static final int REQUEST_IMAGE_CAPTURE = 1;

    public static final int RESULT_NO_IMAGE_CAPTURE_INTENT = RESULT_FIRST_USER;
    public static final int RESULT_PERMISSION_DENIED = RESULT_FIRST_USER + 1;

    public static final String EXTRA_FILE_EXTENSION = "com.bennet.image_capture.extra_file_extension";

    protected String fileExtension;

    // permission
    private ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        startCamera();
                    } else {
                        setResult(RESULT_PERMISSION_DENIED);
                        finish();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileExtension = getIntent().getStringExtra(EXTRA_FILE_EXTENSION);
        if (fileExtension == null)
            fileExtension = ".jpg";

        takePhoto();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            switch (resultCode) {
                case RESULT_OK:
                    scaleImage();
                    Intent result = new Intent();
                    result.putExtra(EXTRA_RESULT_FILE, currentPhotoFile);
                    result.putExtra(EXTRA_RESULT_URI, currentPhotoUri);
                    setResult(RESULT_OK, result);
                    break;

                case RESULT_CANCELED:
                    setResult(RESULT_CANCELED);
                    break;
            }
            finish();
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    protected void takePhoto() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) // Ensure that the app has CAMERA permission
                startCamera();
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                AlertDialog.Builder build = new AlertDialog.Builder(this);
                build.setTitle(R.string.camera_permission_dialogue_title)
                        .setMessage(R.string.camera_permission_dialogue_image_capture_message)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                setResult(RESULT_PERMISSION_DENIED);
                                finish();
                            }
                        })
                        .setPositiveButton(R.string.go_ahead, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                        })
                        .show();
            }
            else
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
        catch (ActivityNotFoundException e) { // Ensure that there's a camera activity to handle the intent
            setResult(RESULT_NO_IMAGE_CAPTURE_INTENT);
            finish();
        }
    }

    protected void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String folderPath = getFilesDir() + "/" + folderName;
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdirs();

        currentPhotoFile = new File(folderPath, fileName + fileExtension);
        currentPhotoUri = FileProvider.getUriForFile(this, fileProviderAuthority, currentPhotoFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

}
*/
