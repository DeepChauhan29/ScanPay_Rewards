package com.example.qrcodescannner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private ListView transactionsListView;
    private TextView emptyView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        
        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);
        
        // Initialize views
        transactionsListView = findViewById(R.id.transactions_list);
        emptyView = findViewById(R.id.empty_view);
        transactionsListView.setEmptyView(emptyView);
        
        // Set up bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_transactions);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_transactions) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
                
        // Load transactions
        loadTransactions();
    }
    
    private void loadTransactions() {
        try {
            // Get transactions from database
            List<Map<String, String>> transactionsList = databaseHelper.getTransactions();
            
            if (transactionsList.isEmpty()) {
                Log.w("TransactionsActivity", "No transactions found in database");
                emptyView.setText("No transactions yet! Make your first payment.");
                emptyView.setVisibility(View.VISIBLE);
                return;
            }
            
            // Get total transaction count
            Log.d("TransactionsActivity", "Found " + transactionsList.size() + " transactions");
            
            // Verify we have the expected data fields for each transaction
            boolean hasValidData = true;
            for (Map<String, String> transaction : transactionsList) {
                String app = transaction.get("app_name");
                String amount = transaction.get("amount");
                String date = transaction.get("date");
                String status = transaction.get("status");
                
                if (app == null || amount == null || date == null || status == null) {
                    hasValidData = false;
                    Log.e("TransactionsActivity", "Missing data in transaction: " + 
                           "App=" + app + ", Amount=" + amount + 
                           ", Date=" + date + ", Status=" + status);
                } else {
                    Log.d("TransactionsActivity", "Transaction data - App: " + app + 
                           ", Amount: " + amount + ", Date: " + date + ", Status: " + status);
                }
            }
            
            // Create adapter with data validation
            if (!hasValidData) {
                Log.w("TransactionsActivity", "Some transactions have missing data fields");
            }
            
            // Create the adapter with custom view handling
            SimpleAdapter adapter = new SimpleAdapter(
                this,
                transactionsList,
                R.layout.item_transaction,
                new String[]{"app_name", "amount", "date", "status"},
                new int[]{R.id.transaction_app, R.id.transaction_amount, R.id.transaction_date, R.id.transaction_status}
            ) {
                @Override
                public void setViewText(TextView v, String text) {
                    // Apply special formatting for different view types
                    int id = v.getId();
                    
                    if (id == R.id.transaction_amount) {
                        // Format and style the amount
                        Log.d("TransactionsActivity", "Setting amount: " + text);
                        if (text == null || text.isEmpty()) {
                            text = "₹0";
                            Log.w("TransactionsActivity", "Empty amount value, using default");
                        }
                        v.setTextColor(getResources().getColor(R.color.colorPrimary));
                        v.setTextSize(20);
                        v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
                    }
                    else if (id == R.id.transaction_date) {
                        // Format and style the date
                        Log.d("TransactionsActivity", "Setting date: " + text);
                        if (text == null || text.isEmpty()) {
                            text = "Unknown date";
                            Log.w("TransactionsActivity", "Empty date value, using default");
                        }
                        v.setTypeface(v.getTypeface(), android.graphics.Typeface.ITALIC);
                    }
                    
                    // Set the text after formatting
                    super.setViewText(v, text);
                }
            };
            
            // Set adapter and show list
            transactionsListView.setAdapter(adapter);
            transactionsListView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            
        } catch (Exception e) {
            Log.e("TransactionsActivity", "Error loading transactions: " + e.getMessage(), e);
            emptyView.setText("Error loading transactions: " + e.getMessage());
            emptyView.setVisibility(View.VISIBLE);
        }
    }
} 