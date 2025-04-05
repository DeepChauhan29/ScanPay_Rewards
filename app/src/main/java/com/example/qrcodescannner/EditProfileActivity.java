package com.example.qrcodescannner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    private EditText nameEditText;
    private EditText emailEditText;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize DatabaseHelper and SharedPreferences
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        Button saveButton = findViewById(R.id.saveButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        // Get current user data from intent
        String currentName = getIntent().getStringExtra("userName");
        String currentEmail = getIntent().getStringExtra("userEmail");
        
        Log.d(TAG, "EditProfileActivity started - Current Name: '" + currentName + "', Current Email: '" + currentEmail + "'");

        // Set current data to EditTexts
        nameEditText.setText(currentName);
        emailEditText.setText(currentEmail);

        // Set click listeners
        saveButton.setOnClickListener(v -> {
            String newName = nameEditText.getText().toString().trim();
            String newEmail = emailEditText.getText().toString().trim();

            // Validate input
            if (TextUtils.isEmpty(newName)) {
                nameEditText.setError("Name is required");
                return;
            }
            if (TextUtils.isEmpty(newEmail)) {
                emailEditText.setError("Email is required");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                emailEditText.setError("Please enter a valid email");
                return;
            }

            // Check if the new email is already in use by another user
            if (!newEmail.equals(currentEmail) && databaseHelper.checkEmail(newEmail)) {
                emailEditText.setError("This email is already in use");
                return;
            }

            // Update the database
            boolean success = databaseHelper.updateUserProfile(currentEmail, newEmail, newName);
            
            Log.d(TAG, "Attempting to update profile - Old Email: '" + currentEmail + "', New Email: '" + newEmail + "', New Name: '" + newName + "'");
            
            if (success) {
                // Verify the update was successful
                boolean verified = databaseHelper.verifyUserProfile(newEmail, newName);
                
                if (!verified) {
                    Log.e(TAG, "Profile update verification failed");
                    Toast.makeText(this, "Failed to verify profile update", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userName", newName);
                editor.putString("email", newEmail);
                editor.apply();
                
                Log.d(TAG, "Profile updated and verified successfully");
                
                // Create result intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra("newUserName", newName);
                resultIntent.putExtra("newUserEmail", newEmail);
                setResult(RESULT_OK, resultIntent);
                finish();
                
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to update profile in database");
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
} 