plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kanata.mod.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kanata.mod.example"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // compileOnly — mod-api is already in the host app's classloader at runtime.
    // Do NOT change this to `implementation`, it would bloat the APK with duplicate classes.
    compileOnly(project(":mod-api"))
}
