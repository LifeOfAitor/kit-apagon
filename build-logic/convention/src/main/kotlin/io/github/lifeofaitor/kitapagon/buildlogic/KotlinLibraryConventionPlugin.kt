package io.github.lifeofaitor.kitapagon.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Pure Kotlin modules hold the logic that does not need Android (decision 0008). Their tests run
 * on the JVM in seconds, so they are held to a minimum line coverage.
 */
class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            configureJavaAndKotlin()
            configureJUnitPlatform()
            configureMinimumCoverage()
        }
    }

    private fun Project.configureJavaAndKotlin() {
        val bytecodeVersion = JavaVersion.toVersion(ProjectConfiguration.JAVA_BYTECODE_VERSION)
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = bytecodeVersion
            targetCompatibility = bytecodeVersion
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(ProjectConfiguration.JAVA_BYTECODE_VERSION.toString()))
                allWarningsAsErrors.set(true)
            }
        }
    }

    private fun Project.configureJUnitPlatform() {
        val libraries = versionCatalog()
        dependencies {
            add("testImplementation", platform(libraries.findLibrary("junit-bom").get()))
            add("testImplementation", libraries.findLibrary("junit-jupiter").get())
            add("testImplementation", libraries.findLibrary("kotlin-test-junit5").get())
            add("testRuntimeOnly", libraries.findLibrary("junit-platform-launcher").get())
        }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }

    private fun Project.configureMinimumCoverage() {
        extensions.configure<KoverProjectExtension> {
            reports.verify.rule {
                minBound(ProjectConfiguration.MINIMUM_LINE_COVERAGE_PERCENT)
            }
        }
    }
}
