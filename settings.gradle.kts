pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kit-apagon"

include(":app")
include(":core:ui")
include(":core:package-format")
include(":core:package-download")
include(":feature:map")
include(":tools:package-generator")
