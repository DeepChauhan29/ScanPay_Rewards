package com.example.qrcodescannner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;

import com.example.qrcodescanner.R;
import com.google.zxing.Result;


public class ScanQRActivity extends AppCompatActivity {

    private Button backbtn;
    private CodeScanner mCodeScanner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.qrcodescanner.R.layout.activity_scan_qractivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            accessCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog();
        } else {
            // No explanation needed; request the permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        backbtn=findViewById(R.id.backbtn);
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ScanQRActivity.this,HomeActivity.class);
                startActivity(i);
            }
        });
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);

        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(() -> {
                    Intent i = new Intent(ScanQRActivity.this, AmountActivity.class);
                    i.putExtra("decoded_data", result.getText());
                    startActivity(i);
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
            }
        });
    }

    private void accessCamera(){

    }
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    accessCamera();
                } else {
                    showPermissionDeniedMessage();
                }
            });

    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            accessCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("This app needs access to your camera to take pictures.")
                .setPositiveButton("OK", (dialog, which) -> {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPermissionDeniedMessage() {
        // Show a message to the user indicating that the camera feature is unavailable
        Toast.makeText(this, "Camera permission denied. Feature unavailable.", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
}