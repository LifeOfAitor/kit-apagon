package io.github.lifeofaitor.kitapagon.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                namespace = ProjectConfiguration.NAMESPACE_PREFIX + "." + modulePathAsPackage()
            }
        }
    }

    private fun Project.modulePathAsPackage(): String = path.removePrefix(":").replace(':', '.').replace('-', '.')
}
