plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.qrcodescanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.qrcodescanner"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core AndroidX libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Barcode scanning library
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Additional libraries
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0") // CardView dependency
    implementation("androidx.camera:camera-core:1.0.0")
    implementation("androidx.camera:camera-camera2:1.2.0") // Updated version
    implementation("androidx.camera:camera-lifecycle:1.2.0") // Updated version
    implementation("androidx.camera:camera-view:1.2.0") // Updated version

    // QR code scanner library
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.0")

    // Example custom library
    implementation(libs.engage.core) // Ensure this alias is correctly set in your libs.versions.toml

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
