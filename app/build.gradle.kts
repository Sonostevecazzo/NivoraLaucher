plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingStorePath = System.getenv("NIVORA_KEYSTORE_PATH")
val signingStorePassword = System.getenv("NIVORA_STORE_PASSWORD")
val signingKeyAlias = System.getenv("NIVORA_KEY_ALIAS")
val signingKeyPassword = System.getenv("NIVORA_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    signingStorePath,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "it.nivora.launcher"
    compileSdk = 36
    defaultConfig {
        applicationId = "it.nivora.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.5.3"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    if (hasReleaseSigning) {
        signingConfigs {
            create("stableRelease") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("stableRelease")
                isMinifyEnabled = false
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Newer native graphics-path binary helps keep bundled native code current for 16 KB page-size devices.
    implementation("androidx.graphics:graphics-path:1.1.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
