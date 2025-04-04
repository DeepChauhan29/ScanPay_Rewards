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
import android.util.Patterns;

public class SignupActivity extends AppCompatActivity {
    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button signupButton;
    private ProgressBar progressBar;
    private TextView loginLink;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.progressBar);
        loginLink = findViewById(R.id.loginLink);

        // Set click listeners
        signupButton.setOnClickListener(v -> handleSignup());
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleSignup() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);

        // Check if email already exists
        if (databaseHelper.checkEmail(email)) {
            // Check if user is verified
            if (databaseHelper.isEmailVerified(email)) {
                Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                // User exists but not verified, generate new OTP and allow verification
                String otp = generateOTP();
                if (databaseHelper.saveOTP(email, otp)) {
                    Intent intent = new Intent(SignupActivity.this, OTPVerificationActivity.class);
                    intent.putExtra(OTPVerificationActivity.EXTRA_EMAIL, email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Error generating OTP. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
            return;
        }

        // Generate OTP for new user
        String otp = generateOTP();

        // Save new user and OTP to database
        if (databaseHelper.addUser(username, email, password, otp)) {
            // Launch OTP verification screen
            Intent intent = new Intent(SignupActivity.this, OTPVerificationActivity.class);
            intent.putExtra(OTPVerificationActivity.EXTRA_EMAIL, email);
            startActivity(intent);
            finish();
        } else {
            // Show error
            Toast.makeText(this, "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
        }
    }

    private String generateOTP() {
        // Generate a 6-digit OTP
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}