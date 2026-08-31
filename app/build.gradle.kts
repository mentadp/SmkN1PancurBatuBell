plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.sch.smkn1pancurbatu.bell"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.sch.smkn1pancurbatu.bell"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.1"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Library Web Server Lokal di Android
    implementation("org.nanohttpd:nanohttpd:2.3.1")
}