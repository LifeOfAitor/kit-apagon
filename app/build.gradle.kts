plugins {
    alias(libs.plugins.kitapagon.android.application)
}

android {
    // The identifier can still change freely until the first public release; after that it is
    // permanent, because stores use it to recognise the app.
    namespace = "io.github.lifeofaitor.kitapagon.app"

    defaultConfig {
        applicationId = "io.github.lifeofaitor.kitapagon"
        versionCode = 1
        versionName = "0.1.0"
    }

    // One APK per processor architecture: the native map library alone takes 9-13 MB per
    // architecture (decision 0010). x86_64 is only for the emulator.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:package-format"))
    implementation(project(":feature:map"))
    implementation(project(":feature:packages"))
    implementation(project(":core:package-download"))
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
