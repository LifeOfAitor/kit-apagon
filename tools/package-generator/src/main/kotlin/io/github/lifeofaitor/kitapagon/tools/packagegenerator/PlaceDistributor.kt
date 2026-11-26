package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Gives each municipality the places inside its package area. A place near a border belongs to
 * every municipality whose margin reaches it, which is the point of the margin (decision 0006).
 */
object PlaceDistributor {
    // A cell of about 11 km: package areas span a few cells, so each place is compared with a handful
    // of municipalities instead of all of Spain.
    private const val CELL_SIZE_DEGREES = 0.1

    fun distribute(
        places: List<Place>,
        municipalities: List<Municipality>,
    ): Map<PackageId, List<Place>> {
        val municipalitiesByCell = indexByCell(municipalities)
        val placesByMunicipality = municipalities.associate { it.id to mutableListOf<Place>() }
        places.forEach { place ->
            municipalitiesByCell[cellOf(place.location)]
                .orEmpty()
                .filter { municipality -> municipality.packageArea.contains(place.location) }
                .forEach { municipality -> placesByMunicipality.getValue(municipality.id) += place }
        }
        return placesByMunicipality.mapValues { (_, municipalityPlaces) -> DuplicatePlaces.remove(municipalityPlaces) }
    }

    private fun indexByCell(municipalities: List<Municipality>): Map<Cell, List<Municipality>> {
        val index = mutableMapOf<Cell, MutableList<Municipality>>()
        municipalities.forEach { municipality ->
            val area = municipality.packageArea
            val southWest = cellOf(GeographicPoint(area.south, area.west))
            val northEast = cellOf(GeographicPoint(area.north, area.east))
            for (column in southWest.column..northEast.column) {
                for (row in southWest.row..northEast.row) {
                    index.getOrPut(Cell(column, row)) { mutableListOf() } += municipality
                }
            }
        }
        return index
    }

    private fun cellOf(point: GeographicPoint): Cell =
        Cell(floor(point.longitude / CELL_SIZE_DEGREES).toInt(), floor(point.latitude / CELL_SIZE_DEGREES).toInt())

    private data class Cell(
        val column: Int,
        val row: Int,
    )
}

/**
 * The same place is sometimes mapped twice in OpenStreetMap. Places of the same category with the same
 * name less than 10 m apart are one place, and the first in the list is kept. The distance is small on
 * purpose: with 150 m, the rule removed different fountains and defibrillators with generic names such
 * as "Iturria" or "Desfibrilador", and losing a real fountain is worse than showing a pharmacy twice
 * (docs/investigacion/09-paquetes-de-toda-espana.md). Unnamed places are never merged.
 */
object DuplicatePlaces {
    private const val MAXIMUM_DISTANCE_METRES = 10.0
    private const val METRES_PER_DEGREE_OF_LATITUDE = 111_320.0

    fun remove(places: List<Place>): List<Place> {
        val keptByIdentity = mutableMapOf<PlaceIdentity, MutableList<Place>>()
        val kept = mutableListOf<Place>()
        places.forEach { place ->
            val identity = identityOf(place)
            val sameIdentity = identity?.let { keptByIdentity.getOrPut(it) { mutableListOf() } }
            if (sameIdentity == null || sameIdentity.none { keptPlace -> areClose(keptPlace, place) }) {
                sameIdentity?.add(place)
                kept += place
            }
        }
        return kept
    }

    private fun identityOf(place: Place): PlaceIdentity? =
        place.details.name
            ?.trim()
            ?.lowercase()
            ?.takeIf { name -> name.isNotEmpty() }
            ?.let { name -> PlaceIdentity(place.category, name) }

    private fun areClose(
        first: Place,
        second: Place,
    ): Boolean = distanceInMetres(first.location, second.location) < MAXIMUM_DISTANCE_METRES

    // Equirectangular approximation: precise enough for distances of a few metres.
    private fun distanceInMetres(
        first: GeographicPoint,
        second: GeographicPoint,
    ): Double {
        val latitudeMetres = (first.latitude - second.latitude) * METRES_PER_DEGREE_OF_LATITUDE
        val middleLatitude = Math.toRadians((first.latitude + second.latitude) / 2)
        val longitudeMetres = (first.longitude - second.longitude) * METRES_PER_DEGREE_OF_LATITUDE * cos(middleLatitude)
        return sqrt(latitudeMetres * latitudeMetres + longitudeMetres * longitudeMetres)
    }

    private data class PlaceIdentity(
        val category: PlaceCategory,
        val normalisedName: String,
    )
}
