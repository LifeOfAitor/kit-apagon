package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory

/** Maps OpenStreetMap tags to the place categories of decision 0011. Anything else is discarded. */
object OsmPlaceClassifier {
    private val HEALTH_CENTRE_AMENITIES = setOf("hospital", "clinic", "doctors")
    private val HEALTH_CENTRE_HEALTHCARE_VALUES = setOf("hospital", "clinic", "doctor", "centre")
    private val DRINKING_WATER_AMENITIES = setOf("drinking_water", "water_point")

    fun categoryOf(tags: Map<String, String>): PlaceCategory? {
        val amenity = tags["amenity"]
        return when {
            // Checked first: pharmacies are also tagged healthcare=pharmacy.
            amenity == "pharmacy" || tags["healthcare"] == "pharmacy" -> PlaceCategory.PHARMACY

            isHealthCentre(tags) -> PlaceCategory.HEALTH_CENTRE

            amenity in DRINKING_WATER_AMENITIES -> PlaceCategory.DRINKING_WATER

            tags["emergency"] == "assembly_point" -> PlaceCategory.ASSEMBLY_POINT

            tags["emergency"] == "defibrillator" -> PlaceCategory.DEFIBRILLATOR

            amenity == "police" -> PlaceCategory.POLICE

            amenity == "fire_station" -> PlaceCategory.FIRE_STATION

            else -> null
        }
    }

    // Veterinary hospitals are tagged healthcare=hospital too, and must never look like a hospital.
    private fun isHealthCentre(tags: Map<String, String>): Boolean {
        val isVeterinary = tags["amenity"] == "veterinary"
        val isHealthCentre =
            tags["amenity"] in HEALTH_CENTRE_AMENITIES || tags["healthcare"] in HEALTH_CENTRE_HEALTHCARE_VALUES
        return isHealthCentre && !isVeterinary
    }
}
