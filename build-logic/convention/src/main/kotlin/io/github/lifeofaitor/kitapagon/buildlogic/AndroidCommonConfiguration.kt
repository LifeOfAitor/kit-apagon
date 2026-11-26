package io.github.lifeofaitor.kitapagon.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureAndroidCommon(androidExtension: CommonExtension) {
    androidExtension.apply {
        compileSdk = ProjectConfiguration.COMPILE_SDK
        compileSdkMinor = ProjectConfiguration.COMPILE_SDK_MINOR
        defaultConfig.minSdk = ProjectConfiguration.MINIMUM_SDK
        compileOptions.sourceCompatibility = JavaVersion.toVersion(ProjectConfiguration.JAVA_BYTECODE_VERSION)
        compileOptions.targetCompatibility = JavaVersion.toVersion(ProjectConfiguration.JAVA_BYTECODE_VERSION)
        buildFeatures.compose = true
        testOptions.unitTests.isIncludeAndroidResources = true
        configureStrictLint(lint)
    }
    configureKotlinCompiler()
    addComposeDependencies()
}

private fun configureStrictLint(lint: Lint) {
    lint.apply {
        warningsAsErrors = true
        abortOnError = true
        // These checks report that a newer dependency or platform exists. Versions are upgraded
        // deliberately, one at a time, so a new release elsewhere must never break the build.
        disable +=
            setOf(
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable",
                "OldTargetApi",
            )
    }
}

private fun Project.configureKotlinCompiler() {
    extensions.getByType<KotlinAndroidProjectExtension>().compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(ProjectConfiguration.JAVA_BYTECODE_VERSION.toString()))
        allWarningsAsErrors.set(true)
    }
}

private fun Project.addComposeDependencies() {
    val libraries = versionCatalog()
    dependencies {
        add("implementation", platform(libraries.findLibrary("compose-bom").get()))
        add("implementation", libraries.findLibrary("compose-ui").get())
        add("implementation", libraries.findLibrary("compose-material3").get())
        add("implementation", libraries.findLibrary("compose-ui-tooling-preview").get())
        add("debugImplementation", libraries.findLibrary("compose-ui-tooling").get())
        add("testImplementation", platform(libraries.findLibrary("compose-bom").get()))
        add("testImplementation", libraries.findLibrary("compose-ui-test-junit4").get())
        add("testImplementation", libraries.findLibrary("junit4").get())
        add("testImplementation", libraries.findLibrary("robolectric").get())
        add("testImplementation", libraries.findLibrary("androidx-test-core").get())
        add("testImplementation", libraries.findLibrary("androidx-test-ext-junit").get())
        add("debugImplementation", libraries.findLibrary("compose-ui-test-manifest").get())
    }
}

internal fun Project.versionCatalog(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
