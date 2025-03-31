package com.example.qrcodescannner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {
    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button signupButton;
    private TextView loginLink;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        databaseHelper = new DatabaseHelper(this);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.signupButton);
        loginLink = findViewById(R.id.loginLink);

        signupButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.checkEmail(email)) {
                Toast.makeText(SignupActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.addUser(username, email, password)) {
                Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(SignupActivity.this, "Signup failed", Toast.LENGTH_SHORT).show();
            }
        });

        loginLink.setOnClickListener(v -> finish());
    }
}