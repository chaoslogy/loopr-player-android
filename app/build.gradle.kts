plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "co.loopr.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "co.loopr.player"
        minSdk = 22                       // Fire TV Stick 2nd gen
        targetSdk = 35
        versionCode = 16
        versionName = "0.1.15"

        // Read at runtime via BuildConfig
        buildConfigField("String", "API_BASE_URL", "\"https://api-staging.loopr.studio\"")
        buildConfigField("String", "PAIR_URL",     "\"app-staging.loopr.studio\"")
    }

    // Stable signing for the staging channel. CI runners generate a fresh
    // ~/.android/debug.keystore on every run, so consecutive staging builds were
    // signed with DIFFERENT keys -> Fire TV refused in-place updates ("App not
    // installed", Aparna QA 16 Jul). This checked-in keystore (staging only —
    // protects nothing beyond update continuity) makes every build's signature
    // identical so updates install over the old version and keep app data.
    // A real release key (not in git) is still needed for any store/prod build.
    signingConfigs {
        create("staging") {
            storeFile = file("staging.keystore")
            storePassword = "android"
            keyAlias = "looprstaging"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("staging")
        }
    }

    flavorDimensions += "store"
    productFlavors {
        create("amazon") {
            dimension = "store"
            // What ships to the Amazon Appstore (Fire TV)
        }
        create("play") {
            dimension = "store"
            // What ships to the Google Play Store (Android TV / Android tablets)
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)

    implementation(libs.coil.compose)
}
