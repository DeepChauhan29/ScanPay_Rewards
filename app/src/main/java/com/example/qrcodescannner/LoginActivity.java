package com.example.qrcodescannner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "userName";
    
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView signupLink;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize SharedPreferences and DatabaseHelper
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(this);

        // Check if database exists and is valid
        File dbFile = getDatabasePath("transactions.db");
        if (!dbFile.exists()) {
            // Database doesn't exist, clear any stored preferences
            sharedPreferences.edit().clear().apply();
        }

        // Check if user is already logged in
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            String storedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            if (!storedEmail.isEmpty()) {
                // Verify if the user exists in database and is verified
                if (databaseHelper.isEmailVerified(storedEmail)) {
                    // User is verified and logged in, go to HomeActivity
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                    return;
                } else {
                    // User exists but not verified, clear login state
                    sharedPreferences.edit().clear().apply();
                }
            }
        }

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        signupLink = findViewById(R.id.signupLink);

        // Restore last used email if available
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
        if (!savedEmail.isEmpty()) {
            emailEditText.setText(savedEmail);
        }

        // Set click listeners
        loginButton.setOnClickListener(v -> handleLogin());
        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // Check if user exists
        if (!databaseHelper.checkEmail(email)) {
            Toast.makeText(LoginActivity.this, "User does not exist. Please sign up first.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            return;
        }

        // Verify credentials
        if (databaseHelper.verifyUser(email, password)) {
            // Get username from database
            String username = databaseHelper.getUsername(email);
            
            // Log the values for debugging
            Log.d(TAG, "User verified - Username: '" + username + "', Email: '" + email + "'");
            
            // Save login state
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("email", email)
                .putString("userName", username)
                .apply();
                
            Log.d(TAG, "Login state saved to SharedPreferences");

            // Go to home screen
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Check if user is verified
            if (!databaseHelper.isEmailVerified(email)) {
                Toast.makeText(LoginActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }
    }

    public static void logout(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}