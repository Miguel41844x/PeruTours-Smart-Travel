plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
<<<<<<< HEAD
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.perutours.smarttravel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.perutours.smarttravel"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
=======
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.example.perutours"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.perutours"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
>>>>>>> codex/correcciones-hu02
    }

    buildTypes {
        release {
<<<<<<< HEAD
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
=======
            optimization {
                enable = false
            }
>>>>>>> codex/correcciones-hu02
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
<<<<<<< HEAD
    kotlinOptions {
        jvmTarget = "11"
    }
=======
>>>>>>> codex/correcciones-hu02
    buildFeatures {
        compose = true
    }
}

dependencies {
<<<<<<< HEAD
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.ui.tooling)
}
=======
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    //componente de navegacion rutas
    implementation(libs.androidx.navigation.compose)
    //iconos extendidos de material3
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

// Firebase (BOM controla versiones)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)


    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.coil.compose)
    implementation(libs.google.play.services.location)

}
<<<<<<< HEAD
>>>>>>> codex/correcciones-hu02
=======
>>>>>>> codex/hu03
