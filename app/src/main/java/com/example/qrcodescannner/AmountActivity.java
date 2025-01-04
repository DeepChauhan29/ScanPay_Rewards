package com.example.qrcodescannner;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import androidx.cardview.widget.CardView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.qrcodescanner.R;

import java.text.NumberFormat;
import java.util.Locale;

public class AmountActivity extends AppCompatActivity {

    private TextView payeeNameTextView;
    private TextView upiIdTextView;
    private Button payButton;
    private EditText amountEditText;

    private CardView gpayCard, credpayCard;
    private boolean isGpaySelected = true; // Set Google Pay as default selected
    private boolean isSelectionLocked = false; // New variable to lock selection

    private static final int MAX_AMOUNT = 100000; // Maximum amount limit
    private boolean isFormatting; // Flag to prevent recursive calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        initializeViews();
        setupListeners();
        handleIntentData();
        requestFocusOnAmountEditText();

        // Set Google Pay as the default selected option visually
        selectPaymentMethod(true);
    }

    private void initializeViews() {
        payeeNameTextView = findViewById(R.id.PayeeName);
        upiIdTextView = findViewById(R.id.UpiID);
        amountEditText = findViewById(R.id.amount);
        payButton = findViewById(R.id.payButton);
        Button backButton = findViewById(R.id.bk);
        gpayCard = findViewById(R.id.gpaycard);
        credpayCard = findViewById(R.id.credcard);
    }

    private void setupListeners() {
        amountEditText.addTextChangedListener(new AmountTextWatcher());
        payButton.setOnClickListener(v -> handlePayment());
        gpayCard.setOnClickListener(v -> selectPaymentMethod(true));
        credpayCard.setOnClickListener(v -> selectPaymentMethod(false));
        findViewById(R.id.bk).setOnClickListener(v -> finish());
    }

    private void handleIntentData() {
        String qrCodeData = getIntent().getStringExtra("decoded_data");
        if (qrCodeData != null) {
            parseUPIData(qrCodeData);
        } else {
            Toast.makeText(this, "No QR code data found", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestFocusOnAmountEditText() {
        amountEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(amountEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void selectPaymentMethod(boolean isGpay) {
        if (isSelectionLocked) {
            Toast.makeText(this, "You cannot change the payment method at this time.", Toast.LENGTH_SHORT).show();
            return; // Prevent changing the selection if locked
        }
        this.isGpaySelected = isGpay;

        // Define colors
        int selectedColor = ContextCompat.getColor(this, R.color.green); // Green color for selected
        int unselectedColor = ContextCompat.getColor(this, R.color.black_shade_1); // Color for unselected

        // Set the background color for the selected and unselected states
        gpayCard.setCardBackgroundColor(isGpay ? selectedColor : unselectedColor);
        credpayCard.setCardBackgroundColor(isGpay ? unselectedColor : selectedColor);
    }

    private void handlePayment() {
        String amount = amountEditText.getText().toString().replace(",", ""); // Get the amount without commas
        if (TextUtils.isEmpty(amount)) {
            Toast.makeText(AmountActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return; // Prevent navigation
        }
        if (!isGpaySelected && !credpayCard.isSelected()) {
            Toast.makeText(AmountActivity.this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return; // Prevent navigation
        }

        // Lock the selection after initiating payment
        isSelectionLocked = true;

        if (isGpaySelected) {
            launchGPay(Integer.parseInt(amount));
        } else {
            launchCred(Integer.parseInt(amount));
        }
    }

    private class AmountTextWatcher implements TextWatcher {
        private String current = "";

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No action needed before text changes
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (isFormatting) return; // Prevent recursive calls

            String amount = s.toString().replaceAll(",", "").trim(); // Remove commas for processing
            if (!TextUtils.isEmpty(amount)) {
                try {
                    int enteredAmount = Integer.parseInt(amount);
                    if (enteredAmount > MAX_AMOUNT) {
                        Toast.makeText(AmountActivity.this, "You can't enter an amount more than ₹1,00,000", Toast.LENGTH_SHORT).show();
                        amountEditText.setText(current); // Reset to previous valid input
                        amountEditText.setSelection(current.length()); // Move cursor to the end
                    } else {
                        isFormatting = true; // Set the flag to true
                        String formattedAmount = NumberFormat.getInstance(Locale.US).format(enteredAmount);
                        current = formattedAmount; // Update current to the formatted amount
                        amountEditText.setText(formattedAmount);
                        amountEditText.setSelection(formattedAmount.length()); // Move cursor to the end
                        payButton.setText("Pay ₹" +formattedAmount);
                        payButton.setEnabled(true); // Enable the pay button
                        isFormatting = false; // Reset the flag
                    }
                } catch (NumberFormatException e) {
                    payButton.setText("Pay ₹");
                    payButton.setEnabled(false); // Disable the pay button if input is invalid
                }
            } else {
                payButton.setText("Pay ₹"); // Reset to default if empty
                payButton.setEnabled(false); // Disable the pay button if input is empty
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // No action needed after text changes
        }
    }

    private void parseUPIData(String qrCodeData) {
        String[] dataParts = qrCodeData.split("&");
        for (String part : dataParts) {
            if (part.startsWith("pn=")) {
                String payeeName = part.substring(3).replace("+", " ");
                payeeNameTextView.setText(payeeName);
            } else if (part.startsWith("pa=")) {
                String upiId = part.substring(3);
                upiIdTextView.setText(Uri.decode(upiId));
            }
        }
    }

    private void launchGPay(int amount) {
        launchPayment("com.google.android.apps.nbu.paisa.user", amount);
    }

    private void launchCred(int amount) {
        launchPayment("com.dreamplug.androidapp", amount);
    }

    private void launchPayment(String packageName, int amount) {
        String upiUri = "upi://pay?pa=" + upiIdTextView.getText().toString() +
                "&pn=" + payeeNameTextView.getText().toString() +
                "&am=" + amount +
                "&cu=INR" +
                "&tn=Payment for your order";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(upiUri));

        if (isAppInstalled(packageName)) {
            intent.setPackage(packageName);
            startActivity(intent);
        } else {
            Toast.makeText(this, packageName.equals("com.google.android.apps.nbu.paisa.user") ? "Google Pay is not installed" : "Cred is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true; // App is installed
        } catch (PackageManager.NameNotFoundException e) {
            return false; // App is not installed
        }
    }
}
