package com.example.qrcodescannner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qrcodescanner.R;

import java.text.NumberFormat;
import java.util.Locale;

public class AmountActivity extends AppCompatActivity {

    private TextView payeeNameTextView;
    private TextView upiIdTextView;
    private Button payButton;
    private EditText amountEditText;

    private static final int MAX_AMOUNT = 100000; // Maximum amount limit
    private boolean isFormatting; // Flag to prevent recursive calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount); // Ensure this matches your XML layout file name

        payeeNameTextView = findViewById(R.id.PayeeName);
        upiIdTextView = findViewById(R.id.UpiID);
        amountEditText = findViewById(R.id.amount);
        payButton = findViewById(R.id.payButton);
        Button backButton = findViewById(R.id.bk);

        // Get the decoded QR code data from the intent
        String qrCodeData = getIntent().getStringExtra("decoded_data");
        if (qrCodeData != null) {
            parseUPIData(qrCodeData);
        } else {
            Toast.makeText(this, "No QR code data found", Toast.LENGTH_SHORT).show();
        }

        // Request focus and show keyboard for the amount EditText
        amountEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(amountEditText, InputMethodManager.SHOW_IMPLICIT);

        // Add TextWatcher to update the pay button text and validate input
        amountEditText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) return; // Prevent recursive calls

                String amount = s.toString().replaceAll(",", "").trim(); // Remove commas for processing
                if (!amount.isEmpty()) {
                    try {
                        int enteredAmount = Integer.parseInt(amount);
                        if (enteredAmount > MAX_AMOUNT) {
                            // Show a toast message and do not change the input
                            Toast.makeText(AmountActivity.this, "You can't enter an amount more than ₹1,00,000", Toast.LENGTH_SHORT).show();
                            amountEditText.setText(current); // Reset to previous valid input
                            amountEditText.setSelection(current.length()); // Move cursor to the end
                        } else {
                            // Format the amount with commas
                            isFormatting = true; // Set the flag to true
                            String formattedAmount = NumberFormat.getInstance(Locale.US).format(enteredAmount);
                            current = formattedAmount; // Update current to the formatted amount
                            amountEditText.setText(formattedAmount);
                            amountEditText.setSelection(formattedAmount.length()); // Move cursor to the end
                            payButton.setText("Pay ₹" + formattedAmount);
                            isFormatting = false; // Reset the flag
                        }
                    } catch (NumberFormatException e) {
                        // Handle the case where the input is not a valid number
                        payButton.setText("Pay ₹");
                    }
                } else {
                    payButton.setText("Pay ₹"); // Reset to default if empty
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });

        // Set up button listeners
        payButton.setOnClickListener(v -> {
            // Create an intent to start the UpiAppSelection activity
            Intent intent = new Intent(AmountActivity.this, UpiAppSelection.class);
            intent.putExtra("payee_name", payeeNameTextView.getText().toString());
            intent.putExtra("upi_id", upiIdTextView.getText().toString());
            intent.putExtra("amount", amountEditText.getText().toString().replace(",", "")); // Pass amount without commas
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            finish(); // Go back to the previous activity
        });
    }

    private void parseUPIData(String qrCodeData) {

        try {
            Uri uri = Uri.parse(qrCodeData);
            String upiId = uri.getQueryParameter("pa");
            String payeeName = uri.getQueryParameter("pn");

            // Set the extracted data to the TextViews
            upiIdTextView.setText(upiId != null ? upiId : "UPI ID not found");
            payeeNameTextView.setText(payeeName != null ? payeeName : "Payee name not found");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing UPI data", Toast.LENGTH_SHORT).show();
        }
    }
}