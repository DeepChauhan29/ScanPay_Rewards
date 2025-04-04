package com.example.qrcodescannner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TransactionResultActivity extends AppCompatActivity {

    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_APP_NAME = "app_name";
    public static final String EXTRA_CASHBACK = "cashback";

    private ImageView resultIcon;
    private TextView resultTitle;
    private TextView resultMessage;
    private TextView amountValue;
    private TextView appValue;
    private TextView cashbackValue;
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_result);

        // Initialize views
        resultIcon = findViewById(R.id.resultIcon);
        resultTitle = findViewById(R.id.resultTitle);
        resultMessage = findViewById(R.id.resultMessage);
        amountValue = findViewById(R.id.amountValue);
        appValue = findViewById(R.id.appValue);
        cashbackValue = findViewById(R.id.cashbackValue);
        actionButton = findViewById(R.id.actionButton);

        // Get intent extras
        boolean isSuccess = getIntent().getBooleanExtra(EXTRA_SUCCESS, false);
        String message = getIntent().getStringExtra(EXTRA_MESSAGE);
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String appName = getIntent().getStringExtra(EXTRA_APP_NAME);
        String cashback = getIntent().getStringExtra(EXTRA_CASHBACK);

        // Set up UI based on transaction result
        if (isSuccess) {
            setupSuccessUI(amount, appName, cashback);
        } else {
            setupFailureUI(message);
        }

        // Set up action button
        actionButton.setOnClickListener(v -> {
            // Return to home screen
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupSuccessUI(String amount, String appName, String cashback) {
        resultIcon.setImageResource(R.drawable.ic_success);
        resultTitle.setText("Payment Successful!");
        resultTitle.setTextColor(getResources().getColor(R.color.green));
        resultMessage.setText("Your payment has been processed successfully.");
        
        // Set transaction details
        amountValue.setText("₹" + amount);
        appValue.setText(appName);
        
        if (cashback != null && !cashback.isEmpty()) {
            cashbackValue.setText("₹" + cashback);
            cashbackValue.setVisibility(View.VISIBLE);
        } else {
            cashbackValue.setVisibility(View.GONE);
        }
        
        actionButton.setText("Scan Another QR");
    }

    private void setupFailureUI(String message) {
        resultIcon.setImageResource(R.drawable.ic_failure);
        resultTitle.setText("Payment Failed");
        resultTitle.setTextColor(getResources().getColor(R.color.red));
        resultMessage.setText(message != null ? message : "Unable to process payment. Please try again.");
        
        // Hide transaction details for failure
        findViewById(R.id.transactionDetails).setVisibility(View.GONE);
        
        actionButton.setText("Try Again");
    }
} 