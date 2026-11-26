package io.github.lifeofaitor.kitapagon.core.packageformat

/** The part of a PMTiles version 3 header the app needs to show a package. */
data class PmtilesHeader(
    val minimumZoom: Int,
    val maximumZoom: Int,
    val bounds: GeographicBounds,
)

sealed interface PmtilesHeaderResult {
    data class Valid(
        val header: PmtilesHeader,
    ) : PmtilesHeaderResult

    data class Invalid(
        val reason: InvalidPmtilesHeaderReason,
    ) : PmtilesHeaderResult
}

enum class InvalidPmtilesHeaderReason {
    UNREADABLE_FILE,
    TOO_SHORT,
    NOT_PMTILES,
    UNSUPPORTED_VERSION,
    NOT_VECTOR_TILES,
    INVALID_ZOOM_RANGE,
    INVALID_BOUNDS,
}
