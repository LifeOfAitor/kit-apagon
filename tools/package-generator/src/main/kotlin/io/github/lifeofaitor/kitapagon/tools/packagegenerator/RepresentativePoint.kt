package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.abs

/**
 * Reduces a GeoJSON geometry to the single point the app shows. Places drawn as an outline, such as
 * hospitals, use the centroid of their outer ring; for several outlines, the largest one wins.
 */
object RepresentativePoint {
    fun of(geometry: JsonObject): GeographicPoint? {
        val coordinates = geometry["coordinates"] as? JsonArray ?: return null
        return when ((geometry["type"] as? JsonPrimitive)?.content) {
            "Point" -> pointOf(coordinates)
            "Polygon" -> outerRingOf(coordinates)?.let(::centroidOf)
            "MultiPolygon" -> largestOuterRingOf(coordinates)?.let(::centroidOf)
            else -> null
        }
    }

    private fun pointOf(position: JsonElement): GeographicPoint? {
        val values = (position as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
        return if (values != null &&
            values.size >= 2
        ) {
            GeographicPoint(latitude = values[1], longitude = values[0])
        } else {
            null
        }
    }

    private fun outerRingOf(polygon: JsonElement): List<GeographicPoint>? {
        val ring = (polygon as? JsonArray)?.firstOrNull() as? JsonArray
        return ring?.mapNotNull(::pointOf)?.takeIf { points -> points.size >= MINIMUM_RING_SIZE }
    }

    private fun largestOuterRingOf(multiPolygon: JsonArray): List<GeographicPoint>? =
        multiPolygon.mapNotNull(::outerRingOf).maxByOrNull { ring -> abs(signedAreaOf(ring)) }

    // Area-weighted centroid of a closed ring (shoelace formula). Falls back to the average of the
    // vertices for degenerate rings whose area is zero.
    private fun centroidOf(ring: List<GeographicPoint>): GeographicPoint {
        val area = signedAreaOf(ring)
        if (area == 0.0) return averageOf(ring)
        val edges = ring.zipWithNext()
        val longitude = edges.sumOf { (from, to) -> (from.longitude + to.longitude) * crossProduct(from, to) }
        val latitude = edges.sumOf { (from, to) -> (from.latitude + to.latitude) * crossProduct(from, to) }
        return GeographicPoint(latitude = latitude / (SIX * area), longitude = longitude / (SIX * area))
    }

    private fun signedAreaOf(ring: List<GeographicPoint>): Double =
        ring.zipWithNext().sumOf { (from, to) -> crossProduct(from, to) } / 2

    private fun crossProduct(
        from: GeographicPoint,
        to: GeographicPoint,
    ): Double = from.longitude * to.latitude - to.longitude * from.latitude

    private fun averageOf(ring: List<GeographicPoint>): GeographicPoint =
        GeographicPoint(latitude = ring.map { it.latitude }.average(), longitude = ring.map { it.longitude }.average())

    // A closed ring repeats its first point, so a triangle has four positions.
    private const val MINIMUM_RING_SIZE = 4
    private const val SIX = 6
}
