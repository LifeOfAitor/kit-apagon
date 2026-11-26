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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
