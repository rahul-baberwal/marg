import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rahulbaberwal.marg"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.1.0"
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
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile != null && resolvedKeyFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    applicationVariants.all {
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val type = if (buildType.name == "release") "" else "-debug"
            output?.outputFileName = "Marg-v${versionName}$type.apk"
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
    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Custom Tabs
    implementation("androidx.browser:browser:1.8.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // MapLibre
    implementation("org.maplibre.gl:android-sdk:11.5.2")
}
