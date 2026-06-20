plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.shubham.verifiedcredential"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.shubham.verifiedcredential"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${providers.gradleProperty("BACKEND_BASE_URL").getOrElse("https://54ac-171-76-80-37.ngrok-free.app/")}\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.googleid)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation(libs.androidx.compiler)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.runtime.livedata)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.converter.gson)
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)
}
