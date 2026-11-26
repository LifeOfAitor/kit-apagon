plugins {
    alias(libs.plugins.kitapagon.kotlin.library)
    application
}

application {
    mainClass.set("io.github.lifeofaitor.kitapagon.tools.packagegenerator.PackageGeneratorKt")
}

dependencies {
    implementation(project(":core:package-format"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(testFixtures(project(":core:package-format")))
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.test)
}
