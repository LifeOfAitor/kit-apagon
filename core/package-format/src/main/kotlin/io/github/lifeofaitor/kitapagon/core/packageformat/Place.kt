package io.github.lifeofaitor.kitapagon.core.packageformat

/** Kinds of places useful during a blackout (decision 0011). The wire name is the one in places.json. */
enum class PlaceCategory(
    val wireName: String,
) {
    PHARMACY("pharmacy"),
    HEALTH_CENTRE("health_centre"),
    DRINKING_WATER("drinking_water"),
    ASSEMBLY_POINT("assembly_point"),
    DEFIBRILLATOR("defibrillator"),
    POLICE("police"),
    FIRE_STATION("fire_station"),
    ;

    companion object {
        /** Returns null for categories this version of the app does not know yet. */
        fun fromWireName(wireName: String): PlaceCategory? = entries.firstOrNull { it.wireName == wireName }
    }
}

/** Where a place comes from, so the app can tell community data from official data (decision 0007). */
enum class PlaceSource(
    val wireName: String,
) {
    OPENSTREETMAP("openstreetmap"),
    OFFICIAL("official"),
    ;

    companion object {
        fun fromWireName(wireName: String): PlaceSource? = entries.firstOrNull { it.wireName == wireName }
    }
}

/** A position in WGS84 degrees. */
data class GeographicPoint(
    val latitude: Double,
    val longitude: Double,
) {
    val isValid: Boolean
        get() = latitude in -MAXIMUM_LATITUDE..MAXIMUM_LATITUDE && longitude in -MAXIMUM_LONGITUDE..MAXIMUM_LONGITUDE

    private companion object {
        const val MAXIMUM_LATITUDE = 90.0
        const val MAXIMUM_LONGITUDE = 180.0
    }
}

/** Optional information about a place. Most places in OpenStreetMap only have some of it. */
data class PlaceDetails(
    val name: String? = null,
    val address: String? = null,
    val openingHours: String? = null,
    val phone: String? = null,
)

data class Place(
    val category: PlaceCategory,
    val location: GeographicPoint,
    val source: PlaceSource,
    val sourceId: String,
    val details: PlaceDetails = PlaceDetails(),
)
