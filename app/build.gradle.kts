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
    compileSdk = 35
    defaultConfig {
        applicationId = "it.nivora.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }
    buildFeatures { compose = true }
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
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
