package com.example.qrcodescannner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.qrcodescannner.R;

public class UpiAppSelection extends AppCompatActivity {

    private CardView selectedCard; // To keep track of the selected CardView
    private String payeeName;
    private String upiId;
    private String amount;
    private String gpayPlayStoreLink = "https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.paisa.user"; // Google Pay Play Store link
    private String credPackageName = "com.dreamplug.androidapp"; // CRED package name
    private String credPlayStoreLink = "https://play.google.com/store/apps/details?id=com.dreamplug.androidapp"; // CRED Play Store link
    private String naviPackageName = "com.naviapp"; // Navi package name
    private String naviPlayStoreLink = "https://play.google.com/store/apps/details?id=com.navi.android"; // Navi Play Store link
    private String supermoneyPackageName = "money.super.payments"; // SuperMoney package name
    private String supermoneyPlayStoreLink = "https://play.google.com/store/apps/details?id=com.supermoney.android"; // SuperMoney Play Store link

    private static final String TAG = "UpiAppSelection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upi_app_selection); // Ensure this matches your XML layout file name

        // Retrieve data from the intent
        payeeName = getIntent().getStringExtra("payee_name");
        upiId = getIntent().getStringExtra("upi_id");
        amount = getIntent().getStringExtra("amount");

        CardView gpayCard = findViewById(R.id.gpaycard);
        CardView credCard = findViewById(R.id.credcard);
        CardView naviCard = findViewById(R.id.navicard); // Navi Card
        CardView supermoneyCard = findViewById(R.id.supermoneycard); // SuperMoney Card
        Button backButton = findViewById(R.id.back);

        // Set click listeners for the CardViews
        gpayCard.setOnClickListener(v -> selectCard(gpayCard));
        credCard.setOnClickListener(v -> selectCard(credCard));
        naviCard.setOnClickListener(v -> selectCard(naviCard)); // Navi Card selection
        supermoneyCard.setOnClickListener(v -> selectCard(supermoneyCard)); // SuperMoney Card selection

        // Set click listener for the back button
        backButton.setOnClickListener(v -> finish()); // Close the activity
    }

    private void selectCard(CardView card) {
        // Deselect previously selected card
        if (selectedCard != null) {
            selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white)); // Reset background color
        }

        // Select the new card
        selectedCard = card;
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray)); // Change background color to indicate selection

        // Redirect to UPI payment page
        redirectToUpiPayment();
    }

    private void redirectToUpiPayment() {
        String uri = "upi://pay?pa=" + upiId + "&pn=" + payeeName + "&mc=1234&tid=1234567890&tt=Test&am=" + amount + "&cu=INR&url=https://example.com";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));

        if (selectedCard == findViewById(R.id.credcard)) {
            intent.setPackage(credPackageName); // Set CRED package name
            if (isAppInstalled(credPackageName)) {
                startActivity(intent);
            } else {
                showDownloadDialog("CRED Not Installed", credPlayStoreLink);
            }
        } else if (selectedCard == findViewById(R.id.navicard)) {
            if (isAppInstalled(naviPackageName)) {
                intent.setPackage(naviPackageName); // Set Navi package name
                startActivity(intent);
                Toast.makeText(this, "Press back to return to the UPI-Payment page.", Toast.LENGTH_LONG).show();
            } else {
                showDownloadDialog("Navi Not Installed", naviPlayStoreLink);
            }
        } else if (selectedCard == findViewById(R.id.supermoneycard)) {
            intent.setPackage(supermoneyPackageName); // Set SuperMoney package name
            if (isAppInstalled(supermoneyPackageName)) {
                startActivity(intent);
            } else {
                showDownloadDialog("SuperMoney Not Installed", supermoneyPlayStoreLink);
            }
        } else {
            // Handle Google Pay logic
            String gpayPackageName = getGPayPackageName();
            if (gpayPackageName != null) {
                intent.setPackage(gpayPackageName); // Set Google Pay package name
                startActivity(intent);
            } else {
                showDownloadDialog("Google Pay Not Installed", gpayPlayStoreLink);
            }
        }
    }

    private String getGPayPackageName() {
        // Check for both possible package names of Google Pay
        if (isAppInstalled("com.google.android.apps.nbu.paisa.user")) {
            return "com.google.android.apps.nbu.paisa.user"; // India-specific Google Pay
        } else if (isAppInstalled("com.google.android.apps.walletnfcrel")) {
            return "com.google.android.apps.walletnfcrel"; // International Google Pay
        }
        return null; // Google Pay is not installed
    }

    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            Log.d(TAG, "App is installed: " + packageName);
            return true; // App is installed
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "App is NOT installed: " + packageName);
            return false; // App is not installed
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
