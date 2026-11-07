plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kendamasenses.kenrec"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kendamasenses.kenrec"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Test builds can force a short restart time, in milliseconds (0 = use the setting):
        //   ./gradlew assembleDebug -PmaxRecordingMs=150000
        val maxRecordingMs = (project.findProperty("maxRecordingMs") as String?)?.toLongOrNull() ?: 0L
        buildConfigField("long", "MAX_RECORDING_MS_OVERRIDE", "${maxRecordingMs}L")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sideloaded builds: signed with the debug key until a release key exists.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.video)
    implementation(libs.camerax.view)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}
