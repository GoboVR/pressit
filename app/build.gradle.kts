plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pressit.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pressit.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Local, on-device audio/video transcoding (no server calls).
    // The original com.arthenica ffmpeg-kit-full artifact was pulled from Maven
    // Central in April 2025 when the project was retired. This is a community
    // republish of the same library (same com.arthenica.ffmpegkit Java package,
    // just a new group/artifact id) — no source changes needed.
    implementation("com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1")
}
