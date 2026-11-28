plugins {
    alias(libs.plugins.kitapagon.kotlin.library)
}

dependencies {
    api(project(":core:package-format"))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(testFixtures(project(":core:package-format")))
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.test)
}
