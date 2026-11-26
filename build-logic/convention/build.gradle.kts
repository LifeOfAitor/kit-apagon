plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id =
                libs.plugins.kitapagon.android.application
                    .get()
                    .pluginId
            implementationClass = "io.github.lifeofaitor.kitapagon.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.kitapagon.android.library
                    .get()
                    .pluginId
            implementationClass = "io.github.lifeofaitor.kitapagon.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("kotlinLibrary") {
            id =
                libs.plugins.kitapagon.kotlin.library
                    .get()
                    .pluginId
            implementationClass = "io.github.lifeofaitor.kitapagon.buildlogic.KotlinLibraryConventionPlugin"
        }
    }
}
