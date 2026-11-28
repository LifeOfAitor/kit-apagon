package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityNames
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.cos

data class Municipality(
    val id: PackageId,
    val names: MunicipalityNames,
    val provinceCode: String,
    /** The municipality itself, without margin. */
    val bounds: GeographicBounds,
    val centre: GeographicPoint,
    /** Set when the boundary had to be completed from Overpass (decision 0016). */
    val completion: BoundaryCompletion? = null,
) {
    /** What the package covers: the municipality plus the margin of decision 0006. */
    val packageArea: GeographicBounds get() = bounds.expandedBy(PACKAGE_MARGIN_KILOMETRES)

    private companion object {
        const val PACKAGE_MARGIN_KILOMETRES = 3.0
    }
}

/**
 * Reads Spanish municipalities from the boundaries exported by `osmium export -f geojsonseq`
 * (decision 0013): administrative boundaries of level 8 with a five digit INE code. Mancomunidades,
 * French communes and other boundaries without a code are discarded.
 */
object MunicipalityReader {
    private val INE_CODE = Regex("^[0-9]{5}$")
    private val ALTERNATIVE_NAME_TAGS = listOf("name:es", "name:eu", "name:ca", "name:gl", "official_name", "alt_name")
    private const val MUNICIPALITY_LEVEL = "8"
    private const val SPAIN_PREFIX = "ES-"
    private const val PROVINCE_CODE_LENGTH = 2

    /** Throws IllegalArgumentException when a line is not JSON. */
    fun read(boundaryFeatureLines: Sequence<String>): List<Municipality> =
        OsmiumFeatures
            .featuresOf(boundaryFeatureLines)
            .mapNotNull(::toMunicipalityOrNull)
            .groupBy { municipality -> municipality.id }
            .map { (_, parts) -> parts.reduce(::mergeParts) }
            .sortedBy { municipality -> municipality.id.value }

    private fun toMunicipalityOrNull(feature: JsonElement): Municipality? {
        val featureObject = feature as? JsonObject ?: return null
        val tags = OsmiumFeatures.tagsOf(featureObject)
        val id = municipalityIdOf(tags)
        val name = tags["name"]
        val location = (featureObject["geometry"] as? JsonObject)?.let(::locationOf)
        return if (id != null && name != null && location != null) {
            Municipality(
                id,
                namesOf(name, tags),
                provinceCodeOf(id),
                location.first,
                location.second,
                BoundaryCompletion.from(tags),
            )
        } else {
            null
        }
    }

    private fun municipalityIdOf(tags: Map<String, String>): PackageId? =
        tags["ine:municipio"]
            ?.takeIf { code -> tags["admin_level"] == MUNICIPALITY_LEVEL && INE_CODE.matches(code) }
            ?.let { code -> PackageId.parseOrNull(SPAIN_PREFIX + code) }

    private fun provinceCodeOf(id: PackageId): String = id.value.removePrefix(SPAIN_PREFIX).take(PROVINCE_CODE_LENGTH)

    private fun locationOf(geometry: JsonObject): Pair<GeographicBounds, GeographicPoint>? {
        val bounds = boundsOf(geometry)
        val centre = RepresentativePoint.of(geometry)
        return if (bounds != null && centre != null) bounds to centre else null
    }

    private fun namesOf(
        officialName: String,
        tags: Map<String, String>,
    ): MunicipalityNames {
        val alternatives =
            ALTERNATIVE_NAME_TAGS
                .mapNotNull { tag -> tags[tag] }
                .flatMap { value -> value.split(";") }
                .map { name -> name.trim() }
                .filter { name -> name.isNotEmpty() && name != officialName }
                .distinct()
        return MunicipalityNames(officialName, alternatives)
    }

    // A municipality split in several boundaries, such as one with an exclave, keeps the union of
    // their areas and the centre of its first part.
    private fun mergeParts(
        first: Municipality,
        second: Municipality,
    ): Municipality = first.copy(bounds = first.bounds.union(second.bounds))

    private fun boundsOf(geometry: JsonObject): GeographicBounds? {
        val positions = positionsOf(geometry["coordinates"]).toList()
        if (positions.isEmpty()) return null
        return GeographicBounds(
            west = positions.minOf { it.longitude },
            south = positions.minOf { it.latitude },
            east = positions.maxOf { it.longitude },
            north = positions.maxOf { it.latitude },
        ).takeIf { it.isValid }
    }

    // Walks nested coordinate arrays of any depth (Polygon or MultiPolygon) down to their positions.
    private fun positionsOf(element: JsonElement?): Sequence<GeographicPoint> {
        val array = element as? JsonArray ?: return emptySequence()
        val longitude = (array.getOrNull(0) as? JsonPrimitive)?.doubleOrNull
        val latitude = (array.getOrNull(1) as? JsonPrimitive)?.doubleOrNull
        return if (longitude != null && latitude != null) {
            sequenceOf(GeographicPoint(latitude, longitude))
        } else {
            array.asSequence().flatMap(::positionsOf)
        }
    }
}

/** The smallest rectangle containing both. */
fun GeographicBounds.union(other: GeographicBounds): GeographicBounds =
    GeographicBounds(
        west = minOf(west, other.west),
        south = minOf(south, other.south),
        east = maxOf(east, other.east),
        north = maxOf(north, other.north),
    )

fun GeographicBounds.contains(point: GeographicPoint): Boolean =
    point.longitude in west..east && point.latitude in south..north

/** The smallest rectangle containing every one, or null for none. */
fun Iterable<GeographicBounds>.unionOrNull(): GeographicBounds? = reduceOrNull(GeographicBounds::union)

/** Grows the rectangle by about [kilometres] on every side, never past the edges of the world. */
fun GeographicBounds.expandedBy(kilometres: Double): GeographicBounds {
    val latitudeDegrees = kilometres / KILOMETRES_PER_DEGREE_OF_LATITUDE
    val middleLatitude = Math.toRadians((south + north) / 2)
    val longitudeDegrees = kilometres / (KILOMETRES_PER_DEGREE_OF_LATITUDE * cos(middleLatitude))
    return GeographicBounds(
        west = (west - longitudeDegrees).coerceAtLeast(-MAXIMUM_LONGITUDE),
        south = (south - latitudeDegrees).coerceAtLeast(-MAXIMUM_LATITUDE),
        east = (east + longitudeDegrees).coerceAtMost(MAXIMUM_LONGITUDE),
        north = (north + latitudeDegrees).coerceAtMost(MAXIMUM_LATITUDE),
    )
}

private const val KILOMETRES_PER_DEGREE_OF_LATITUDE = 111.32
private const val MAXIMUM_LONGITUDE = 180.0
private const val MAXIMUM_LATITUDE = 90.0
