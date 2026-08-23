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
    // NOTE: the original com.arthenica ffmpeg-kit project was archived in 2025.
    // If this coordinate 404s in Maven Central, swap it for the community-maintained
    // fork published under "io.github.ffmpeg-kit" (same API surface).
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")
}
