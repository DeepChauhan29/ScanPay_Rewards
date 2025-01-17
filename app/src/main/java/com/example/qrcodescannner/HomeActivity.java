package com.example.qrcodescannner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.qrcodescanner.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    private Button Scanbtn;
    private PreviewView previewView;
    private static final String TAG = "HomeActivity";
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private CameraControl cameraControl; // CameraControl instance
    private float zoomRatio = 1.0f; // Initial zoom ratio
    private int frameCount = 0; // Counter to skip frames
    private static final int FRAME_SKIP_COUNT = 2; // Analyze every 3rd frame

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: HomeActivity created");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);
        barcodeScanner = BarcodeScanning.getClient();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            accessCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        Scanbtn = findViewById(R.id.Scanbtn);
        Scanbtn.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Scanbtn clicked");
            Intent i = new Intent(HomeActivity.this, ScanQRActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: HomeActivity destroyed");
        cameraExecutor.shutdown();
    }

    private void accessCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraControl = cameraProvider.bindToLifecycle(this, cameraSelector, preview).getCameraControl(); // Get CameraControl
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
        }
    }

    private void bindImageAnalysis(ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720)) // Set a lower resolution
                .setBackpressureStrategy (ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::universalUPIScanner);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error binding image analysis", e);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void universalUPIScanner(ImageProxy imageProxy) {
        if (frameCount++ % FRAME_SKIP_COUNT != 0) {
            imageProxy.close();
            return; // Skip frames to reduce processing load
        }

        try {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            Log.d(TAG, "Scanned QR Code Data: " + rawValue);

                            if (rawValue != null && rawValue.startsWith("upi://")) {
                                parseUPIData(rawValue);
                            } else {
                                Log.w(TAG, "Non-UPI QR Code detected");
                                Toast.makeText(this, "This is not a valid UPI QR Code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Barcode detection failed", e);
                        Toast.makeText(this, "Failed to scan QR Code", Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing image", e);
            imageProxy.close();
        }
    }

    private void parseUPIData(String rawValue) {
        String upiId = null;
        String name = null;
        String amount = null; // Optional: Amount to be paid
        String mode = null;
        String version = null;
        String transactionType = null;
        String qrMedium = null;

        // Remove the "upi://pay?" prefix before splitting
        String parametersString = rawValue.substring(rawValue.indexOf("?") + 1);
        String[] parameters = parametersString.split("&");

        for (String param : parameters) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1].replace("%20", " ").replace("+", " "); // Decode URL-encoded spaces
                Log.d(TAG, "Key: " + key + ", Value: " + value);
                switch (key) {
                    case "pa": // Payee address
                        upiId = value.replace("%40", "@"); // Decode the UPI ID
                        break;
                    case "pn": // Payee name
                        name = value;
                        break;
                    case "am": // Amount (if applicable)
                        amount = value;
                        break;
                    case "mode": // Mode of payment
                        mode = value;
                        break;
                    case "ver": // Version
                        version = value;
                        break;
                    case "txntype": // Transaction type
                        transactionType = value;
                        break;
                    case "qrmedium": // QR medium
                        qrMedium = value;
                        break;
                }
            } else {
                Log.w(TAG, "Invalid parameter format: " + param);
            }
        }

        // Validate extracted data
        if (isValidUPIId(upiId)) {
            Log.d(TAG, "Extracted UPI ID: " + upiId);
            Log.d(TAG, "Extracted Payee Name: " + (name != null ? name : "N/A"));
            Log.d(TAG, "Extracted Amount: " + (amount != null ? amount : "N/A"));
            Log.d(TAG, "Extracted Mode: " + (mode != null ? mode : "N/A"));
            Log.d(TAG, "Extracted Version: " + (version != null ? version : "N/A"));
            Log.d(TAG, "Extracted Transaction Type: " + (transactionType != null ? transactionType : "N/A"));
            Log.d(TAG, "Extracted QR Medium: " + (qrMedium != null ? qrMedium : "N/A"));

            // Start the next activity with the extracted data
            Intent i = new Intent(HomeActivity.this, AmountActivity.class);
            i.putExtra("decoded_data", rawValue);
            i.putExtra("upi_id", upiId);
            i.putExtra("payee_name", name != null ? name : "Unknown");
            i.putExtra("amount", amount != null ? amount : "0");
            i.putExtra("mode", mode);
            i.putExtra("version", version);
            i.putExtra("transaction_type", transactionType);
            i.putExtra("qr_medium", qrMedium);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } else {
            Log.w(TAG, "Invalid UPI ID: " + upiId);
            Toast.makeText(this, "Invalid UPI ID in QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidUPIId(String upiId) {
        // Basic validation for UPI ID format
        return upiId != null && upiId.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+$");
    }

    private void adjustZoom(Barcode barcode) {
        Rect boundingBox = barcode.getBoundingBox();
        if (boundingBox != null) {
            int barcodeWidth = boundingBox.width();
            zoomRatio = barcodeWidth > 200 ? Math.min(2.0f, zoomRatio + 0.1f) : Math.max(1.0f, zoomRatio - 0.1f);
            setCameraZoom(zoomRatio);
        }
    }

    private void setCameraZoom(float zoomRatio) {
        if (cameraControl != null) {
            cameraControl.setZoomRatio(zoomRatio);
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("This app requires camera permission to scan barcodes.")
                .setPositiveButton("OK", (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.CAMERA))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    accessCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });
}