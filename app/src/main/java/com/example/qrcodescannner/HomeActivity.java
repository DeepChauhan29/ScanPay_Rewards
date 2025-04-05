package com.example.qrcodescannner;

import android.Manifest;
import android.app.Activity;
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
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    // Add new constant for Android 13+ media permission
    private static final String READ_MEDIA_IMAGES_PERMISSION = Manifest.permission.READ_MEDIA_IMAGES;
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private TextView welcomeText, userNameText, totalTransactions, totalAmountPaid;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private static final long AUTO_PAUSE_DELAY = 3000; // 3 seconds
    private Handler autoPauseHandler;
    private FrameLayout tapToScanOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize auto-pause handler
        autoPauseHandler = new Handler();
        
        // Initialize views
        welcomeText = findViewById(R.id.welcomeText);
        userNameText = findViewById(R.id.userNameText);
        totalTransactions = findViewById(R.id.totalTransactions);
        totalAmountPaid = findViewById(R.id.totalAmountPaid);
        previewView = findViewById(R.id.previewView);
        tapToScanOverlay = findViewById(R.id.tapToScanOverlay);
        
        // Initialize DatabaseHelper and SharedPreferences
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        
        // Initialize camera components
        barcodeScanner = BarcodeScanning.getClient();
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Initialize gallery launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedImageUri = data.getData();
                        processQRCodeFromGallery(selectedImageUri);
                    }
                }
            }
        );
        
        // Load user data and update UI
        loadUserData();
        updateStatistics();
        
        // Verify login state
        String storedEmail = sharedPreferences.getString("email", "");
        if (storedEmail.isEmpty()) {
            // No stored email, redirect to login
            Intent i = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }
        
        // Verify if user exists in database and is verified
        if (!databaseHelper.checkEmail(storedEmail)) {
            // User doesn't exist in database, clear login state and redirect to login
            sharedPreferences.edit().clear().apply();
            Intent i = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }

        // Initialize FloatingActionButtons
        FloatingActionButton scanFab = findViewById(R.id.scanButton);
        FloatingActionButton galleryFab = findViewById(R.id.gallery);
        
        scanFab.setOnClickListener(v -> startCamera());
        galleryFab.setOnClickListener(v -> openGallery());

        // Initialize bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
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
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        setupTapToResumeOverlay();
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
        // Always check camera permission
        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        
        // Check appropriate storage permission based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above - use READ_MEDIA_IMAGES
            return ContextCompat.checkSelfPermission(getBaseContext(), READ_MEDIA_IMAGES_PERMISSION) 
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 and below - use READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (permissions.length > 0) {
                String permission = permissions[0];
                if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) || 
                    permission.equals(READ_MEDIA_IMAGES_PERMISSION)) {
                    // This was a storage permission request
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Storage permission granted, open gallery
                        openGallery();
                    } else {
                        Toast.makeText(this, "Storage permission is required to access gallery", Toast.LENGTH_SHORT).show();
                    }
                } else if (permission.equals(Manifest.permission.CAMERA)) {
                    // This was a camera permission request
                    boolean allGranted = true;
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false;
                            break;
                        }
                    }
                    
                    if (allGranted) {
                startCamera();
            } else {
                        Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Unbind all use cases before binding new ones
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }

                // Get camera provider
                cameraProvider = cameraProviderFuture.get();

                // Create preview use case
                preview = new Preview.Builder()
                    .setTargetRotation(previewView.getDisplay().getRotation())
                    .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Create image analysis use case
                imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetRotation(previewView.getDisplay().getRotation())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::universalUPIScanner);

                // Create camera selector
                CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

                try {
                    // Bind use cases to camera
                    Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    );
                    cameraControl = camera.getCameraControl();
                    
                    // Hide the overlay when camera starts
                    if (tapToScanOverlay != null) {
                        tapToScanOverlay.setVisibility(View.GONE);
                    }
                    
                    // Start the auto-pause timer
                    startAutoPauseTimer();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Use case binding failed", e);
                    Toast.makeText(this, "Failed to initialize camera. Please restart the app.", Toast.LENGTH_LONG).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting camera provider", e);
                Toast.makeText(this, "Failed to access camera. Please check permissions.", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void openGallery() {
        // Check for appropriate permission based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above - use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES_PERMISSION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{READ_MEDIA_IMAGES_PERMISSION}, 
                    REQUEST_CODE_PERMISSIONS);
                return;
            }
        } else {
            // Android 12 and below - use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    REQUEST_CODE_PERMISSIONS);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void processQRCodeFromGallery(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {
                        Toast.makeText(this, "No QR code found in the image", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    for (Barcode barcode : barcodes) {
                        if (barcode.getValueType() == Barcode.TYPE_TEXT) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null) {
                                parseUPIData(rawValue);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error processing QR code from gallery", e);
                    Toast.makeText(this, "Error processing QR code", Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from gallery", e);
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Cleaning up camera resources");
        
        // Cancel the auto-pause handler
        if (autoPauseHandler != null) {
            autoPauseHandler.removeCallbacksAndMessages(null);
        }
        
        // Pause camera and scanner
        pauseCameraAndScanner();
        
        // Release camera executor
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Initialize camera executor if needed
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }
        
        // Start camera and scanner
        resumeCameraAndScanner();
        
        // Start auto-pause timer
        startAutoPauseTimer();
    }

    private void loadUserData() {
        try {
            String userName = sharedPreferences.getString("userName", "");
            if (userNameText != null) {
                userNameText.setText(userName);
                            }
                        } catch (Exception e) {
            Log.e(TAG, "Error loading user data: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        try {
            if (totalTransactions != null && totalAmountPaid != null) {
                // Get total transactions
                int transactionCount = databaseHelper.getTotalTransactionCount();
                totalTransactions.setText(String.valueOf(transactionCount));

                // Get total amount paid
                double totalAmountPaidValue = databaseHelper.getTotalAmountPaid();
                totalAmountPaid.setText("₹" + String.format("%.2f", totalAmountPaidValue));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating statistics: " + e.getMessage());
        }
    }

    private void setupTapToResumeOverlay() {
        if (tapToScanOverlay != null) {
            tapToScanOverlay.setOnClickListener(v -> {
                Log.d(TAG, "Preview tapped to resume scanner and camera");
                resumeCameraAndScanner();
                startAutoPauseTimer(); // Restart the auto-pause timer
            });
        }
    }

    private void pauseCameraAndScanner() {
        if (cameraProvider != null) {
            try {
                // Cancel any existing timer
                if (autoPauseHandler != null) {
                    autoPauseHandler.removeCallbacksAndMessages(null);
                }
                
                // Unbind all use cases to pause the camera
                cameraProvider.unbindAll();
                Log.d(TAG, "Camera and scanner paused successfully");
                
                // Show the tap to resume overlay
                if (tapToScanOverlay != null) {
                    tapToScanOverlay.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error pausing camera", e);
            }
        }
    }

    private void resumeCameraAndScanner() {
        if (cameraProvider != null) {
            try {
                // Hide the tap to resume overlay
                if (tapToScanOverlay != null) {
                    tapToScanOverlay.setVisibility(View.GONE);
                }
                
                // Start the camera
                startCamera();
                Log.d(TAG, "Camera and scanner resumed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error resuming camera", e);
            }
        } else {
            // If cameraProvider is null, try to start the camera from scratch
            if (allPermissionsGranted()) {
                startCamera();
            }
        }
    }

    private void startAutoPauseTimer() {
        // Cancel any existing timer
        if (autoPauseHandler != null) {
            autoPauseHandler.removeCallbacksAndMessages(null);
        }
        
        // Start new timer
        autoPauseHandler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && cameraProvider != null) {
                runOnUiThread(() -> {
                    pauseCameraAndScanner();
                });
            }
        }, AUTO_PAUSE_DELAY);
    }
}