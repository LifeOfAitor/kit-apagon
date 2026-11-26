plugins {
    alias(libs.plugins.kitapagon.kotlin.library)
    `java-test-fixtures`
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.test)
}
