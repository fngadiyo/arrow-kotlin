plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fngadiyo.arrow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fngadiyo.arrow"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["ADMOB_APP_ID"] = (project.findProperty("ADMOB_APP_ID") as String?) ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
