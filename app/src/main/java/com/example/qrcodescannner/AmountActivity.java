package com.example.qrcodescannner;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.qrcodescannner.R;

import java.text.NumberFormat;
import java.util.Locale;

public class AmountActivity extends AppCompatActivity {

    private TextView payeeNameTextView;
    private TextView upiIdTextView;
    private Button payButton;
    private EditText amountEditText;

    private CardView gpayCard, credpayCard, naviCard, supermoneyCard, paytmCard, phonepeCard;
    private RadioButton gpayRadio, credRadio, naviRadio, supermoneyRadio, paytmRadio, phonepeRadio;

    private boolean isGpaySelected = true; // Set Gpay as default selected
    private boolean isCredSelected = false;
    private boolean isNaviSelected = false;
    private boolean isSuperSelected = false;
    private boolean isPaytmSelected = false;
    private boolean isPhonepeSelected = false;

    private static final int MAX_AMOUNT = 100000; // Maximum amount limit

    // Play Store links
    private String credPlayStoreLink = "https://play.google.com/store/apps/details?id=com.dreamplug.androidapp"; // CRED Play Store link
    private String naviPlayStoreLink = "https://play.google.com/store/apps/details?id=com.navi.android"; // Navi Play Store link
    private String supermoneyPlayStoreLink = "https://play.google.com/store/apps/details?id=money.super.payments&hl=en"; // Supermoney Play Store link
    private String paytmPlayStoreLink = "https://play.google.com/store/apps/details?id=net.one97.paytm"; // Paytm Play Store link
    private String phonepePlayStoreLink = "https://play.google.com/store/apps/details?id=com.phonepe.app"; // PhonePe Play Store link

    private boolean isFormatting; // Flag to prevent recursive calls

    // Variables to hold dynamic values
    private String merchantCode;
    private String mode;
    private String purpose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        initializeViews();
        setupListeners();
        handleIntentData();
        requestFocusOnAmountEditText();
        updateCardSelection(); // Set default card selection
    }

    private void initializeViews() {
        payeeNameTextView = findViewById(R.id.PayeeName);
        upiIdTextView = findViewById(R.id.UpiID);
        amountEditText = findViewById(R.id.amount);
        payButton = findViewById(R.id.payButton);

        gpayCard = findViewById(R.id.gpaycard);
        credpayCard = findViewById(R.id.credcard);
        naviCard = findViewById(R.id.navicard);
        supermoneyCard = findViewById(R.id.supermoneycard);
        paytmCard = findViewById(R.id.paytmcard);
        phonepeCard = findViewById(R.id.phonepecard);

        gpayRadio = findViewById(R.id.gpayRadioButton);
        credRadio = findViewById(R.id.credRadioButton);
        naviRadio = findViewById(R.id.naviRadioButton);
        supermoneyRadio = findViewById(R.id.superRadioButton);
        paytmRadio = findViewById(R.id.paytmRadioButton);
        phonepeRadio = findViewById(R.id.phonepeRadioButton);
    }

    private void setupListeners() {
        amountEditText.addTextChangedListener(new AmountTextWatcher());
        payButton.setOnClickListener(v -> handlePayment());

        gpayRadio.setOnClickListener(v -> {
            isGpaySelected = true;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = false;
            updateCardSelection();
        });

        credRadio.setOnClickListener(v -> {
            isGpaySelected = false;
            isCredSelected = true;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = false;
            updateCardSelection();
        });

        naviRadio.setOnClickListener(v -> {
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = true;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = false;
            updateCardSelection();
        });

        supermoneyRadio.setOnClickListener(v -> {
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = true;
            isPaytmSelected = false;
            isPhonepeSelected = false;
            updateCardSelection();
        });

        paytmRadio.setOnClickListener(v -> {
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = true;
            isPhonepeSelected = false;
            updateCardSelection();
        });

        phonepeRadio.setOnClickListener(v -> {
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = true;
            updateCardSelection();
        });

        findViewById(R.id.bk).setOnClickListener(v -> finish());
    }

    private void updateCardSelection() {
        int selectedColor = ContextCompat.getColor(this, R.color.green); // Green color for selected
        int unselectedColor = ContextCompat.getColor(this, R.color.black_shade_1);
        gpayCard.setCardBackgroundColor(isGpaySelected ? selectedColor : unselectedColor);
        credpayCard.setCardBackgroundColor(isCredSelected ? selectedColor : unselectedColor);
        naviCard.setCardBackgroundColor(isNaviSelected ? selectedColor : unselectedColor);
        supermoneyCard.setCardBackgroundColor(isSuperSelected ? selectedColor : unselectedColor);
        paytmCard.setCardBackgroundColor(isPaytmSelected ? selectedColor : unselectedColor);
        phonepeCard.setCardBackgroundColor(isPhonepeSelected ? selectedColor : unselectedColor);
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

    private void handlePayment() {
        String amount = amountEditText.getText().toString().replace(",", ""); // Get the amount without commas
        if (TextUtils.isEmpty(amount) || Double.parseDouble(amount) <= 0) {
            Toast.makeText(AmountActivity.this, "Please enter a valid amount greater than 0", Toast.LENGTH_SHORT).show();
            return; // Prevent navigation
        }

        // Proceed with the selected payment method
        if (isGpaySelected) {
            launchGPay(Integer.parseInt(amount));
        } else if (isCredSelected) {
            launchCred(Integer.parseInt(amount));
        } else if (isNaviSelected) {
            launchNavi(Integer.parseInt(amount));
        } else if (isSuperSelected) {
            launchSupermoney(Integer.parseInt(amount));
        } else if (isPaytmSelected) {
            launchPaytm(Integer.parseInt(amount));
        } else if (isPhonepeSelected) {
            launchPhonePe(Integer.parseInt(amount));
        }
    }

    private class AmountTextWatcher implements TextWatcher {
        private String current = "";

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (isFormatting) return; // Prevent recursive calls

            String amount = s.toString().replaceAll(",", "").trim(); // Remove commas for processing
            if (!TextUtils.isEmpty(amount)) {
                try {
                    Integer enteredAmount = Integer.parseInt(amount);
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
                        payButton.setText("Pay ₹" + formattedAmount);
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
        public void afterTextChanged(Editable s) {}
    }

    private void parseUPIData(String qrCodeData) {
        try {
            Uri uri = Uri.parse(qrCodeData);
            String upiId = uri.getQueryParameter("pa");
            String payeeName = uri.getQueryParameter("pn");
            merchantCode = uri.getQueryParameter("mc"); // Extract merchant code
            mode = uri.getQueryParameter("mode"); // Extract mode
            purpose = uri.getQueryParameter("purpose"); // Extract purpose

            // Set the extracted data to the TextViews
            upiIdTextView.setText(upiId != null ? upiId : "UPI ID not found");
            payeeNameTextView.setText(payeeName != null ? payeeName : "Payee name not found");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing UPI data", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGPay(int amount) {
        String gpayPackage = "com.google.android.apps.nbu.paisa.user";
        String rawQrData = getIntent().getStringExtra("decoded_data");
        
        if (rawQrData != null && !rawQrData.isEmpty()) {
            // Parse the original URI to preserve all parameters
            Uri originalUri = Uri.parse(rawQrData);
            String upiId = originalUri.getQueryParameter("pa");
            String payeeName = originalUri.getQueryParameter("pn");
            
            // Build a new URI with minimal parameters for Google Pay
            Uri.Builder uriBuilder = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", payeeName)
                .appendQueryParameter("am", String.valueOf(amount))
                .appendQueryParameter("cu", "INR");

            // Add merchant code only if it exists in original QR
            if (merchantCode != null) {
                uriBuilder.appendQueryParameter("mc", merchantCode);
            }

            String uri = uriBuilder.build().toString();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri));
            intent.setPackage(gpayPackage);

            try {
                if (isAppInstalled(gpayPackage)) {
                    startActivity(intent);
                } else {
                    showDownloadDialog("Google Pay Not Installed", 
                        "https://play.google.com/store/apps/details?id=" + gpayPackage);
                }
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Could not launch Google Pay", Toast.LENGTH_SHORT).show();
            }
        } else {
            // If no QR data, fallback to regular payment method
            launchPayment(gpayPackage, amount);
        }
    }

    private void launchCred(int amount) {
        launchPayment("com.dreamplug.androidapp", amount);
    }

    private void launchNavi(int amount) {
        launchPayment("com.naviapp", amount);
    }

    private void launchSupermoney(int amount) {
        launchPayment("money.super.payments", amount);
    }

    private void launchPaytm(int amount) {
        launchPayment("net.one97.paytm", amount);
    }

    private void launchPhonePe(int amount) {
        launchPayment("com.phonepe.app", amount);
    }

    private void launchPayment(String packageName, int amount) {
        // Get the raw QR code data that was originally scanned
        String rawQrData = getIntent().getStringExtra("decoded_data");
        
        if (rawQrData != null && !rawQrData.isEmpty()) {
            // Only modify the amount in the original QR data
            String baseUri = rawQrData;
            if (baseUri.contains("am=")) {
                // Replace existing amount
                baseUri = baseUri.replaceAll("am=[^&]+", "am=" + amount);
             Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(baseUri));
            intent.setPackage(packageName);
            
            try {
                if (isAppInstalled(packageName)) {
                    startActivity(intent);
                } else {
                    String playStoreLink = getPlayStoreLink(packageName);
                    showDownloadDialog("App Not Installed", playStoreLink);
                }
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Google Pay app is not installed", Toast.LENGTH_SHORT).show();
                String playStoreLink = "https://play.google.com/store/apps/details?id=" + gpayPackage;
                showDownloadDialog("Google Pay Not Installed", playStoreLink);
            }
            return;
        }

        // Fallback to manual URI construction if no raw data available
        String upiId = upiIdTextView.getText().toString().trim();
        String payeeName = payeeNameTextView.getText().toString().trim();

        if (TextUtils.isEmpty(upiId) || TextUtils.isEmpty(payeeName)) {
            Toast.makeText(this, "UPI ID and Payee Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri.Builder uriBuilder = new Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("am", String.valueOf(amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", "TXN" + System.currentTimeMillis());
upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("am", String.valueOf(amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", "TXN" + System.currentTimeMillis())
            .appendQueryParameter("mv", "1.0"); // Add minimum version

        if (!TextUtils.isEmpty(merchantCode)) {
            uriBuilder.appendQueryParameter("mc", merchantCode);
        }
        if (!TextUtils.isEmpty(mode)) {
            uriBuilder.appendQueryParameter("mode", mode);
        }
        if (!TextUtils.isEmpty(purpose)) {
            uriBuilder.appendQueryParameter("purpose", purpose);
        }

        String uri = uriBuilder.build().toString();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        intent.setPackage(packageName);

        try {
            if (isAppInstalled(packageName)) {
                startActivity(intent);
            } else {
                String playStoreLink = getPlayStoreLink(packageName);
                showDownloadDialog("App Not Installed", playStoreLink);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Could not launch payment app", Toast.LENGTH_SHORT).show();
            Intent chooser = Intent.createChooser(intent, "Pay with");
            startActivity(chooser);
        }
    }

    private boolean isAppInstalled(String packageName) {
        Log.d("AppCheck", "Checking for package: " + packageName);
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true; // App is installed
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("AppCheck", "Package not found: " + e.getMessage());
            return false; // App is not installed
        }
    }

    private String getPlayStoreLink(String packageName) {
        switch (packageName) {
            case "com.dreamplug.androidapp":
                return credPlayStoreLink;
            case "com.navi.android":
                return naviPlayStoreLink;
            case "money.super.payments":
                return supermoneyPlayStoreLink;
            case "net.one97.paytm":
                return paytmPlayStoreLink;
            case "com.phonepe.app":
                return phonepePlayStoreLink;
            default:
                return "";
        }
    }

    private void showDownloadDialog(String title, String playStoreLink) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("The selected app is not installed. Do you want to continue to the Play Store to download it?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Redirect to Play Store
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(playStoreLink));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Just close the dialog
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}