package com.example.qrcodescannner;

import android.app.Application;
import android.content.Context;

public class QRCodeScannerApp extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }
} 