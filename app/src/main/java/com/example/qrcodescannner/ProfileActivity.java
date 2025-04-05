package com.example.qrcodescannner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private TextView nameTextView, emailTextView;
    private Button editProfileButton, changePasswordButton, logoutButton;
    private SharedPreferences sharedPreferences;
    
    private static final int EDIT_PROFILE_REQUEST = 1;
    private static final String KEY_USERNAME = "userName";
    private static final String KEY_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize views
        nameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        editProfileButton = findViewById(R.id.editProfileButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        logoutButton = findViewById(R.id.logoutButton);
        
        // Get shared preferences
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        
        // Load user data
        loadUserData();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Initialize bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_transactions) {
                startActivity(new Intent(getApplicationContext(), TransactionsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
    
    private void loadUserData() {
        // Get user data from SharedPreferences
        String userName = sharedPreferences.getString(KEY_USERNAME, "");
        String userEmail = sharedPreferences.getString(KEY_EMAIL, "");
        
        // Log the values for debugging
        Log.d(TAG, "Loading user data - Username: '" + userName + "', Email: '" + userEmail + "'");
        
        // If username is empty, try to get it from the database
        if (userName.isEmpty() && !userEmail.isEmpty()) {
            DatabaseHelper databaseHelper = new DatabaseHelper(this);
            userName = databaseHelper.getUsername(userEmail);
            Log.d(TAG, "Retrieved username from database: '" + userName + "'");
            
            // Save the username to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_USERNAME, userName);
            editor.apply();
        }

        // Set the text views
        nameTextView.setText(userName);
        emailTextView.setText(userEmail);
    }
    
    private void setupButtonListeners() {
        editProfileButton.setOnClickListener(v -> {
            String currentName = sharedPreferences.getString(KEY_USERNAME, "");
            String currentEmail = sharedPreferences.getString(KEY_EMAIL, "");
            
            Log.d(TAG, "Opening EditProfileActivity - Current Name: '" + currentName + "', Current Email: '" + currentEmail + "'");
            
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("userName", currentName);
            intent.putExtra("userEmail", currentEmail);
            startActivityForResult(intent, EDIT_PROFILE_REQUEST);
        });
        
        changePasswordButton.setOnClickListener(v -> {
            // Open change password activity
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });
        
        logoutButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Clear login state
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();
                        
                        // Redirect to login
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "onActivityResult - Request Code: " + requestCode + ", Result Code: " + resultCode);
        
        if (requestCode == EDIT_PROFILE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                // Get updated data
                String newName = data.getStringExtra("newUserName");
                String newEmail = data.getStringExtra("newUserEmail");
                
                Log.d(TAG, "Profile edit successful - New Name: '" + newName + "', New Email: '" + newEmail + "'");
                
                // Update shared preferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_USERNAME, newName);
                editor.putString(KEY_EMAIL, newEmail);
                editor.apply();
                
                Log.d(TAG, "SharedPreferences updated with new profile data");
                
                // Reload user data
                loadUserData();
                
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Profile edit was canceled by user");
            } else {
                Log.e(TAG, "Profile edit failed or returned invalid result");
            }
        }
    }
} 