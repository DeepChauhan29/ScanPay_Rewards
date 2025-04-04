package com.example.qrcodescannner;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OTPVerificationActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "email";
    
    private EditText otpEditText;
    private Button verifyButton;
    private Button resendButton;
    private ProgressBar progressBar;
    private TextView emailTextView;
    private DatabaseHelper databaseHelper;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Get email from intent
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Initialize views
        otpEditText = findViewById(R.id.otpEditText);
        verifyButton = findViewById(R.id.verifyButton);
        resendButton = findViewById(R.id.resendButton);
        progressBar = findViewById(R.id.progressBar);
        emailTextView = findViewById(R.id.emailTextView);

        // Set email text
        emailTextView.setText("Enter the OTP sent to: " + email);

        // Show OTP in toast message
        String otp = databaseHelper.getOTP(email);
        if (otp != null) {
            Toast.makeText(this, "Your OTP is: " + otp, Toast.LENGTH_LONG).show();
        }

        // Set click listeners
        verifyButton.setOnClickListener(v -> verifyOTP());
        resendButton.setOnClickListener(v -> resendOTP());
    }

    private void verifyOTP() {
        String otp = otpEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(otp)) {
            otpEditText.setError("OTP is required");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(false);
        resendButton.setEnabled(false);

        // Verify OTP
        if (databaseHelper.verifyOTP(email, otp)) {
            // OTP verified successfully
            Toast.makeText(this, "Email verified successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(OTPVerificationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // OTP verification failed
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }

        // Hide progress bar and enable buttons
        progressBar.setVisibility(View.GONE);
        verifyButton.setEnabled(true);
        resendButton.setEnabled(true);
    }

    private void resendOTP() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(false);
        resendButton.setEnabled(false);

        // Generate and save new OTP
        String otp = generateOTP();
        if (databaseHelper.saveOTP(email, otp)) {
            // OTP saved successfully
            Toast.makeText(this, "New OTP: " + otp, Toast.LENGTH_LONG).show();
        } else {
            // Failed to save OTP
            Toast.makeText(this, "Error generating OTP", Toast.LENGTH_SHORT).show();
        }

        // Hide progress bar and enable buttons
        progressBar.setVisibility(View.GONE);
        verifyButton.setEnabled(true);
        resendButton.setEnabled(true);
    }

    private String generateOTP() {
        // Generate a 6-digit OTP
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
} 