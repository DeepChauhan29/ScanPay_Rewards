package com.example.qrcodescannner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "UserDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_IS_VERIFIED = "is_verified";
    private static final String COLUMN_OTP = "otp";
    private static final String COLUMN_OTP_TIMESTAMP = "otp_timestamp";

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
    }

    // Add new methods for OTP handling
    public boolean saveOTP(String email, String otp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_OTP, otp);
        values.put(COLUMN_OTP_TIMESTAMP, System.currentTimeMillis());
        
        String whereClause = COLUMN_EMAIL + "=?";
        String[] whereArgs = {email};
        int result = db.update(TABLE_USERS, values, whereClause, whereArgs);
        return result > 0;
    }

    public boolean verifyOTP(String email, String otp) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_OTP, COLUMN_OTP_TIMESTAMP};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        
        if (cursor.moveToFirst()) {
            String savedOTP = cursor.getString(cursor.getColumnIndex(COLUMN_OTP));
            long timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_OTP_TIMESTAMP));
            cursor.close();
            
            // Check if OTP is not expired (valid for 5 minutes)
            long currentTime = System.currentTimeMillis();
            boolean isValid = savedOTP != null && 
                            savedOTP.equals(otp) && 
                            (currentTime - timestamp) <= 300000; // 5 minutes in milliseconds
            
            if (isValid) {
                // Mark user as verified
                ContentValues values = new ContentValues();
                values.put(COLUMN_IS_VERIFIED, 1);
                values.put(COLUMN_OTP, ""); // Clear OTP after verification
                db.update(TABLE_USERS, values, selection, selectionArgs);
            }
            
            return isValid;
        }
        return false;
    }

    public boolean isEmailVerified(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_IS_VERIFIED};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        
        if (cursor.moveToFirst()) {
            int isVerified = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VERIFIED));
            cursor.close();
            return isVerified == 1;
        }
        return false;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean addUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
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
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }
}