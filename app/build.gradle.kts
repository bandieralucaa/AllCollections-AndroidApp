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
            minSdk = 26
            targetSdk = 35
            versionCode = 1
            versionName = "1.0"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${cloudName ?: ""}\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"${apiKey ?: ""}\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${apiSecret ?: ""}\"")

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

        kotlinOptions {
            jvmTarget = "11"
        }

        buildFeatures {
            compose = true
            buildConfig = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.1"
        }
    }

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.cloudinary:cloudinary-android:3.0.2")
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.github.dhaval2404:imagepicker:2.1")

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


}
