plugins {
    alias(libs.plugins.kitapagon.android.library)
}

// The app carries the key that signs the index, read when building from the only copy in the
// repository, so the two can never differ (decision 0017).
val indexPublicKey =
    providers
        .fileContents(
            rootProject.layout.projectDirectory.file("tools/package-generator/data/clave-publica-del-indice.txt"),
        ).asText
        .map { text -> text.trim() }

android {
    buildFeatures.buildConfig = true
    defaultConfig {
        val key = indexPublicKey.get()
        require(Regex("^[A-Za-z0-9+/]+={0,2}$").matches(key)) { "The public key of the index is not Base64." }
        buildConfigField("String", "INDEX_PUBLIC_KEY", "\"$key\"")
    }
}

dependencies {
    implementation(project(":core:package-format"))
    implementation(project(":core:package-download"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(testFixtures(project(":core:package-format")))
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
