package io.github.lifeofaitor.kitapagon.feature.map

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource
import java.io.File

/** Places and states shared by the tests of the map feature. */
object TestPlaces {
    val IRUN: PackageId = PackageId.parseOrNull("ES-20045")!!
    val IRUN_AREA = GeographicBounds(west = -1.9, south = 43.2, east = -1.6, north = 43.4)

    val PHARMACY =
        Place(
            category = PlaceCategory.PHARMACY,
            location = GeographicPoint(43.3697, -1.7984),
            source = PlaceSource.OPENSTREETMAP,
            sourceId = "node/479837125",
            details =
                PlaceDetails(
                    name = "Aroia Larrea",
                    address = "Donostia kalea 25",
                    openingHours = "Mo-Fr 09:00-20:00",
                    phone = "+34 943 000 000",
                ),
        )

    val UNNAMED_FOUNTAIN =
        Place(PlaceCategory.DRINKING_WATER, GeographicPoint(43.338, -1.789), PlaceSource.OPENSTREETMAP, "node/2")

    val HOSPITAL =
        Place(
            PlaceCategory.HEALTH_CENTRE,
            GeographicPoint(43.34, -1.79),
            PlaceSource.OFFICIAL,
            "irun/hospital/1",
            PlaceDetails(name = "Bidasoko Ospitalea"),
        )

    fun readyState(places: List<Place> = listOf(PHARMACY, UNNAMED_FOUNTAIN, HOSPITAL)) =
        MapScreenState.Ready(IRUN, File("map.pmtiles"), IRUN_AREA, places)
}
