plugins {
    alias(libs.plugins.kitapagon.android.library)
}

dependencies {
    implementation(project(":core:package-format"))
    implementation(project(":core:ui"))
    implementation(libs.maplibre.android)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(testFixtures(project(":core:package-format")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.serialization.json)
}
