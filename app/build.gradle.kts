import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = when {
    rootProject.file("keystore.properties").exists() -> rootProject.file("keystore.properties")
    project.file("keystore.properties").exists() -> project.file("keystore.properties")
    else -> null
}

val keystoreProperties = Properties().apply {
    keystorePropertiesFile?.let {
        load(FileInputStream(it))
    }
}

val rawStoreFile = keystoreProperties.getProperty("storeFile")
val resolvedKeyFile = when {
    rawStoreFile == null -> null
    file(rawStoreFile).exists() -> file(rawStoreFile)
    rootProject.file(rawStoreFile).exists() -> rootProject.file(rawStoreFile)
    keystorePropertiesFile?.parentFile?.resolve(rawStoreFile)?.exists() == true -> keystorePropertiesFile.parentFile.resolve(rawStoreFile)
    rootProject.file("my-upload-key.jks").exists() -> rootProject.file("my-upload-key.jks")
    else -> file(rawStoreFile)
}

android {
    namespace = "com.rahulbaberwal.marg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rahulbaberwal.marg"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile != null && resolvedKeyFile != null) {
                storeFile = resolvedKeyFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropertiesFile != null && resolvedKeyFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.maplibre.gl:android-sdk:11.5.2")
}
