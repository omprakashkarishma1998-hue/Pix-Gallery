plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pixgallery.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pixgallery.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing reads from environment variables so the keystore itself
    // never has to be committed to the repo. Locally you'd export these
    // yourself before running ./gradlew assembleRelease; in GitHub Actions
    // they come from repo Secrets (see .github/workflows/build.yml).
    // If they're not set (e.g. a plain debug build), signingConfig below
    // simply isn't applied and the release build falls back to being
    // unsigned - assembleRelease will still work, it just won't produce an
    // installable signed APK until the env vars are provided.
    val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("RELEASE_KEY_ALIAS")
    val keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    val hasSigningEnv = !keystorePath.isNullOrBlank() &&
        !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    signingConfigs {
        if (hasSigningEnv) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningEnv) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")

    // Image loading (handles MediaStore / video thumbnails nicely)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // AdMob (banner ads)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    // Needed to detect when the app is brought to the foreground (for the App Open ad)
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
