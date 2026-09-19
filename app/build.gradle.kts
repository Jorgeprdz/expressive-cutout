import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val ciDebugKeystoreBase64 = rootProject.file("app/signing/expressive-debug.keystore.b64")
val ciDebugKeystoreFile = rootProject.layout.buildDirectory
    .file("ci-signing/expressive-debug.keystore")
    .get()
    .asFile

fun ensureCiDebugKeystore() {
    if (!ciDebugKeystoreBase64.exists()) return
    val decoded = Base64.getMimeDecoder().decode(ciDebugKeystoreBase64.readText())
    ciDebugKeystoreFile.parentFile.mkdirs()
    if (!ciDebugKeystoreFile.exists() || !ciDebugKeystoreFile.readBytes().contentEquals(decoded)) {
        ciDebugKeystoreFile.writeBytes(decoded)
    }
}

android {
    namespace = "com.ekoehler.expressivecutout"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ekoehler.expressivecutout"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.0-beta"
    }

    signingConfigs {
        getByName("debug") {
            if (ciDebugKeystoreBase64.exists()) {
                ensureCiDebugKeystore()
                storeFile = ciDebugKeystoreFile
                storePassword = "expressive"
                keyAlias = "expressive-debug"
                keyPassword = "expressive"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        aidl = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.lottie.compose)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
