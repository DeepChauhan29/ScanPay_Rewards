package com.example.qrcodescannner;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrcodescannner.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.material.bottomnavigation.BottomNavigationView;

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
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final int REQUEST_CODE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verify login state
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("isLoggedIn", false)) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        Log.d(TAG, "onCreate: HomeActivity created");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize upload button
        Button uploadButton = findViewById(R.id.gallery);
        uploadButton.setOnClickListener(v -> openGallery());

        // Initialize profile button
        Button profileButton = findViewById(R.id.profile);
        profileButton.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(i);
        });

        // Initialize bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_transactions) {
                startActivity(new Intent(getApplicationContext(), TransactionsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: HomeActivity destroyed");
        cleanupCamera();
    }

    private void cleanupCamera() {
        if (cameraExecutor != null) {
        cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void accessCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Store the cameraProvider instance
                cameraProvider = cameraProviderFuture.get();

                // Unbind all use cases before binding new ones
                cameraProvider.unbindAll();

                // Create preview use case
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Create image analysis use case
                imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::universalUPIScanner);

                // Create camera selector
                CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

                // Bind all use cases to lifecycle
                try {
                    Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    );
                    cameraControl = camera.getCameraControl();
                } catch (Exception e) {
                    Log.e(TAG, "Error binding use cases", e);
                    Toast.makeText(this, "Failed to initialize camera. Please restart the app.", Toast.LENGTH_LONG).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting camera provider", e);
                Toast.makeText(this, "Failed to access camera. Please check permissions.", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void universalUPIScanner(ImageProxy imageProxy) {
        if (frameCount++ % FRAME_SKIP_COUNT != 0) {
            imageProxy.close();
            return; // Skip frames to reduce processing load
        }

        try {
            // Check for null image before processing
            if (imageProxy.getImage() == null) {
                Log.e(TAG, "Null image received from camera");
                imageProxy.close();
                return;
            }
            
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            Log.d("QRDebug", "Scanned QR Code Data: " + rawValue);

                            if (rawValue != null && rawValue.startsWith("upi://")) {
                                Log.d("QRDebug", "Valid UPI QR code detected");
                                runOnUiThread(() -> parseUPIData(rawValue));
                                return; // Stop processing after first valid QR code
                            } else {
                                Log.w("QRDebug", "Non-UPI QR Code detected: " + rawValue);
                                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "This is not a valid UPI QR Code", Toast.LENGTH_SHORT).show());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("QRDebug", "Barcode detection failed", e);
                        runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Failed to scan QR Code", Toast.LENGTH_SHORT).show());
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Log.e("QRDebug", "Error analyzing image: " + e.getMessage(), e);
            imageProxy.close();
        }
    }

    private void parseUPIData(String rawValue) {
        try {
        Log.d("QRDebug", "Raw QR Code Data: " + rawValue);
        String upiId = null;
        String payeeName = null;
        String amount = null;
        String merchantCode = null;
        String mode = null;
        String purpose = null;
        String transactionType = null;
        String qrMedium = null;

            if (rawValue == null || !rawValue.contains("?")) {
                Log.e("QRDebug", "Invalid QR format: " + rawValue);
                runOnUiThread(() -> Toast.makeText(this, "Invalid UPI QR Code format", Toast.LENGTH_SHORT).show());
                return;
            }

        // Remove the "upi://pay?" prefix before splitting
        String parametersString = rawValue.substring(rawValue.indexOf("?") + 1);
        String[] parameters = parametersString.split("&");

        for (String param : parameters) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1].replace("%20", " ").replace("+", " "); // Decode URL-encoded spaces
                Log.d("QRDebug", "Key: " + key + ", Value: " + value);
                switch (key) {
                    case "pa": // Payee address
                        upiId = value.replace("%40", "@"); // Decode the UPI ID
                        break;
                    case "pn": // Payee name
                        payeeName = value;
                        break;
                    case "am": // Amount (if applicable)
                        amount = value;
                        break;
                    case "mc": // Merchant code
                        merchantCode = value;
                        break;
                    case "mode": // Mode of payment
                        mode = value;
                        break;
                    case "purpose": // Purpose of payment
                        purpose = value;
                        break;
                    case "txntype": // Transaction type
                        transactionType = value;
                        break;
                    case "qrmedium": // QR medium
                        qrMedium = value;
                        break;
                }
            } else {
                Log.w("QRDebug", "Invalid parameter format: " + param);
            }
        }

            // Ensure we have at least a UPI ID before proceeding
            if (upiId == null || upiId.isEmpty()) {
                Log.e("QRDebug", "No UPI ID found in QR code");
                runOnUiThread(() -> Toast.makeText(this, "Invalid QR code: No UPI ID found", Toast.LENGTH_SHORT).show());
                return;
        }

        // Validate extracted data
        if (isValidUPIId(upiId)) {
            Log.d("QRDebug", "Extracted UPI ID: " + upiId);
            Log.d("QRDebug", "Extracted Payee Name: " + (payeeName != null ? payeeName : "N/A"));
            Log.d("QRDebug", "Extracted Amount: " + (amount != null ? amount : "N/A"));
            Log.d("QRDebug", "Extracted Merchant Code: " + (merchantCode != null ? merchantCode : "N/A"));
            Log.d("QRDebug", "Extracted Mode: " + (mode != null ? mode : "N/A"));
            Log.d("QRDebug", "Extracted Purpose: " + (purpose != null ? purpose : "N/A"));
            Log.d("QRDebug", "Extracted Transaction Type: " + (transactionType != null ? transactionType : "N/A"));
            Log.d("QRDebug", "Extracted QR Medium: " + (qrMedium != null ? qrMedium : "N/A"));

                // Set default values for null fields to prevent NullPointerException
                final String finalPayeeName = payeeName != null ? payeeName : "Unknown";
                final String finalAmount = amount != null ? amount : "0";
                final String finalMerchantCode = merchantCode != null ? merchantCode : "";
                final String finalMode = mode != null ? mode : "";
                final String finalPurpose = purpose != null ? purpose : "";
                final String finalTransactionType = transactionType != null ? transactionType : "";
                final String finalQrMedium = qrMedium != null ? qrMedium : "";
                final String finalUpiId = upiId;

                // Make sure we're on the UI thread when starting a new activity
                runOnUiThread(() -> {
                    try {
                        // Clean up camera resources before starting new activity
                        if (cameraProvider != null) {
                            cameraProvider.unbindAll();
                        }

            // Start the next activity with the extracted data
            Intent i = new Intent(HomeActivity.this, AmountActivity.class);
            i.putExtra("decoded_data", rawValue);
                        i.putExtra("upi_id", finalUpiId);
                        i.putExtra("payee_name", finalPayeeName);
                        i.putExtra("amount", finalAmount);
                        i.putExtra("merchant_code", finalMerchantCode);
                        i.putExtra("mode", finalMode);
                        i.putExtra("purpose", finalPurpose);
                        i.putExtra("transaction_type", finalTransactionType);
                        i.putExtra("qr_medium", finalQrMedium);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Log.d("QRDebug", "Starting AmountActivity with extracted data");
            startActivity(i);
                    } catch (Exception e) {
                        Log.e("QRDebug", "Error starting AmountActivity: " + e.getMessage(), e);
                        Toast.makeText(HomeActivity.this, "Error processing QR code", Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            Log.e("QRDebug", "Invalid UPI ID: " + upiId);
                runOnUiThread(() -> Toast.makeText(this, "Invalid UPI ID in QR Code", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e("QRDebug", "Error parsing UPI data: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(this, "Error processing QR code", Toast.LENGTH_SHORT).show());
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

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::universalUPIScanner);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                );
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void openGallery() {
        // Implementation of openGallery method
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Cleaning up camera resources");
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reinitializing camera");
        
        // Reinitialize camera executor if needed
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }
        
        // Reinitialize barcode scanner if needed
        if (barcodeScanner == null) {
            barcodeScanner = BarcodeScanning.getClient();
        }
        
        // Start camera if permissions are granted
        if (allPermissionsGranted()) {
            new Handler().postDelayed(() -> {
                try {
                    if (cameraProvider != null) {
                        cameraProvider.unbindAll();
                    }
                    accessCamera();
                } catch (Exception e) {
                    Log.e(TAG, "Error restarting camera in onResume", e);
                    Toast.makeText(this, "Failed to restart camera. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }, 500); // Small delay to ensure resources are properly released
        }
    }
}