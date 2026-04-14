plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val enableNativeSymbols = providers.gradleProperty("enableNativeSymbols")
    .map(String::toBoolean)
    .orElse(false)

android {
    namespace = "com.fngadiyo.arrow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fngadiyo.arrow"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.0.2"

        manifestPlaceholders["ADMOB_APP_ID"] = (project.findProperty("ADMOB_APP_ID") as String?) ?: ""
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            storePassword = "arrow-entangled-2026"
            keyAlias = "arrow-key"
            keyPassword = "arrow-entangled-2026"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (enableNativeSymbols.get()) {
                ndk {
                    debugSymbolLevel = "SYMBOL_TABLE"
                }
            }
        }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Google Mobile Ads SDK (Placeholder version)
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    
    // Google Play Games Services v2 (Placeholder version)
    implementation("com.google.android.gms:play-services-games-v2:20.1.2")

    // Google Play Billing Library
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    testImplementation("junit:junit:4.13.2")
}
