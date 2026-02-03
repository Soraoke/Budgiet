import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val myMinSdk = 26
val myTargetSdk = 36

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.budgiet"
    compileSdk {
        version = release(myTargetSdk)
    }

    defaultConfig {
        applicationId = "com.example.budgiet"
        minSdk = myMinSdk
        targetSdk = myTargetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        @Suppress("UnstableApiUsage")
        managedDevices {
            localDevices {
                create("pixel2api34") {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 2"
                    // Use only API levels 34 and higher.
                    apiLevel = 34
                    // To include Google services, use "google".
                    systemImageSource = "aosp"
                }
                create("pixel5api35") {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 5"
                    // Use only API levels 34 and higher.
                    apiLevel = 35
                    // To include Google services, use "google".
                    systemImageSource = "aosp"
                }
                create("pixel9aapi36") {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 9a"
                    // Use only API levels 34 and higher.
                    apiLevel = 36
                    // To include Google services, use "google".
                    systemImageSource = "aosp"
                }
            }
            groups {
                create("pixelDevices") {
                    targetDevices.add(localDevices["pixel2api34"])
                    targetDevices.add(localDevices["pixel5api35"])
                    targetDevices.add(localDevices["pixel9aapi36"])
                }
            }
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.paging.common)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.androidx.paging.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}