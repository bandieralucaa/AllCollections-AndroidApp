import org.gradle.kotlin.dsl.android
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

val cloudName = localProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")
val apiKey = localProperties.getProperty("CLOUDINARY_API_KEY", "")
val apiSecret = localProperties.getProperty("CLOUDINARY_API_SECRET", "")

android {
    namespace = "com.example.allcollections"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.allcollections"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudName\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"$apiSecret\"")

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }

    ndkVersion = "28.0.13004108"

    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "AllCollections.apk"
        }
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose BOM e dipendenze Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Firebase BOM aggiornato
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Cloudinary aggiornato
    implementation("com.cloudinary:cloudinary-android:3.1.2")

    // Koin per Dependency Injection
    implementation("io.insert-koin:koin-android:3.5.6")
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")

    // Altre librerie
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.github.dhaval2404:imagepicker:2.1")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    implementation("androidx.graphics:graphics-core:1.0.2")
    implementation("androidx.core:core:1.15.0")
}