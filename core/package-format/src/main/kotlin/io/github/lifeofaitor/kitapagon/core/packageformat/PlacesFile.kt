package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

/**
 * The places.json file of a package: a GeoJSON FeatureCollection of points (RFC 7946, decision 0011).
 * The file name is shared with InstalledPackages.
 */
data class PlacesFile(
    val places: List<Place>,
    /** Features that were not valid or had a category this version does not know. */
    val skippedFeatureCount: Int,
)

sealed interface PlacesFileResult {
    data class Valid(
        val placesFile: PlacesFile,
    ) : PlacesFileResult

    data class Invalid(
        val reason: InvalidPlacesFileReason,
    ) : PlacesFileResult
}

enum class InvalidPlacesFileReason {
    NOT_JSON,
    NOT_A_FEATURE_COLLECTION,
}

/**
 * Reads places.json. A broken feature is skipped instead of rejecting the whole file, so one bad
 * place never hides all the others.
 */
object PlacesFileParser {
    fun parse(text: String): PlacesFileResult {
        val root = parseJsonOrNull(text)
        val features = root?.let(::featuresOf)
        return when {
            root == null -> PlacesFileResult.Invalid(InvalidPlacesFileReason.NOT_JSON)
            features == null -> PlacesFileResult.Invalid(InvalidPlacesFileReason.NOT_A_FEATURE_COLLECTION)
            else -> PlacesFileResult.Valid(placesFileOf(features))
        }
    }

    private fun parseJsonOrNull(text: String): JsonElement? =
        try {
            Json.parseToJsonElement(text)
        } catch (expectedForMalformedJson: SerializationException) {
            null
        }

    private fun featuresOf(root: JsonElement): JsonArray? {
        val collection = root as? JsonObject
        val isFeatureCollection = collection?.stringOrNull(GeoJson.TYPE) == GeoJson.FEATURE_COLLECTION
        return if (isFeatureCollection) collection[GeoJson.FEATURES] as? JsonArray else null
    }

    private fun placesFileOf(features: JsonArray): PlacesFile {
        val places = features.map { feature -> toPlaceOrNull(feature) }
        return PlacesFile(places.filterNotNull(), skippedFeatureCount = places.count { place -> place == null })
    }

    private fun toPlaceOrNull(feature: JsonElement): Place? {
        val featureObject = feature as? JsonObject
        val location = pointOf(featureObject?.get(GeoJson.GEOMETRY))?.takeIf { point -> point.isValid }
        val fields = (featureObject?.get(GeoJson.PROPERTIES) as? JsonObject)?.let(::placeFieldsOf)
        return if (location != null && fields != null) {
            Place(fields.category, location, fields.source, fields.sourceId, fields.details)
        } else {
            null
        }
    }

    /** Everything a place needs besides its location; null when a mandatory property is missing or unknown. */
    private fun placeFieldsOf(properties: JsonObject): PlaceFields? {
        val category = properties.stringOrNull(PlaceProperty.CATEGORY)?.let(PlaceCategory::fromWireName)
        val source = properties.stringOrNull(PlaceProperty.SOURCE)?.let(PlaceSource::fromWireName)
        val sourceId = properties.nonBlankStringOrNull(PlaceProperty.SOURCE_ID)
        return if (category != null && source != null && sourceId != null) {
            PlaceFields(category, source, sourceId, detailsOf(properties))
        } else {
            null
        }
    }

    private data class PlaceFields(
        val category: PlaceCategory,
        val source: PlaceSource,
        val sourceId: String,
        val details: PlaceDetails,
    )

    private fun pointOf(geometry: JsonElement?): GeographicPoint? {
        val geometryObject = geometry as? JsonObject ?: return null
        val coordinates = geometryObject[GeoJson.COORDINATES] as? JsonArray
        val isPoint = geometryObject.stringOrNull(GeoJson.TYPE) == GeoJson.POINT && coordinates?.size == 2
        val longitude = coordinates?.getOrNull(0)?.numberOrNull()
        val latitude = coordinates?.getOrNull(1)?.numberOrNull()
        return if (isPoint && longitude != null && latitude != null) GeographicPoint(latitude, longitude) else null
    }

    private fun detailsOf(properties: JsonObject): PlaceDetails =
        PlaceDetails(
            name = properties.nonBlankStringOrNull(PlaceProperty.NAME),
            address = properties.nonBlankStringOrNull(PlaceProperty.ADDRESS),
            openingHours = properties.nonBlankStringOrNull(PlaceProperty.OPENING_HOURS),
            phone = properties.nonBlankStringOrNull(PlaceProperty.PHONE),
        )
}

private fun JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content

private fun JsonObject.nonBlankStringOrNull(key: String): String? = stringOrNull(key)?.takeIf { it.isNotBlank() }

private fun JsonElement.numberOrNull(): Double? = (this as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull

/** Writes places.json. Used by the package generator; the app only reads. */
object PlacesFileWriter {
    fun write(places: List<Place>): String =
        buildJsonObject {
            put(GeoJson.TYPE, GeoJson.FEATURE_COLLECTION)
            put(GeoJson.FEATURES, buildJsonArray { places.forEach { place -> add(featureOf(place)) } })
        }.toString()

    private fun featureOf(place: Place): JsonObject =
        buildJsonObject {
            put(GeoJson.TYPE, GeoJson.FEATURE)
            put(
                GeoJson.GEOMETRY,
                buildJsonObject {
                    put(GeoJson.TYPE, GeoJson.POINT)
                    put(
                        GeoJson.COORDINATES,
                        buildJsonArray {
                            add(JsonPrimitive(place.location.longitude))
                            add(JsonPrimitive(place.location.latitude))
                        },
                    )
                },
            )
            put(GeoJson.PROPERTIES, propertiesOf(place))
        }

    private fun propertiesOf(place: Place): JsonObject =
        buildJsonObject {
            put(PlaceProperty.CATEGORY, place.category.wireName)
            put(PlaceProperty.SOURCE, place.source.wireName)
            put(PlaceProperty.SOURCE_ID, place.sourceId)
            place.details.name?.let { name -> put(PlaceProperty.NAME, name) }
            place.details.address?.let { address -> put(PlaceProperty.ADDRESS, address) }
            place.details.openingHours?.let { hours -> put(PlaceProperty.OPENING_HOURS, hours) }
            place.details.phone?.let { phone -> put(PlaceProperty.PHONE, phone) }
        }
}

private object GeoJson {
    const val TYPE = "type"
    const val FEATURE_COLLECTION = "FeatureCollection"
    const val FEATURE = "Feature"
    const val FEATURES = "features"
    const val GEOMETRY = "geometry"
    const val POINT = "Point"
    const val COORDINATES = "coordinates"
    const val PROPERTIES = "properties"
}

private object PlaceProperty {
    const val CATEGORY = "category"
    const val SOURCE = "source"
    const val SOURCE_ID = "source_id"
    const val NAME = "name"
    const val ADDRESS = "address"
    const val OPENING_HOURS = "opening_hours"
    const val PHONE = "phone"
}
