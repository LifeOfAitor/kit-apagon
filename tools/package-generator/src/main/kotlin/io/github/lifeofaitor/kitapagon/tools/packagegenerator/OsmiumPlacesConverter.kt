package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToLong

/**
 * Converts the GeoJSON written by `osmium export -u type_id` into the places of a package.
 * Features that are not one of the categories of decision 0011 are discarded.
 */
object OsmiumPlacesConverter {
    private val OSM_TYPES = mapOf('n' to "node", 'w' to "way", 'r' to "relation", 'a' to "area")

    // About 11 cm, more than enough to find a place and it keeps the file small.
    private const val COORDINATE_DECIMALS_SCALE = 1_000_000.0

    /** Throws IllegalArgumentException when the input is not the GeoJSON osmium produces. */
    fun convert(osmiumGeoJson: String): List<Place> {
        val features = (Json.parseToJsonElement(osmiumGeoJson) as? JsonObject)?.get("features") as? JsonArray
        requireNotNull(features) { "The input is not a GeoJSON FeatureCollection exported by osmium." }
        return features
            .mapNotNull(::toPlaceOrNull)
            .sortedWith(compareBy({ place -> place.category.ordinal }, { place -> place.sourceId }))
    }

    private fun toPlaceOrNull(feature: JsonElement): Place? {
        val featureObject = feature as? JsonObject ?: return null
        val tags = tagsOf(featureObject)
        val category = OsmPlaceClassifier.categoryOf(tags)
        val location = (featureObject["geometry"] as? JsonObject)?.let(RepresentativePoint::of)?.takeIf { it.isValid }
        val sourceId = sourceIdOf(featureObject)
        return if (category != null && location != null && sourceId != null) {
            Place(category, rounded(location), PlaceSource.OPENSTREETMAP, sourceId, detailsOf(tags))
        } else {
            null
        }
    }

    private fun tagsOf(feature: JsonObject): Map<String, String> =
        (feature["properties"] as? JsonObject)
            .orEmpty()
            .mapNotNull { (key, value) -> (value as? JsonPrimitive)?.takeIf { it.isString }?.let { key to it.content } }
            .toMap()

    // osmium writes identifiers such as "n479837125"; the package keeps the OpenStreetMap form "node/479837125".
    private fun sourceIdOf(feature: JsonObject): String? {
        val identifier = (feature["id"] as? JsonPrimitive)?.content ?: return null
        val type = OSM_TYPES[identifier.firstOrNull()]
        val number = identifier.drop(1).takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        return if (type != null && number != null) "$type/$number" else null
    }

    private fun detailsOf(tags: Map<String, String>): PlaceDetails =
        PlaceDetails(
            name = tags["name"],
            address = addressOf(tags),
            openingHours = tags["opening_hours"],
            phone = tags["phone"] ?: tags["contact:phone"],
        )

    private fun addressOf(tags: Map<String, String>): String? {
        val street = tags["addr:street"] ?: return null
        return listOfNotNull(street, tags["addr:housenumber"]).joinToString(" ")
    }

    private fun rounded(point: GeographicPoint): GeographicPoint =
        GeographicPoint(latitude = roundCoordinate(point.latitude), longitude = roundCoordinate(point.longitude))

    private fun roundCoordinate(value: Double): Double =
        (value * COORDINATE_DECIMALS_SCALE).roundToLong() / COORDINATE_DECIMALS_SCALE
}
