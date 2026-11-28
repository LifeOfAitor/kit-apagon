package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/** Areas and points as the index and package.json write them, in the order of GeoJSON. */
internal object GeographicJson {
    fun boundsOf(element: JsonElement?): GeographicBounds? =
        element
            .numbersOrNull(BOUNDS_SIZE)
            ?.let { numbers -> GeographicBounds(numbers[WEST], numbers[SOUTH], numbers[EAST], numbers[NORTH]) }
            ?.takeIf { it.isValid }

    fun pointOf(element: JsonElement?): GeographicPoint? =
        element
            .numbersOrNull(POINT_SIZE)
            ?.let { (longitude, latitude) -> GeographicPoint(latitude, longitude) }
            ?.takeIf { it.isValid }

    fun arrayOf(bounds: GeographicBounds): JsonArray = numberArrayOf(bounds.run { listOf(west, south, east, north) })

    fun arrayOf(point: GeographicPoint): JsonArray = numberArrayOf(point.run { listOf(longitude, latitude) })

    private fun numberArrayOf(values: List<Double>): JsonArray =
        buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }

    private const val BOUNDS_SIZE = 4
    private const val POINT_SIZE = 2

    // Bounds are written as [west, south, east, north], like the bbox of GeoJSON.
    private const val WEST = 0
    private const val SOUTH = 1
    private const val EAST = 2
    private const val NORTH = 3
}
