package com.example.qrcodescannner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.text.NumberFormat;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "transactions.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_USERS = "users";
    private static final String TABLE_OTP = "otp";
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_IS_VERIFIED = "is_verified";
    private static final String COLUMN_OTP = "otp";
    private static final String COLUMN_OTP_TIMESTAMP = "otp_timestamp";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_APP_NAME = "app_name";
    private static final String COLUMN_CASHBACK = "cashback";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_TRANSACTION_DATE = "transaction_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT,"
                + COLUMN_EMAIL + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_IS_VERIFIED + " INTEGER DEFAULT 0,"
                + COLUMN_OTP + " TEXT,"
                + COLUMN_OTP_TIMESTAMP + " INTEGER)";
        db.execSQL(createTable);

        // Create transactions table
        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_APP_NAME + " TEXT,"
                + COLUMN_CASHBACK + " REAL,"
                + COLUMN_STATUS + " TEXT,"
                + COLUMN_TRANSACTION_DATE + " TEXT"
                + ")";
        db.execSQL(CREATE_TRANSACTIONS_TABLE);
    }

    // Add new methods for OTP handling
    public boolean saveOTP(String email, String otp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_OTP, otp);
        
        int rowsAffected = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return rowsAffected > 0;
    }

    public boolean verifyOTP(String email, String otp) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_OTP}, 
            COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String storedOTP = cursor.getString(cursor.getColumnIndex(COLUMN_OTP));
            cursor.close();
            
            if (otp.equals(storedOTP)) {
                // OTP verified, update user as verified
                ContentValues values = new ContentValues();
                values.put(COLUMN_IS_VERIFIED, 1);
                db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
                return true;
            }
        }
        return false;
    }

    public boolean isEmailVerified(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_IS_VERIFIED}, 
            COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            int isVerified = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VERIFIED));
            cursor.close();
            return isVerified == 1;
        }
        return false;
    }

    public String getOTP(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_OTP}, 
            COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String otp = cursor.getString(cursor.getColumnIndex(COLUMN_OTP));
            cursor.close();
            return otp;
        }
        return null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    public boolean addUser(String username, String email, String password, String otp) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        try {
            // First check if user exists with a transaction to prevent race conditions
            db.beginTransaction();
            
            // Check if email exists
            Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_EMAIL}, 
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
            
            boolean emailExists = cursor != null && cursor.moveToFirst();
            if (cursor != null) {
                cursor.close();
            }
            
            if (emailExists) {
                db.endTransaction();
                Log.d("DatabaseHelper", "Email already exists: " + email);
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_USERNAME, username);
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_PASSWORD, hashPassword(password));
            values.put(COLUMN_IS_VERIFIED, 0); // User starts as unverified
            values.put(COLUMN_OTP, otp); // Save the OTP

            long result = db.insert(TABLE_USERS, null, values);
            db.setTransactionSuccessful();
            db.endTransaction();
            
            return result != -1;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding user: " + e.getMessage());
            if (db.inTransaction()) {
                db.endTransaction();
            }
            return false;
        }
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?";
        String[] selectionArgs = {email, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_EMAIL};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        
        return exists;
    }

    public boolean checkPassword(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String hashedPassword = hashPassword(password);
        Log.d("DatabaseHelper", "Checking password for email: " + email);
        Log.d("DatabaseHelper", "Hashed input password: " + hashedPassword);
        
        String selection = COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?";
        String[] selectionArgs = {email, hashedPassword};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        
        Log.d("DatabaseHelper", "Password check result: " + exists);
        return exists;
    }

    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, hashPassword(newPassword));
        
        String whereClause = COLUMN_EMAIL + "=?";
        String[] whereArgs = {email};
        int result = db.update(TABLE_USERS, values, whereClause, whereArgs);
        
        Log.d("DatabaseHelper", "Password update result: " + (result > 0));
        return result > 0;
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            String hashedPassword = hexString.toString();
            Log.d("DatabaseHelper", "Password hashed successfully");
            return hashedPassword;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error hashing password: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Add methods for transaction tracking
    public void incrementTransactionCount(String appName) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Get today's date for the transaction
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Create transaction record with today's date
            ContentValues values = new ContentValues();
            values.put(COLUMN_APP_NAME, appName);
            values.put(COLUMN_TRANSACTION_DATE, today);
            values.put(COLUMN_STATUS, "completed");
            
            // Insert the transaction
            long id = db.insert(TABLE_TRANSACTIONS, null, values);
            
            // Log the transaction for debugging
            Log.d("DatabaseHelper", "Transaction recorded for " + appName + " on date " + today + " (id: " + id + ")");
            
            // Verify count after insert
            int count = getTransactionCountForApp(appName, today);
            Log.d("DatabaseHelper", "Current transaction count for " + appName + " on " + today + ": " + count);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error incrementing transaction count: " + e.getMessage(), e);
        }
    }

    public int getTransactionCount(String appName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {"transaction_count"};
        String selection = "app_name = ?";
        String[] selectionArgs = {appName};
        Cursor cursor = db.query(TABLE_TRANSACTIONS, columns, selection, selectionArgs, null, null, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public double getTotalCashback(String appName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {"total_cashback"};
        String selection = "app_name = ?";
        String[] selectionArgs = {appName};
        Cursor cursor = db.query(TABLE_TRANSACTIONS, columns, selection, selectionArgs, null, null, null);
        
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    // Get app priorities based on cashback rates
    public List<String> getAppPriorities() {
        List<String> priorities = new ArrayList<>();
        try {
            // Get today's date in yyyy-MM-dd format
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Get transaction counts for today
            int naviCount = getTransactionCountForApp("com.naviapp", today);
            int credCount = getTransactionCountForApp("com.dreamplug.androidapp", today);
            
            Log.d("DatabaseHelper", "App priorities - Navi count: " + naviCount + " (limit 1), CRED count: " + credCount + " (limit 3)");
            
            // First priority: Navi (if under limit)
            if (naviCount < 1) {
                priorities.add("com.naviapp");
            }
            
            // Second priority: CRED (if under limit)
            if (credCount < 3) {
                priorities.add("com.dreamplug.androidapp");
            }
            
            // Third priority: SuperMoney
            priorities.add("money.super.payments");
            
            // Fourth priority: Other apps (no cashback)
            priorities.add("com.google.android.apps.nbu.paisa.user");
            priorities.add("com.phonepe.app");
            priorities.add("net.one97.paytm");
            
            // Add Navi and CRED at the bottom if their limits are reached
            if (naviCount >= 1) {
                priorities.add("com.naviapp");
            }
            if (credCount >= 3) {
                priorities.add("com.dreamplug.androidapp");
            }
            
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting app priorities: " + e.getMessage(), e);
        }
        return priorities;
    }

    public int getTransactionCountForApp(String appName, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        try {
            // Query to get count of transactions for a specific app on a specific date
            String query = "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS + 
                         " WHERE " + COLUMN_APP_NAME + " = ? AND date(" + COLUMN_TRANSACTION_DATE + ") = ?";
            
            Log.d("DatabaseHelper", "Executing transaction count query for app: " + appName + " on date: " + date);
            Log.d("DatabaseHelper", "SQL Query: " + query + " with params [" + appName + ", " + date + "]");
            
            Cursor cursor = db.rawQuery(query, new String[]{appName, date});

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                Log.d("DatabaseHelper", "Found " + count + " transactions for " + appName + " on " + date);
                cursor.close();
            } else {
                Log.d("DatabaseHelper", "No transactions found for " + appName + " on " + date);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting transaction count: " + e.getMessage(), e);
        }

        return count;
    }

    public long addTransaction(String userId, double amount, String appName, double cashback, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            // Get current timestamp with time in ISO format
            SimpleDateFormat fullTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String timestamp = fullTimestampFormat.format(new Date());
            
            // Create values for the transaction
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, userId);
            values.put(COLUMN_AMOUNT, amount);  // Store the exact amount
            values.put(COLUMN_APP_NAME, appName);
            values.put(COLUMN_CASHBACK, cashback);
            values.put(COLUMN_STATUS, status);
            values.put(COLUMN_TRANSACTION_DATE, timestamp);
            
            // Log the transaction details before insertion
            Log.d("DatabaseHelper", "Adding transaction - Amount: " + amount + 
                    ", App: " + appName + ", Time: " + timestamp + 
                    ", Cashback: " + cashback + ", Status: " + status);

            // Insert the transaction and get the ID
            id = db.insert(TABLE_TRANSACTIONS, null, values);
            
            // Verify the transaction was inserted
            if (id > 0) {
                Log.d("DatabaseHelper", "Transaction inserted successfully with ID: " + id);
                
                // Run detailed verification query
                Cursor cursor = db.query(TABLE_TRANSACTIONS, 
                    new String[]{"*"}, // Get all columns 
                    COLUMN_ID + "=?", new String[]{String.valueOf(id)}, 
                    null, null, null);
                
                if (cursor != null && cursor.moveToFirst()) {
                    // Get and log all column values
                    double savedAmount = cursor.getDouble(cursor.getColumnIndex(COLUMN_AMOUNT));
                    String savedStatus = cursor.getString(cursor.getColumnIndex(COLUMN_STATUS));
                    String savedDate = cursor.getString(cursor.getColumnIndex(COLUMN_TRANSACTION_DATE));
                    
                    Log.d("DatabaseHelper", "VERIFICATION DETAILS: " +
                            "\n - ID: " + id +
                            "\n - Amount (original): " + amount + 
                            "\n - Amount (saved): " + savedAmount +
                            "\n - Status (original): " + status +
                            "\n - Status (saved): " + savedStatus +
                            "\n - Date: " + savedDate);
                    
                    cursor.close();
                }
                
                // Immediately verify that this transaction will be counted in getTotalAmountPaid
                double testTotal = getTotalAmountPaid();
                Log.d("DatabaseHelper", "Current total amount paid (after adding transaction): " + testTotal);
            } else {
                Log.e("DatabaseHelper", "Failed to insert transaction");
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding transaction: " + e.getMessage(), e);
        }

        return id;
    }

    // Add this method to your DatabaseHelper class
    public List<Map<String, String>> getTransactions() {
        List<Map<String, String>> transactionsList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try {
            // Query to get transactions with exact timestamps
            String query = "SELECT id, user_id, amount, app_name, transaction_date, status, cashback FROM transactions ORDER BY transaction_date DESC";
            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    Map<String, String> transaction = new HashMap<>();
                    
                    // Get raw data from cursor
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    int amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount"));
                    String appName = cursor.getString(cursor.getColumnIndexOrThrow("app_name"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("transaction_date"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    int cashback = cursor.getInt(cursor.getColumnIndexOrThrow("cashback"));
                    
                    // Log raw data for debugging
                    Log.d("DatabaseHelper", "Raw transaction data - ID: " + id + 
                           ", Amount: " + amount + 
                           ", App: " + appName + 
                           ", Date: " + date + 
                           ", Status: " + status + 
                           ", Cashback: " + cashback);
                    
                    // Format the amount with rupee symbol
                    String formattedAmount = "₹" + amount;
                    
                    // Format the date for better readability
                    String formattedDate = date;
                    try {
                        // Assuming the date is stored in ISO 8601 format
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                        Date transactionDate = inputFormat.parse(date);
                        if (transactionDate != null) {
                            formattedDate = outputFormat.format(transactionDate);
                        }
                    } catch (Exception e) {
                        Log.e("DatabaseHelper", "Error formatting date: " + e.getMessage());
                        // Keep the original date string if parsing fails
                    }
                    
                    // Convert package name to readable app name
                    String readableAppName = getReadableAppName(appName);
                    
                    // Log formatted data
                    Log.d("DatabaseHelper", "Formatted transaction - Amount: " + formattedAmount + 
                           ", App: " + readableAppName + 
                           ", Date: " + formattedDate);
                    
                    // Add all data to the transaction map
                    transaction.put("id", String.valueOf(id));
                    transaction.put("amount", formattedAmount);
                    transaction.put("app_name", readableAppName);
                    transaction.put("date", formattedDate);
                    transaction.put("status", status);
                    transaction.put("cashback", "₹" + cashback);
                    
                    transactionsList.add(transaction);
                } while (cursor.moveToNext());
            } else {
                Log.d("DatabaseHelper", "No transactions found in database");
            }
            
            cursor.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting transactions: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        
        Log.d("DatabaseHelper", "Retrieved " + transactionsList.size() + " transactions");
        return transactionsList;
    }

    // Helper method to get readable app name from package
    private String getReadableAppName(String appPackage) {
        if (appPackage == null) return "Unknown App";
        
        switch (appPackage) {
            case "com.google.android.apps.nbu.paisa.user":
            case "GooglePay":
                return "Google Pay";
            case "com.phonepe.app":
            case "PhonePe":
                return "PhonePe";
            case "net.one97.paytm":
            case "Paytm":
                return "Paytm";
            case "com.dreamplug.androidapp":
            case "CRED":
                return "CRED";
            case "com.naviapp":
            case "Navi":
                return "Navi";
            case "money.super.payments":
            case "SuperMoney":
                return "SuperMoney";
            default:
                return appPackage;
        }
    }
    
    // Update app priority based on usage frequency
    public boolean updateAppPriority(String appName) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        
        try {
            // Get today's date in yyyy-MM-dd format
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Log the priority update
            Log.d("DatabaseHelper", "Updating app priority for " + appName + " on " + today);
            
            // In this implementation, we use transaction counts to determine app priority
            // The getAppPriorities() method will return apps ordered by their usage frequency
            // No additional action needed here since transactions are already recorded
            
            success = true;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating app priority: " + e.getMessage(), e);
        }
        
        return success;
    }

    public boolean verifyUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_PASSWORD, COLUMN_IS_VERIFIED};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        
        if (cursor.moveToFirst()) {
            String savedPassword = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
            int isVerified = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VERIFIED));
            cursor.close();
            
            // Only compare hashed passwords
            String hashedInputPassword = hashPassword(password);
            if (hashedInputPassword == null || !hashedInputPassword.equals(savedPassword)) {
                return false;
            }
            
            // If password matches but user is not verified, return false
            if (isVerified != 1) {
                return false;
            }
            
            return true;
        }
        return false;
    }

    public boolean registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Check if email already exists
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_EMAIL}, 
            COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        
        if (cursor.getCount() > 0) {
            cursor.close();
            return false; // Email already exists
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, hashPassword(password));
        values.put(COLUMN_IS_VERIFIED, 0); // User starts unverified
        
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public String getUsername(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USERNAME};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        
        Log.d("DatabaseHelper", "Getting username for email: " + email);
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
            Log.d("DatabaseHelper", "Found username: " + username);
            cursor.close();
            return username;
        }
        
        Log.d("DatabaseHelper", "No username found for email: " + email);
        return "";
    }

    public boolean updateUserProfile(String oldEmail, String newEmail, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, newEmail);
        values.put(COLUMN_USERNAME, newUsername);
        
        Log.d("DatabaseHelper", "Updating user profile - Old Email: '" + oldEmail + "', New Email: '" + newEmail + "', New Username: '" + newUsername + "'");
        
        int rowsAffected = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{oldEmail});
        boolean success = rowsAffected > 0;
        
        Log.d("DatabaseHelper", "Profile update " + (success ? "successful" : "failed") + " - Rows affected: " + rowsAffected);
        
        return success;
    }

    public boolean verifyUserProfile(String email, String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USERNAME};
        String selection = COLUMN_EMAIL + "=? AND " + COLUMN_USERNAME + "=?";
        String[] selectionArgs = {email, username};
        
        Log.d("DatabaseHelper", "Verifying user profile - Email: '" + email + "', Username: '" + username + "'");
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        
        if (cursor != null) {
            cursor.close();
        }
        
        Log.d("DatabaseHelper", "Profile verification " + (exists ? "successful" : "failed"));
        return exists;
    }

    public int getTotalTransactionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM transactions", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public double getTotalCashback() {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalCashback = 0.0;
        
        try {
            // Use the correct column name 'cashback' instead of 'cashback_amount'
            Cursor cursor = db.rawQuery("SELECT SUM(cashback) FROM transactions WHERE status = 'success'", null);
            
            if (cursor != null && cursor.moveToFirst()) {
                totalCashback = cursor.getDouble(0);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting total cashback: " + e.getMessage());
        }
        
        return totalCashback;
    }

    public double getTotalAmountPaid() {
        double totalAmountPaid = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        
        try {
            // Query to get the sum of all successful transactions (check for both "success" and "completed" statuses)
            String query = "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + 
                          " WHERE " + COLUMN_STATUS + " = 'success' OR " + COLUMN_STATUS + " = 'completed'";
            Log.d("DatabaseHelper", "Executing query: " + query);
            
            Cursor cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                totalAmountPaid = cursor.getDouble(0);
                Log.d("DatabaseHelper", "Total Amount Paid from DB: " + totalAmountPaid);
                cursor.close();
            } else {
                Log.d("DatabaseHelper", "No successful transactions found or sum returned null.");
                if (cursor != null) {
                    cursor.close();
                }
            }
            
            // Verify with a count query
            cursor = db.rawQuery("SELECT COUNT(*), SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + 
                                " WHERE " + COLUMN_STATUS + " = 'success' OR " + COLUMN_STATUS + " = 'completed'", null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                double sum = cursor.isNull(1) ? 0.0 : cursor.getDouble(1);
                Log.d("DatabaseHelper", "Verification - Successful transaction count: " + count + ", Sum: " + sum);
                cursor.close();
            }
            
            // For debugging, also log all transactions regardless of status
            cursor = db.rawQuery("SELECT " + COLUMN_AMOUNT + ", " + COLUMN_STATUS + " FROM " + TABLE_TRANSACTIONS, null);
            if (cursor != null && cursor.getCount() > 0) {
                Log.d("DatabaseHelper", "--- All transactions in database ---");
                while (cursor.moveToNext()) {
                    double amount = cursor.getDouble(cursor.getColumnIndex(COLUMN_AMOUNT));
                    String status = cursor.getString(cursor.getColumnIndex(COLUMN_STATUS));
                    Log.d("DatabaseHelper", "Transaction: amount=" + amount + ", status=" + status);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting total amount paid: " + e.getMessage(), e);
        }
        
        Log.d("DatabaseHelper", "Final total amount paid value: " + totalAmountPaid);
        return totalAmountPaid;
    }
}