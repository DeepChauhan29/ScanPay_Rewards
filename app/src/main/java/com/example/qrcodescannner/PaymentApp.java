package com.example.qrcodescannner;

public class PaymentApp {
    private String name;
    private String packageName;
    private int iconResId;

    public PaymentApp(String name, String packageName, int iconResId) {
        this.name = name;
        this.packageName = packageName;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getIconResId() {
        return iconResId;
    }
} 