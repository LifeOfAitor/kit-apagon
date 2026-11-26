package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PackageGeneratorTest {
    @Test
    fun readsThePlacesCommand() {
        assertEquals(
            PlacesCommand(File("in.geojson"), File("places.json")),
            PlacesCommand.parse(arrayOf("places", "in.geojson", "places.json")),
        )
    }

    @Test
    fun rejectsAnyOtherCommandLine() {
        assertNull(PlacesCommand.parse(arrayOf()))
        assertNull(PlacesCommand.parse(arrayOf("places", "only-input.geojson")))
        assertNull(PlacesCommand.parse(arrayOf("maps", "in.geojson", "out.json")))
    }

    @Test
    fun summarisesHowManyPlacesOfEachCategoryThereAre() {
        val places =
            listOf(
                place(PlaceCategory.DRINKING_WATER, "node/1"),
                place(PlaceCategory.PHARMACY, "node/2"),
                place(PlaceCategory.DRINKING_WATER, "node/3"),
            )

        assertEquals("pharmacy: 1\ndrinking_water: 2\ntotal: 3", PlacesSummary.describe(places))
    }

    private fun place(
        category: PlaceCategory,
        sourceId: String,
    ) = Place(category, GeographicPoint(43.3, -1.8), PlaceSource.OPENSTREETMAP, sourceId)
}
