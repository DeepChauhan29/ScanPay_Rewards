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
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.widget.HorizontalScrollView;
import android.view.Gravity;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.app.Activity;
import androidx.annotation.Nullable;

import com.example.qrcodescannner.R;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AmountActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PAYMENT_REQUEST_CODE = 1001;
    private static final String TAG = "AmountActivity";

    private TextView payeeNameTextView;
    private TextView upiIdTextView;
    private Button payButton;
    private Button backButton;
    private EditText amountEditText;

    private CardView gpayCard, credpayCard, naviCard, supermoneyCard, paytmCard, phonepeCard;
    private RadioButton gpayRadio, credRadio, naviRadio, supermoneyRadio, paytmRadio, phonepeRadio;

    private boolean isGpaySelected = false; // Remove default selection
    private boolean isCredSelected = false;
    private boolean isNaviSelected = false;
    private boolean isSuperSelected = false;
    private boolean isPaytmSelected = false;
    private boolean isPhonepeSelected = false;

    private String selectedApp = ""; // Remove default selection

    private static final int MAX_AMOUNT = 100000; // Maximum amount limit
    private String credPlayStoreLink = "https://play.google.com/store/apps/details?id=com.dreamplug.androidapp"; // CRED Play Store link
    private String naviPlayStoreLink = "https://play.google.com/store/apps/details?id=com.navi.android"; // Navi Play Store link
    private String supermoneyPlayStoreLink = "https://play.google.com/store/apps/details?id=money.super.payments&hl=en"; // Supermoney Play Store link
    private String paytmPlayStoreLink = "https://play.google.com/store/apps/details?id=net.one97.paytm"; // Paytm Play Store link
    private String phonepePlayStoreLink = "https://play.google.com/store/apps/details?id=com.phonepe.app"; // PhonePe Play Store link

    private boolean isFormatting; // Flag to prevent recursive calls

    private DatabaseHelper databaseHelper;

    private List<PaymentApp> paymentApps;
    private HorizontalScrollView scrollView;
    private LinearLayout parentLayout;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

            // Initialize the database helper first
            databaseHelper = new DatabaseHelper(this);
            
            // Initialize views
        initializeViews();
            
            // Setup listeners
        setupListeners();
            
            // Handle intent data
            handleIntentData();
            
            // Request focus on amount edit text
        requestFocusOnAmountEditText();
            
            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("PaymentPrefs", MODE_PRIVATE);
            
            // Initialize payment apps list
            initializePaymentApps();
            
            // Get the scroll view and its parent layout
            scrollView = findViewById(R.id.scrollView);
            parentLayout = (LinearLayout) scrollView.getChildAt(0);
            
            // Setup initial app buttons and select first available app
            setupAppButtons();
            
        } catch (Exception e) {
            Log.e("AmountActivity", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Get today's date for transaction count
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Check if the currently selected app has reached its limit
        if (!selectedApp.isEmpty()) {
            int count = databaseHelper.getTransactionCountForApp(selectedApp, today);
            if (count >= 1) {
                // Selected app has reached its limit, update positions and select first available
                updateAppPositionsBasedOnTransactions();
            }
        } else {
            // No app selected, update positions and select first available
            updateAppPositionsBasedOnTransactions();
        }
    }
    
    // Centralized method to check transactions and update UI accordingly
    private void updateAppPositionsBasedOnTransactions() {
        try {
            // Get today's date for transaction count
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Get transaction counts for each app
            int naviCount = databaseHelper.getTransactionCountForApp("com.naviapp", today);
            int credCount = databaseHelper.getTransactionCountForApp("com.dreamplug.androidapp", today);
            int gpayCount = databaseHelper.getTransactionCountForApp("com.google.android.apps.nbu.paisa.user", today);
            int superCount = databaseHelper.getTransactionCountForApp("money.super.payments", today);
            int paytmCount = databaseHelper.getTransactionCountForApp("net.one97.paytm", today);
            int phonepeCount = databaseHelper.getTransactionCountForApp("com.phonepe.app", today);
            
            // Determine which apps are available based on transaction limits
            boolean isNaviAvailable = naviCount < 1;
            boolean isCredAvailable = credCount < 1;
            boolean isGpayAvailable = gpayCount < 1;
            boolean isSuperAvailable = superCount < 1;
            boolean isPaytmAvailable = paytmCount < 1;
            boolean isPhonepeAvailable = phonepeCount < 1;
            
            // Update cashback text for apps that reached limit
            if (!isNaviAvailable && naviCard != null) {
                updateCashbackText(naviCard, "Daily limit reached");
            }
            if (!isCredAvailable && credpayCard != null) {
                updateCashbackText(credpayCard, "Daily limit reached");
            }
            if (!isGpayAvailable && gpayCard != null) {
                updateCashbackText(gpayCard, "Daily limit reached");
            }
            if (!isSuperAvailable && supermoneyCard != null) {
                updateCashbackText(supermoneyCard, "Daily limit reached");
            }
            if (!isPaytmAvailable && paytmCard != null) {
                updateCashbackText(paytmCard, "Daily limit reached");
            }
            if (!isPhonepeAvailable && phonepeCard != null) {
                updateCashbackText(phonepeCard, "Daily limit reached");
            }
            
            // Get the parent LinearLayout inside the ScrollView
            HorizontalScrollView scrollView = findViewById(R.id.scrollView);
            if (scrollView == null) {
                Log.e("AmountActivity", "ScrollView not found");
                return;
            }
            
            LinearLayout parentLayout = (LinearLayout) scrollView.getChildAt(0);
            if (parentLayout == null) {
                Log.e("AmountActivity", "Parent layout not found");
                return;
            }
            
            // Create a list of app cards with their transaction counts
            List<View> availableCards = new ArrayList<>();
            List<View> limitReachedCards = new ArrayList<>();

            // Check each app and add to appropriate list
            if (isNaviAvailable) {
                availableCards.add(naviCard);
            } else {
                limitReachedCards.add(naviCard);
            }

            if (isCredAvailable) {
                availableCards.add(credpayCard);
            } else {
                limitReachedCards.add(credpayCard);
            }

            if (isGpayAvailable) {
                availableCards.add(gpayCard);
            } else {
                limitReachedCards.add(gpayCard);
            }

            if (isSuperAvailable) {
                availableCards.add(supermoneyCard);
            } else {
                limitReachedCards.add(supermoneyCard);
            }

            if (isPaytmAvailable) {
                availableCards.add(paytmCard);
            } else {
                limitReachedCards.add(paytmCard);
            }

            if (isPhonepeAvailable) {
                availableCards.add(phonepeCard);
            } else {
                limitReachedCards.add(phonepeCard);
            }

            // Remove all views from the layout
            parentLayout.removeAllViews();

            // Add all available cards first
            for (View card : availableCards) {
                parentLayout.addView(card);
            }

            // Add all limit reached cards at the end
            for (View card : limitReachedCards) {
                parentLayout.addView(card);
            }

            // Reset all selection states
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = false;

            // Check if the currently selected app is still available
            boolean isSelectedAppAvailable = false;
            if (!selectedApp.isEmpty()) {
                switch (selectedApp) {
                    case "com.naviapp":
                        isSelectedAppAvailable = isNaviAvailable;
                        break;
                    case "com.dreamplug.androidapp":
                        isSelectedAppAvailable = isCredAvailable;
                        break;
                    case "com.google.android.apps.nbu.paisa.user":
                        isSelectedAppAvailable = isGpayAvailable;
                        break;
                    case "money.super.payments":
                        isSelectedAppAvailable = isSuperAvailable;
                        break;
                    case "net.one97.paytm":
                        isSelectedAppAvailable = isPaytmAvailable;
                        break;
                    case "com.phonepe.app":
                        isSelectedAppAvailable = isPhonepeAvailable;
                        break;
                }
            }

            // If no app is selected or the selected app is no longer available,
            // select the first available app
            if (selectedApp.isEmpty() || !isSelectedAppAvailable) {
                if (!availableCards.isEmpty()) {
                    View firstCard = availableCards.get(0);
                    if (firstCard == naviCard) {
                        selectedApp = "com.naviapp";
                        isNaviSelected = true;
                    } else if (firstCard == credpayCard) {
                        selectedApp = "com.dreamplug.androidapp";
                        isCredSelected = true;
                    } else if (firstCard == gpayCard) {
                        selectedApp = "com.google.android.apps.nbu.paisa.user";
                        isGpaySelected = true;
                    } else if (firstCard == supermoneyCard) {
                        selectedApp = "money.super.payments";
                        isSuperSelected = true;
                    } else if (firstCard == paytmCard) {
                        selectedApp = "net.one97.paytm";
                        isPaytmSelected = true;
                    } else if (firstCard == phonepeCard) {
                        selectedApp = "com.phonepe.app";
                        isPhonepeSelected = true;
                    }
                }
            } else {
                // Maintain the current selection
                switch (selectedApp) {
                    case "com.naviapp":
                        isNaviSelected = true;
                        break;
                    case "com.dreamplug.androidapp":
                        isCredSelected = true;
                        break;
                    case "com.google.android.apps.nbu.paisa.user":
                        isGpaySelected = true;
                        break;
                    case "money.super.payments":
                        isSuperSelected = true;
                        break;
                    case "net.one97.paytm":
                        isPaytmSelected = true;
                        break;
                    case "com.phonepe.app":
                        isPhonepeSelected = true;
                        break;
                }
            }

            // Update UI to reflect current selection
            updateCardSelection();
            updatePayButton();
            
        } catch (Exception e) {
            Log.e("AmountActivity", "Error updating app positions: " + e.getMessage());
        }
    }

    private void initializeViews() {
        try {
            // Initialize TextViews
        payeeNameTextView = findViewById(R.id.PayeeName);
        upiIdTextView = findViewById(R.id.UpiID);
        amountEditText = findViewById(R.id.amount);
        payButton = findViewById(R.id.payButton);
        backButton = findViewById(R.id.bk);

            // Initialize CardViews
        gpayCard = findViewById(R.id.gpaycard);
        credpayCard = findViewById(R.id.credcard);
        naviCard = findViewById(R.id.navicard);
        supermoneyCard = findViewById(R.id.supermoneycard);
        paytmCard = findViewById(R.id.paytmcard);
        phonepeCard = findViewById(R.id.phonepecard);

            // Initialize RadioButtons
        gpayRadio = findViewById(R.id.gpayRadioButton);
        credRadio = findViewById(R.id.credRadioButton);
        naviRadio = findViewById(R.id.naviRadioButton);
        supermoneyRadio = findViewById(R.id.superRadioButton);
        paytmRadio = findViewById(R.id.paytmRadioButton);
        phonepeRadio = findViewById(R.id.phonepeRadioButton);

            // Verify all views are initialized
            if (payeeNameTextView == null || upiIdTextView == null || amountEditText == null || payButton == null ||
                gpayCard == null || credpayCard == null || naviCard == null || supermoneyCard == null ||
                paytmCard == null || phonepeCard == null || gpayRadio == null || credRadio == null ||
                naviRadio == null || supermoneyRadio == null || paytmRadio == null || phonepeRadio == null) {
                throw new Exception("Failed to initialize all views");
            }

            // Set initial state
            payButton.setEnabled(false);
            payButton.setText("Pay ₹");
        } catch (Exception e) {
            Log.e("AmountActivity", "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupListeners() {
        try {
            // Setup text watcher for amount
        amountEditText.addTextChangedListener(new AmountTextWatcher());
            
            // Setup pay button click listener
        payButton.setOnClickListener(v -> handlePayment());

            // Setup back button click listener
            backButton.setOnClickListener(v -> onBackPressed());
            
            // Get all the app cards and radio buttons
            CardView gPayCard = findViewById(R.id.gpaycard);
            CardView credCard = findViewById(R.id.credcard);
            CardView naviCard = findViewById(R.id.navicard);
            CardView superMoneyCard = findViewById(R.id.supermoneycard);
            CardView paytmCard = findViewById(R.id.paytmcard);
            CardView phonepeCard = findViewById(R.id.phonepecard);
            
            // Get transaction counts to determine which app to prioritize
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int naviCount = databaseHelper.getTransactionCountForApp("com.naviapp", today);
            
            // If Navi is under transaction limit, ensure it appears first in the scrollview
            if (naviCount < 1) {
                // Get the parent LinearLayout of the cards
                HorizontalScrollView scrollView = findViewById(R.id.scrollView);
                if (scrollView != null) {
                    LinearLayout parentLayout = (LinearLayout) scrollView.getChildAt(0);
                    if (parentLayout != null && naviCard != null) {
                        // Make Navi visible and set it as selected
                        naviCard.setVisibility(View.VISIBLE);
                        
                        // Set Navi as the default selected app
                        selectedApp = "com.naviapp";
                        
                        // Force scroll to the beginning to show Navi
                        scrollView.post(() -> {
                            scrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                            scrollView.scrollTo(0, 0);
                        });
                    }
                }
            }
            
            // Set up the radio button click listeners
            RadioButton gpayRadio = findViewById(R.id.gpayRadioButton);
            if (gpayRadio != null) {
        gpayRadio.setOnClickListener(v -> {
                    selectedApp = "com.google.android.apps.nbu.paisa.user";
                    updatePayButton();
            updateCardSelection();
        });
            }

            RadioButton credRadio = findViewById(R.id.credRadioButton);
            if (credRadio != null) {
        credRadio.setOnClickListener(v -> {
                    selectedApp = "com.dreamplug.androidapp";
                    updatePayButton();
            updateCardSelection();
        });
            }

            RadioButton naviRadio = findViewById(R.id.naviRadioButton);
            if (naviRadio != null) {
        naviRadio.setOnClickListener(v -> {
                    selectedApp = "com.naviapp";
                    updatePayButton();
            updateCardSelection();
        });
            }

            RadioButton supermoneyRadio = findViewById(R.id.superRadioButton);
            if (supermoneyRadio != null) {
        supermoneyRadio.setOnClickListener(v -> {
                    selectedApp = "money.super.payments";
                    updatePayButton();
            updateCardSelection();
        });
            }

            RadioButton paytmRadio = findViewById(R.id.paytmRadioButton);
            if (paytmRadio != null) {
        paytmRadio.setOnClickListener(v -> {
                    selectedApp = "net.one97.paytm";
                    updatePayButton();
            updateCardSelection();
        });
            }

            RadioButton phonepeRadio = findViewById(R.id.phonepeRadioButton);
            if (phonepeRadio != null) {
        phonepeRadio.setOnClickListener(v -> {
                    selectedApp = "com.phonepe.app";
                    updatePayButton();
            updateCardSelection();
        });
            }

        } catch (Exception e) {
            Log.e("AmountActivity", "Error setting up listeners: " + e.getMessage());
        }
    }

    private void updateCardSelection() {
        int selectedColor = ContextCompat.getColor(this, R.color.green); // Green color for selected
        int unselectedColor = ContextCompat.getColor(this, R.color.card_unselected);
        
        // Set all cards to unselected first
        gpayCard.setCardBackgroundColor(unselectedColor);
        credpayCard.setCardBackgroundColor(unselectedColor);
        naviCard.setCardBackgroundColor(unselectedColor);
        supermoneyCard.setCardBackgroundColor(unselectedColor);
        paytmCard.setCardBackgroundColor(unselectedColor);
        phonepeCard.setCardBackgroundColor(unselectedColor);
        
        // Update radio buttons
        gpayRadio.setChecked(false);
        credRadio.setChecked(false);
        naviRadio.setChecked(false);
        supermoneyRadio.setChecked(false);
        paytmRadio.setChecked(false);
        phonepeRadio.setChecked(false);
        
        // Set the selected card based on the package name
        if (selectedApp.contains("google")) {
            gpayCard.setCardBackgroundColor(selectedColor);
            gpayRadio.setChecked(true);
        } else if (selectedApp.contains("dreamplug") || selectedApp.contains("cred")) {
            credpayCard.setCardBackgroundColor(selectedColor);
            credRadio.setChecked(true);
        } else if (selectedApp.contains("navi")) {
            naviCard.setCardBackgroundColor(selectedColor);
            naviRadio.setChecked(true);
        } else if (selectedApp.contains("super")) {
            supermoneyCard.setCardBackgroundColor(selectedColor);
            supermoneyRadio.setChecked(true);
        } else if (selectedApp.contains("paytm")) {
            paytmCard.setCardBackgroundColor(selectedColor);
            paytmRadio.setChecked(true);
        } else if (selectedApp.contains("phonepe")) {
            phonepeCard.setCardBackgroundColor(selectedColor);
            phonepeRadio.setChecked(true);
        }
    }

    private void handleIntentData() {
        try {
            // Get QR code data from intent
        String qrCodeData = getIntent().getStringExtra("decoded_data");
            Log.d("AmountActivity", "Received QR code data: " + qrCodeData);
        
        if (qrCodeData != null && !qrCodeData.isEmpty()) {
                // Check if it's a direct UPI ID
                if (qrCodeData.contains("@")) {
                    if (upiIdTextView != null) {
                        upiIdTextView.setText(qrCodeData);
                    }
                    if (payeeNameTextView != null) {
                        payeeNameTextView.setText("Payee");
                    }
                    return;
                }

                // Try to parse as UPI URI
                try {
                    Uri uri = Uri.parse(qrCodeData);
                    String upiId = uri.getQueryParameter("pa");
                    String payeeName = uri.getQueryParameter("pn");

                    if (upiId != null && !upiId.isEmpty()) {
                        if (upiIdTextView != null) {
                            upiIdTextView.setText(upiId);
                        }
                        if (payeeNameTextView != null) {
                            payeeNameTextView.setText(payeeName != null ? payeeName : "Payee");
            }
        } else {
                        // If no UPI ID in URI, try to get from extras
                        String upiIdExtra = getIntent().getStringExtra("upi_id");
                        if (upiIdExtra != null && !upiIdExtra.isEmpty()) {
                            if (upiIdTextView != null) {
                                upiIdTextView.setText(upiIdExtra);
                            }
                            if (payeeNameTextView != null) {
                                payeeNameTextView.setText(getIntent().getStringExtra("payee_name"));
                            }
                        } else {
                            throw new Exception("No valid UPI ID found");
                        }
                    }
                    } catch (Exception e) {
                    Log.e("AmountActivity", "Error parsing UPI URI: " + e.getMessage());
                    // Try to get data from extras as fallback
                    String upiIdExtra = getIntent().getStringExtra("upi_id");
                    if (upiIdExtra != null && !upiIdExtra.isEmpty()) {
                        if (upiIdTextView != null) {
                            upiIdTextView.setText(upiIdExtra);
                        }
                        if (payeeNameTextView != null) {
                            payeeNameTextView.setText(getIntent().getStringExtra("payee_name"));
                        }
                    } else {
                        throw new Exception("No valid UPI data found");
                    }
                }
            } else {
                // Try to get data from extras
                String upiId = getIntent().getStringExtra("upi_id");
                if (upiId != null && !upiId.isEmpty()) {
                    if (upiIdTextView != null) {
                        upiIdTextView.setText(upiId);
                    }
                    if (payeeNameTextView != null) {
                        payeeNameTextView.setText(getIntent().getStringExtra("payee_name"));
                    }
                } else {
                    throw new Exception("No payment data found");
                }
            }
        } catch (Exception e) {
            Log.e("AmountActivity", "Error handling intent data: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
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

        // Launch the app based on the selectedApp package name
        if (selectedApp != null && !selectedApp.isEmpty()) {
            final int amountInt = Integer.parseInt(amount);
            final String appName = getAppNameFromPackage(selectedApp);
            
            try {
                // Launch payment app without any handler delay
                launchPayment(selectedApp, amountInt);
                
                // Show dialog after a brief delay to allow app switching animation
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            showPaymentCompletionDialog(appName, amountInt);
                        }
                    }
                }, 1500); // Use 1.5 second delay for better UX
                
        } catch (Exception e) {
                Log.e("AmountActivity", "Error launching payment: " + e.getMessage(), e);
                Toast.makeText(AmountActivity.this, "Error launching payment app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(AmountActivity.this, "Please select a payment app", Toast.LENGTH_SHORT).show();
        }
    }

    // Show dialog after payment app launched to let user confirm completion
    private void showPaymentCompletionDialog(final String appName, final int amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Payment Completed?");
        builder.setMessage("Did you complete your payment of ₹" + amount + " using " + appName + "?");
        
        builder.setPositiveButton("Yes, Completed", new DialogInterface.OnClickListener() {
    @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get current user ID from SharedPreferences
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String userId = prefs.getString("userId", "user123");
                
                // Calculate cashback based on app used (if applicable)
                int cashbackAmount = 0;
                if (appName.equals("CRED") || appName.contains("cred")) {
                    // CRED gives ₹1 cashback
                    cashbackAmount = 1;
                } else if (appName.equals("Navi") || appName.equals("SuperMoney")) {
                    // Navi and SuperMoney give 5% with max of ₹50
                    cashbackAmount = Math.min((int)(amount * 0.05), 50);
                }
                
                // Get current timestamp in ISO format for consistent storage
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                
                try {
                    Log.d("AmountActivity", "Starting transaction recording - Amount: ₹" + amount + 
                            ", App: " + selectedApp + ", Timestamp: " + timestamp);
                    
                    // First update the transaction count in SharedPreferences
                    String key = selectedApp + "_" + today;
                    int currentCount = sharedPreferences.getInt(key, 0);
                    sharedPreferences.edit().putInt(key, currentCount + 1).apply();
                    
                    Log.d("AmountActivity", "Updated SharedPreferences - New count: " + (currentCount + 1));
                    
                    // Then record the transaction in the database
                    long transactionId = databaseHelper.addTransaction(
                            userId,             // user ID
                            amount,             // payment amount
                            selectedApp,        // app package name
                            cashbackAmount,     // calculated cashback
                            "success"           // transaction status (changed from "completed" to "success")
                    );
                    
                    if (transactionId > 0) {
                        Log.d("AmountActivity", "Transaction recorded successfully. ID: " + transactionId);
                        
                        // Launch TransactionResultActivity with success state
                        Intent resultIntent = new Intent(AmountActivity.this, TransactionResultActivity.class);
                        resultIntent.putExtra(TransactionResultActivity.EXTRA_SUCCESS, true);
                        resultIntent.putExtra(TransactionResultActivity.EXTRA_AMOUNT, String.valueOf(amount));
                        resultIntent.putExtra(TransactionResultActivity.EXTRA_APP_NAME, appName);
                        if (cashbackAmount > 0) {
                            resultIntent.putExtra(TransactionResultActivity.EXTRA_CASHBACK, String.valueOf(cashbackAmount));
                        }
                        startActivity(resultIntent);
                        finish();
                        
                    } else {
                        Log.e("AmountActivity", "Failed to record transaction");
                        // Launch TransactionResultActivity with failure state
                        Intent resultIntent = new Intent(AmountActivity.this, TransactionResultActivity.class);
                        resultIntent.putExtra(TransactionResultActivity.EXTRA_SUCCESS, false);
                        resultIntent.putExtra(TransactionResultActivity.EXTRA_MESSAGE, "Failed to record transaction details");
                        startActivity(resultIntent);
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("AmountActivity", "Error recording transaction: " + e.getMessage(), e);
                    // Launch TransactionResultActivity with failure state
                    Intent resultIntent = new Intent(AmountActivity.this, TransactionResultActivity.class);
                    resultIntent.putExtra(TransactionResultActivity.EXTRA_SUCCESS, false);
                    resultIntent.putExtra(TransactionResultActivity.EXTRA_MESSAGE, "Error: " + e.getMessage());
                    startActivity(resultIntent);
                    finish();
                }
            }
        });
        
        builder.setNegativeButton("No, Failed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get current user ID from SharedPreferences
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String userId = prefs.getString("userId", "user123");
                
                // Get current timestamp in ISO format for consistent storage
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                
                try {
                    Log.d("AmountActivity", "Recording failed transaction - Amount: ₹" + amount + 
                            ", App: " + selectedApp + ", Timestamp: " + timestamp);
                    
                    // Record the failed transaction in the database
                    long transactionId = databaseHelper.addTransaction(
                            userId,             // user ID
                            amount,             // payment amount
                            selectedApp,        // app package name
                            0,                  // no cashback for failed transactions
                            "failed"            // transaction status (keep as "failed")
                    );
                    
                    if (transactionId > 0) {
                        Log.d("AmountActivity", "Failed transaction recorded successfully. ID: " + transactionId);
                    } else {
                        Log.e("AmountActivity", "Failed to record failed transaction");
                    }
                } catch (Exception e) {
                    Log.e("AmountActivity", "Error recording failed transaction: " + e.getMessage(), e);
                }
                
                // Launch TransactionResultActivity with failure state
                Intent resultIntent = new Intent(AmountActivity.this, TransactionResultActivity.class);
                resultIntent.putExtra(TransactionResultActivity.EXTRA_SUCCESS, false);
                resultIntent.putExtra(TransactionResultActivity.EXTRA_MESSAGE, "Payment was not completed");
                startActivity(resultIntent);
                finish();
            }
        });
        
        builder.show();
    }

    private String getAppNameFromPackage(String packageName) {
        if (packageName.contains("google")) {
            return "Google Pay";
        } else if (packageName.contains("dreamplug") || packageName.contains("cred")) {
            return "CRED";
        } else if (packageName.contains("navi")) {
            return "Navi";
        } else if (packageName.contains("super")) {
            return "SuperMoney";
        } else if (packageName.contains("paytm")) {
            return "Paytm";
        } else if (packageName.contains("phonepe")) {
            return "PhonePe";
        }
        return "";
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
            if (qrCodeData == null || qrCodeData.isEmpty()) {
                Log.e("AmountActivity", "QR code data is null or empty");
                Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Check if the data is already a UPI ID
            if (qrCodeData.contains("@")) {
                if (upiIdTextView != null) {
                    upiIdTextView.setText(qrCodeData);
                }
                if (payeeNameTextView != null) {
                    payeeNameTextView.setText("Payee");
                }
                return;
            }

            // Try to parse as URI
            Uri uri = Uri.parse(qrCodeData);
            String upiId = uri.getQueryParameter("pa");
            String payeeName = uri.getQueryParameter("pn");

            // If no UPI ID in URI, try to get from intent extras
            if (upiId == null || upiId.isEmpty()) {
                Log.d("AmountActivity", "UPI ID not found in URI, checking intent extras");
                upiId = getIntent().getStringExtra("upi_id");
            }

            // If no payee name in URI, try to get from intent extras
            if (payeeName == null || payeeName.isEmpty()) {
                Log.d("AmountActivity", "Payee name not found in URI, checking intent extras");
                payeeName = getIntent().getStringExtra("payee_name");
            }

            // Set the extracted data to the TextViews with null checks
            if (upiIdTextView != null) {
                upiIdTextView.setText(upiId != null ? upiId : "UPI ID not found");
        } else {
                Log.e("AmountActivity", "upiIdTextView is null");
            }
            
            if (payeeNameTextView != null) {
                payeeNameTextView.setText(payeeName != null ? payeeName : "Payee name not found");
            } else {
                Log.e("AmountActivity", "payeeNameTextView is null");
            }
            
            Log.d("AmountActivity", "UPI data parsed successfully. UPI ID: " + upiId + ", Payee: " + payeeName);
        } catch (Exception e) {
            Log.e("AmountActivity", "Error parsing UPI data: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing UPI data: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void launchGPay(int amount) {
        launchPayment("com.google.android.apps.nbu.paisa.user", amount);
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
        String uri = "upi://pay?pa=" + upiIdTextView.getText().toString() +
                "&pn=" + payeeNameTextView.getText().toString() +
                "&am=" + amount +
                "&cu=INR";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri));
        intent.setPackage(packageName); // Set the package name
            
        // Check if the app is installed
        if (isAppInstalled(packageName)) {
            try {
                    startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Try alternative package if available
                String altPackage = getAlternativePackage(packageName);
                if (altPackage != null && !altPackage.equals(packageName)) {
                    try {
                        Log.d("PaymentApp", "Trying alternative package: " + altPackage);
                        Intent altIntent = new Intent(Intent.ACTION_VIEW);
                        altIntent.setData(Uri.parse(uri));
                        altIntent.setPackage(altPackage);
                        startActivity(altIntent);
                return;
                    } catch (ActivityNotFoundException ex) {
                        Log.e("PaymentApp", "Alternative app also failed to launch: " + ex.getMessage());
                    }
                }
                
                // If all fails, show the chooser
                Intent chooser = Intent.createChooser(intent, "Pay with");
                startActivity(chooser);
            }
            } else {
            String playStoreLink = getPlayStoreLink(packageName);
            showDownloadDialog("App Not Installed", playStoreLink);
        }
    }

    private String getAlternativePackage(String packageName) {
        switch (packageName) {
            case "com.naviapp":
                return "com.navi.android";
            case "money.super.payments":
                return "com.supermoney";
            case "com.dreamplug.androidapp":
                return "com.cred.club";
            case "com.navi.android":
                return "com.naviapp";
            case "com.supermoney":
                return "money.super.payments";
            case "com.cred.club":
                return "com.dreamplug.androidapp";
            default:
                return null;
        }
    }

    private boolean isAppInstalled(String packageName) {
        Log.d("AppCheck", "Checking for package: " + packageName);
        
        // First try the primary package
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("AppCheck", "Primary package not found: " + e.getMessage());
        }
        
        // If primary fails, try alternative
        String altPackage = getAlternativePackage(packageName);
        if (altPackage != null) {
            try {
                Log.d("AppCheck", "Trying alternative package: " + altPackage);
                getPackageManager().getPackageInfo(altPackage, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                Log.d("AppCheck", "Alternative package also not found: " + e.getMessage());
            }
        }
        
        return false;
    }

    private String getPlayStoreLink(String packageName) {
        switch (packageName) {
            case "com.dreamplug.androidapp":
            case "com.cred.club":
                return credPlayStoreLink;
            case "com.naviapp":
            case "com.navi.android":
                return naviPlayStoreLink;
            case "money.super.payments":
            case "com.supermoney":
                return supermoneyPlayStoreLink;
            case "net.one97.paytm":
                return paytmPlayStoreLink;
            case "com.phonepe.app":
                return phonepePlayStoreLink;
            default:
                return "https://play.google.com/store/apps";
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

    private void setupAppButtons() {
        try {
            // Get today's date in yyyy-MM-dd format
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Get transaction counts for TODAY ONLY
            int naviCount = databaseHelper.getTransactionCountForApp("com.naviapp", today);
            int credCount = databaseHelper.getTransactionCountForApp("com.dreamplug.androidapp", today);
            int gpayCount = databaseHelper.getTransactionCountForApp("com.google.android.apps.nbu.paisa.user", today);
            int superCount = databaseHelper.getTransactionCountForApp("money.super.payments", today);
            int paytmCount = databaseHelper.getTransactionCountForApp("net.one97.paytm", today);
            int phonepeCount = databaseHelper.getTransactionCountForApp("com.phonepe.app", today);
            
            // Get the parent LinearLayout inside the ScrollView
            HorizontalScrollView scrollView = findViewById(R.id.scrollView);
            if (scrollView == null) {
                Log.e("AmountActivity", "ScrollView not found");
                return;
            }
            
            // Get the parent view of the ScrollView
            ViewGroup parentView = (ViewGroup) scrollView.getParent();
            if (parentView != null) {
                // Create new layout params for the ScrollView
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) scrollView.getLayoutParams();
                if (params == null) {
                    params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                }
                // Convert dp to pixels
                int marginStart = (int) (20 * getResources().getDisplayMetrics().density);
                params.setMargins(marginStart, params.topMargin, params.rightMargin, params.bottomMargin);
                scrollView.setLayoutParams(params);
            }
            
            LinearLayout parentLayout = (LinearLayout) scrollView.getChildAt(0);
            if (parentLayout == null) {
                Log.e("AmountActivity", "Parent layout not found");
                return;
            }
            
            // Create a list of app cards with their transaction counts
            List<View> availableCards = new ArrayList<>();
            List<View> limitReachedCards = new ArrayList<>();

            // Check each app and add to appropriate list
            if (naviCount < 1) {
                availableCards.add(naviCard);
            } else {
                limitReachedCards.add(naviCard);
                updateCashbackText(naviCard, "Daily limit reached");
            }

            if (credCount < 1) {
                availableCards.add(credpayCard);
            } else {
                limitReachedCards.add(credpayCard);
                updateCashbackText(credpayCard, "Daily limit reached");
            }

            if (gpayCount < 1) {
                availableCards.add(gpayCard);
                    } else {
                limitReachedCards.add(gpayCard);
                updateCashbackText(gpayCard, "Daily limit reached");
                    }

            if (superCount < 1) {
                availableCards.add(supermoneyCard);
                } else {
                limitReachedCards.add(supermoneyCard);
                updateCashbackText(supermoneyCard, "Daily limit reached");
            }

            if (paytmCount < 1) {
                availableCards.add(paytmCard);
            } else {
                limitReachedCards.add(paytmCard);
                updateCashbackText(paytmCard, "Daily limit reached");
            }

            if (phonepeCount < 1) {
                availableCards.add(phonepeCard);
            } else {
                limitReachedCards.add(phonepeCard);
                updateCashbackText(phonepeCard, "Daily limit reached");
            }

            // Remove all views from the layout
            parentLayout.removeAllViews();

            // Add all available cards first
            for (View card : availableCards) {
                parentLayout.addView(card);
            }

            // Add all limit reached cards at the end
            for (View card : limitReachedCards) {
                parentLayout.addView(card);
            }

            // Reset all selection states
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = false;

            // If no app is currently selected or the selected app has reached its limit,
            // select the first available app
            if (selectedApp.isEmpty() || !availableCards.isEmpty()) {
                View firstCard = availableCards.get(0);
                if (firstCard == naviCard) {
                    selectedApp = "com.naviapp";
                    isNaviSelected = true;
                } else if (firstCard == credpayCard) {
                    selectedApp = "com.dreamplug.androidapp";
                    isCredSelected = true;
                } else if (firstCard == gpayCard) {
                    selectedApp = "com.google.android.apps.nbu.paisa.user";
                    isGpaySelected = true;
                } else if (firstCard == supermoneyCard) {
                    selectedApp = "money.super.payments";
                    isSuperSelected = true;
                } else if (firstCard == paytmCard) {
                    selectedApp = "net.one97.paytm";
                    isPaytmSelected = true;
                } else if (firstCard == phonepeCard) {
                    selectedApp = "com.phonepe.app";
                    isPhonepeSelected = true;
                }
            }

            // Update UI to reflect current selection
            updateCardSelection();
            updatePayButton();
            
        } catch (Exception e) {
            Log.e("AmountActivity", "Error setting up app buttons: " + e.getMessage());
        }
    }

    private void updateCashbackText(CardView card, String text) {
        TextView cashbackText = null;
        if (card.getId() == R.id.navicard) {
            cashbackText = card.findViewById(R.id.naviCashback);
        } else if (card.getId() == R.id.credcard) {
            cashbackText = card.findViewById(R.id.credCashback);
        } else if (card.getId() == R.id.gpaycard) {
            cashbackText = card.findViewById(R.id.gpayCashback);
        } else if (card.getId() == R.id.supermoneycard) {
            cashbackText = card.findViewById(R.id.superCashback);
        } else if (card.getId() == R.id.paytmcard) {
            cashbackText = card.findViewById(R.id.paytmCashback);
        } else if (card.getId() == R.id.phonepecard) {
            cashbackText = card.findViewById(R.id.phonepeCashback);
        }

        if (cashbackText != null) {
            cashbackText.setText(text);
            cashbackText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    // Helper class to store app configurations
    private static class AppConfig {
        String appName;
        String cashbackInfo;
        double maxCashback;
        int maxTransactions;

        AppConfig(String appName, String cashbackInfo, double maxCashback, int maxTransactions) {
            this.appName = appName;
            this.cashbackInfo = cashbackInfo;
            this.maxCashback = maxCashback;
            this.maxTransactions = maxTransactions;
        }

        AppConfig(String appName, String cashbackInfo, double maxCashback) {
            this.appName = appName;
            this.cashbackInfo = cashbackInfo;
            this.maxCashback = maxCashback;
        }
    }

    // Update transaction count and cashback after successful payment
    private void updateTransactionInfo(String appName, double cashbackAmount) {
        databaseHelper.incrementTransactionCount(appName);
        // Refresh the UI to show updated information
        setupAppButtons();
    }

    private void updateAppPriorities() {
        try {
            if (databaseHelper == null) {
                databaseHelper = new DatabaseHelper(this);
            }
            
            // Get today's date in yyyy-MM-dd format
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Get transaction counts for today
            int naviCount = databaseHelper.getTransactionCountForApp("com.naviapp", today);
            int credCount = databaseHelper.getTransactionCountForApp("com.dreamplug.androidapp", today);
            
            // Reset all selections
            isGpaySelected = false;
            isCredSelected = false;
            isNaviSelected = false;
            isSuperSelected = false;
            isPaytmSelected = false;
            isPhonepeSelected = false;

            // Set selection based on priority and transaction limits
            if (naviCount < 1) {
                // Navi is still available (less than 1 transaction)
                isNaviSelected = true;
            } else if (credCount < 1) {
                // Navi limit reached, but CRED is still available
                isCredSelected = true;
            } else {
                // Both Navi and CRED limits reached
                isSuperSelected = true;
            }

            // Update UI to reflect new selection
            updateCardSelection();

            // Update radio buttons
            gpayRadio.setChecked(isGpaySelected);
            credRadio.setChecked(isCredSelected);
            naviRadio.setChecked(isNaviSelected);
            supermoneyRadio.setChecked(isSuperSelected);
            paytmRadio.setChecked(isPaytmSelected);
            phonepeRadio.setChecked(isPhonepeSelected);

        } catch (Exception e) {
            Log.e("AmountActivity", "Error updating app priorities: " + e.getMessage());
            // Set default selection to prevent crash
            isGpaySelected = true;
            updateCardSelection();
            gpayRadio.setChecked(true);
        }
    }

    private void processPayment(String selectedApp) {
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get amount from EditText and remove commas
        String amountStr = amountEditText.getText().toString().replace(",", "");
        double amount = Double.parseDouble(amountStr);

        // Calculate cashback based on selected app
        double cashbackAmount = 0;
        switch (selectedApp) {
            case "GPay":
                cashbackAmount = 0; // No cashback
                break;
            case "Cred":
                cashbackAmount = 1.0; // ₹1 cashback
                break;
            case "Navi":
                cashbackAmount = 3.0; // ₹3 cashback
                break;
            case "Supermoney":
                cashbackAmount = 3.0; // ₹3 cashback
                break;
            case "Paytm":
                cashbackAmount = 0; // No cashback
                break;
            case "PhonePe":
                cashbackAmount = 0; // No cashback
                break;
        }

        // Record the transaction in database
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        long transactionId = dbHelper.addTransaction(
            userId,
            amount,
            selectedApp,
            cashbackAmount,
            "Completed"
        );

        if (transactionId != -1) {
            // Show success message with transaction details
            String message = String.format("Payment of ₹%.2f processed through %s", amount, selectedApp);
            if (cashbackAmount > 0) {
                message += String.format("\nCashback: ₹%.2f", cashbackAmount);
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            
            // Navigate back to home screen
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Failed to record transaction", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof RadioButton) {
            RadioButton selectedButton = (RadioButton) v;
            String buttonText = selectedButton.getText().toString();
            
            // Set the selected app based on the button text
            if (buttonText.contains("Google Pay")) {
                selectedApp = "com.google.android.apps.nbu.paisa.user";
            } else if (buttonText.contains("CRED")) {
                selectedApp = "com.dreamplug.androidapp";
            } else if (buttonText.contains("Navi")) {
                selectedApp = "com.naviapp";
            } else if (buttonText.contains("SuperMoney")) {
                selectedApp = "money.super.payments";
            } else if (buttonText.contains("Paytm")) {
                selectedApp = "net.one97.paytm";
            } else if (buttonText.contains("PhonePe")) {
                selectedApp = "com.phonepe.app";
            }
            
            // Update UI
            updatePayButton();
            updateCardSelection();
        }
    }

    private void updatePayButton() {
        // Get amount text with commas
        String amountStr = amountEditText.getText().toString();
        if (amountStr.isEmpty()) {
            payButton.setText("Pay ₹");
            payButton.setEnabled(false);
            return;
        }
        
        // Get app name based on package
        String appName = "App";
        if (selectedApp.contains("google")) {
            appName = "GPay";
        } else if (selectedApp.contains("dreamplug") || selectedApp.contains("cred")) {
            appName = "CRED";
        } else if (selectedApp.contains("navi")) {
            appName = "Navi";
        } else if (selectedApp.contains("super")) {
            appName = "SuperMoney";
        } else if (selectedApp.contains("paytm")) {
            appName = "Paytm";
        } else if (selectedApp.contains("phonepe")) {
            appName = "PhonePe";
        }
        
        // Update pay button text
        payButton.setText("Pay ₹" + amountStr + " with " + appName);
        payButton.setEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        Log.d("AmountActivity", "onNewIntent called - new QR scan detected");
        
        // Set the new intent to update QR code data
        setIntent(intent);
        
        // Re-handle intent data
        handleIntentData();
        
        // Clear amount field
        if (amountEditText != null) {
            amountEditText.setText("");
        }
        
        // IMPORTANT: Force refresh the database connection to ensure we get latest counts
        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = new DatabaseHelper(this);
        } else {
            databaseHelper = new DatabaseHelper(this);
        }
        
        // Get fresh transaction counts
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int naviCount = databaseHelper.getTransactionCountForApp("com.naviapp", today);
        int credCount = databaseHelper.getTransactionCountForApp("com.dreamplug.androidapp", today);
        
        Log.d("AmountActivity", "FRESH CHECK on new QR scan - Navi: " + naviCount + ", CRED: " + credCount);
        
        // Update the UI based on transaction counts
        setupAppButtons();
        
        // Update app positions based on transactions (this will set proper app selection)
        updateAppPositionsBasedOnTransactions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PAYMENT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Payment successful
                Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                
                // Get the selected app's package name
                String selectedAppPackage = getSelectedAppPackage();
                if (selectedAppPackage != null) {
                    // Increment transaction count for the selected app
                    incrementTransactionCount(selectedAppPackage);
                    
                    // Update the app list to reflect the new transaction count
                    setupAppButtons(); // This will reorder the apps
                }
            } else {
                // Payment failed
                Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void incrementTransactionCount(String packageName) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String key = packageName + "_" + today;
        int currentCount = sharedPreferences.getInt(key, 0);
        sharedPreferences.edit().putInt(key, currentCount + 1).apply();
    }

    private String getSelectedAppPackage() {
        if (selectedApp != null && !selectedApp.isEmpty()) {
            return selectedApp;
        }
        return null;
    }

    private void initializePaymentApps() {
        paymentApps = new ArrayList<>();
        paymentApps.add(new PaymentApp("Navi", "com.naviapp", R.drawable.navilogo));
        paymentApps.add(new PaymentApp("CRED", "com.dreamplug.androidapp", R.drawable.credlogo));
        paymentApps.add(new PaymentApp("Google Pay", "com.google.android.apps.nbu.paisa.user", R.drawable.gpaylogo));
        paymentApps.add(new PaymentApp("SuperMoney", "money.super.payments", R.drawable.supermoneylogo));
        paymentApps.add(new PaymentApp("Paytm", "net.one97.paytm", R.drawable.paytmlogo));
        paymentApps.add(new PaymentApp("PhonePe", "com.phonepe.app", R.drawable.phonepelogo));
    }

    private Map<String, Integer> getTodayTransactionCounts() {
        Map<String, Integer> counts = new HashMap<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        for (PaymentApp app : paymentApps) {
            String key = app.getPackageName() + "_" + today;
            counts.put(app.getPackageName(), sharedPreferences.getInt(key, 0));
        }
        
        return counts;
    }
}