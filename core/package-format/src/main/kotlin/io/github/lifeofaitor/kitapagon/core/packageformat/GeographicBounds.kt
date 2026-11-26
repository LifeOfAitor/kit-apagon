package io.github.lifeofaitor.kitapagon.core.packageformat

/** Rectangle on the map in WGS84 degrees. Packages never cross the antimeridian. */
data class GeographicBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    val isValid: Boolean
        get() =
            west in LONGITUDE_RANGE &&
                east in LONGITUDE_RANGE &&
                south in LATITUDE_RANGE &&
                north in LATITUDE_RANGE &&
                west < east &&
                south < north

    private companion object {
        val LONGITUDE_RANGE = -180.0..180.0
        val LATITUDE_RANGE = -90.0..90.0
    }
}
