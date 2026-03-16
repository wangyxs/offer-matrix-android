import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localSecret(name: String): String {
    val localValue = localProperties.getProperty(name)
    val envValue = System.getenv(name)
    return (localValue ?: envValue ?: "").trim()
}

fun asBuildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.example.offermatrix"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.offermatrix"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            // Force ARM architectures as the speech engine SDK does not support x86/x86_64
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        
        manifestPlaceholders["GALAXY_DATA_VALUE"] = "1"
        manifestPlaceholders["GALAXY_SF_VALUE"] = "1"
        buildConfigField("String", "SPEECH_DIALOG_APP_ID", asBuildConfigString(localSecret("speechDialogAppId")))
        buildConfigField("String", "SPEECH_DIALOG_APP_KEY", asBuildConfigString(localSecret("speechDialogAppKey")))
        buildConfigField("String", "SPEECH_DIALOG_ACCESS_TOKEN", asBuildConfigString(localSecret("speechDialogAccessToken")))
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packagingOptions {
        pickFirst("lib/arm64-v8a/libc++_shared.so")
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.view)

    // Net
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    // ByteDance Speech Dialog SDK
    implementation("com.bytedance.speechengine:speechengine_tob:0.0.14.1-bugfix")
    
    // UI dependencies for InterviewCallActivity
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
