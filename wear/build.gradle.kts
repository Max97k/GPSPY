plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.gpsspy.gpstracker.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gpsspy.gpstracker" // Must match mobile app exactly for paired communication
        minSdk = 30 // Wear OS 3+ for Compose
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    
    // Compose for Wear OS
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha17")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // Wear Core / Ambient Mode
    implementation("androidx.wear:wear:1.3.0")
}
