plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kover) apply false
}

buildscript {
    dependencies {
        // Makes the built-in Kotlin support of the Android Gradle plugin use the Kotlin version
        // fixed in the version catalog instead of the older one it bundles.
        classpath(libs.kotlin.gradle.plugin)
    }
}

val ktlintClasspath: Configuration = configurations.create("ktlintClasspath")
val detektClasspath: Configuration = configurations.create("detektClasspath")

dependencies {
    ktlintClasspath(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
    detektClasspath(libs.detekt.cli)
}

val kotlinSourcePatterns = listOf("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

val ktlintCheck =
    tasks.register<JavaExec>("ktlintCheck") {
        group = "verification"
        description = "Checks Kotlin code formatting with ktlint."
        classpath = ktlintClasspath
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        args(kotlinSourcePatterns)
    }

val ktlintFormat =
    tasks.register<JavaExec>("ktlintFormat") {
        group = "formatting"
        description = "Fixes Kotlin code formatting with ktlint."
        classpath = ktlintClasspath
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        args(listOf("--format") + kotlinSourcePatterns)
    }

// detekt runs as a standalone tool instead of a Gradle plugin, so its outdated embedded Kotlin
// compiler never constrains the Kotlin version of the project (decision 0008).
val detektCheck =
    tasks.register<JavaExec>("detektCheck") {
        group = "verification"
        description = "Checks clean code rules with detekt."
        classpath = detektClasspath
        mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
        args(
            "--input",
            rootDir.absolutePath,
            "--excludes",
            "**/build/**,**/.gradle/**",
            "--config",
            file("config/detekt/detekt.yml").absolutePath,
            "--build-upon-default-config",
        )
    }

val noEmojiCheck =
    tasks.register<Exec>("noEmojiCheck") {
        group = "verification"
        description = "Fails if any tracked file contains an emoji."
        commandLine("scripts/check-no-emoji.sh")
    }

val downloadRetriesScriptTests =
    tasks.register<Exec>("downloadRetriesScriptTests") {
        group = "verification"
        description = "Runs the tests of the script that repeats the checks when a download fails."
        commandLine("scripts/tests/run-with-download-retries-test.sh")
    }

val monthlyPublicationScriptTests =
    tasks.register<Exec>("monthlyPublicationScriptTests") {
        group = "verification"
        description = "Runs the tests of the script that publishes the packages every month."
        commandLine("scripts/tests/publish-monthly-packages-test.sh")
    }

val monthCheckScriptTests =
    tasks.register<Exec>("monthCheckScriptTests") {
        group = "verification"
        description = "Runs the tests of the script that tells whether a month is already published."
        commandLine("scripts/tests/month-is-published-test.sh")
    }

val scriptTests =
    tasks.register("scriptTests") {
        group = "verification"
        description = "Runs the tests of the shell scripts."
        dependsOn(downloadRetriesScriptTests, monthlyPublicationScriptTests, monthCheckScriptTests)
    }

tasks.register("verifyAll") {
    group = "verification"
    description = "Runs every check required before merging: format, clean code, emojis, lint, tests and release build."
    dependsOn(
        ktlintCheck,
        detektCheck,
        noEmojiCheck,
        scriptTests,
        // The application lint also checks every module it depends on (checkDependencies).
        ":app:lint",
        ":app:testDebugUnitTest",
        ":core:ui:testDebugUnitTest",
        ":core:package-format:test",
        ":core:package-format:koverVerify",
        ":feature:map:testDebugUnitTest",
        ":tools:package-generator:test",
        ":tools:package-generator:koverVerify",
        ":app:assembleRelease",
    )
}
