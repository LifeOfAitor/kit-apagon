package io.github.lifeofaitor.kitapagon.buildlogic

/**
 * Values shared by every Android module. Each one is fixed by an accepted decision in
 * docs/decisiones, so changing any of them requires a new decision.
 */
internal object ProjectConfiguration {
    const val NAMESPACE_PREFIX = "io.github.lifeofaitor.kitapagon"

    /** Decision 0002: Android 8.0 is the oldest supported version. */
    const val MINIMUM_SDK = 26

    const val COMPILE_SDK = 37

    /** Compose 1.13 and later require compiling against Android 37.1 or newer. */
    const val COMPILE_SDK_MINOR = 2

    /**
     * Kept one level below COMPILE_SDK until the Android 17 behaviour changes are reviewed
     * (pending item in docs/investigacion/04-plataforma-android-y-legal.md).
     */
    const val TARGET_SDK = 36

    const val JAVA_BYTECODE_VERSION = 17

    /** Minimum line coverage of the pure Kotlin modules, checked by koverVerify. */
    const val MINIMUM_LINE_COVERAGE_PERCENT = 90
}
