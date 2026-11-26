package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityNames
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource

/** Irun without margin, rounded from its OpenStreetMap boundary. */
val IRUN_BOUNDS = GeographicBounds(west = -1.8523, south = 43.2864, east = -1.7293, north = 43.3533)

fun municipality(
    id: String = "ES-20045",
    officialName: String = "Irun",
    bounds: GeographicBounds = IRUN_BOUNDS,
): Municipality =
    Municipality(
        id = PackageId.parseOrNull(id) ?: error("Invalid test package id $id"),
        names = MunicipalityNames(officialName),
        provinceCode = id.removePrefix("ES-").take(2),
        bounds = bounds,
        centre = GeographicPoint((bounds.south + bounds.north) / 2, (bounds.west + bounds.east) / 2),
    )

fun place(
    latitude: Double,
    longitude: Double,
    name: String? = null,
    category: PlaceCategory = PlaceCategory.PHARMACY,
    sourceId: String = "node/$latitude,$longitude",
): Place =
    Place(
        category,
        GeographicPoint(latitude, longitude),
        PlaceSource.OPENSTREETMAP,
        sourceId,
        PlaceDetails(name = name),
    )

/** One line of `osmium export -f geojsonseq` for a municipal boundary drawn as a rectangle. */
fun boundaryLine(
    bounds: GeographicBounds,
    tags: Map<String, String>,
): String {
    val ring =
        listOf(
            bounds.west to bounds.south,
            bounds.east to bounds.south,
            bounds.east to bounds.north,
            bounds.west to bounds.north,
            bounds.west to bounds.south,
        ).joinToString(",") { (longitude, latitude) -> "[$longitude,$latitude]" }
    val properties = tags.entries.joinToString(",") { (key, value) -> "\"$key\":\"$value\"" }
    return "\u001E{\"type\":\"Feature\"," +
        "\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[$ring]]]},\"properties\":{$properties}}"
}

fun municipalityTags(
    ineCode: String,
    name: String,
    vararg otherTags: Pair<String, String>,
): Map<String, String> =
    mapOf("boundary" to "administrative", "admin_level" to "8", "ine:municipio" to ineCode, "name" to name) + otherTags
