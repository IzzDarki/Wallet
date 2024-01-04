package com.izzdarki.wallet.ui.utility;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import izzdarki.wallet.R;
import com.google.zxing.Result;
import com.izzdarki.wallet.logic.authentication.AuthenticatedAppCompatActivity;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class CodeScannerActivity extends AuthenticatedAppCompatActivity implements ZXingScannerView.ResultHandler {
    static public final int RESULT_PERMISSION_DENIED = RESULT_FIRST_USER;
    static public final String EXTRA_RESULT_CODE_TYPE = "com.izzdarki.code_scanner.code_type"; // Serializable extra
    static public final String EXTRA_RESULT_CODE = "com.izzdarki.code_scanner.code"; // String extra

    private ZXingScannerView scannerView;

    // permission
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        scannerView.startCamera();
                    } else {
                        setResult(RESULT_PERMISSION_DENIED);
                        finish();
                    }
                }
            });


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        startCameraWithPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_CODE_TYPE, rawResult.getBarcodeFormat());
        intent.putExtra(EXTRA_RESULT_CODE, rawResult.getText());
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void startCameraWithPermission() {
        //https://developer.android.com/training/permissions/requesting
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            scannerView.startCamera();
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.camera_permission_dialogue_title)
                    .setMessage(R.string.camera_permission_dialogue_qr_code_message)
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        dialog.cancel();
                        setResult(RESULT_PERMISSION_DENIED);
                    })
                    .setPositiveButton(R.string.go_ahead, (dialog, which) -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA))
                    .setOnCancelListener(dialog -> finish())
                    .show();
        }
        else
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

}