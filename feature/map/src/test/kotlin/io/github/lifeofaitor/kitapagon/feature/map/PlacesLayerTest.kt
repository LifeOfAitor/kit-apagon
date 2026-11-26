package io.github.lifeofaitor.kitapagon.feature.map

import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.HOSPITAL
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.PHARMACY
import org.junit.Assert.assertEquals
import org.junit.Test
import org.maplibre.geojson.Point

class PlacesLayerTest {
    @Test
    fun drawsEachPlaceAtItsLocation() {
        val feature = PlacesLayer.featureCollectionOf(listOf(PHARMACY)).features()!!.single()

        val point = feature.geometry() as Point
        assertEquals(PHARMACY.location.longitude, point.longitude(), 0.0)
        assertEquals(PHARMACY.location.latitude, point.latitude(), 0.0)
    }

    @Test
    fun remembersTheIndexOfEachPlaceSoATapCanFindIt() {
        val features = PlacesLayer.featureCollectionOf(listOf(PHARMACY, HOSPITAL)).features()!!

        assertEquals(listOf(0, 1), features.map { it.getNumberProperty("index").toInt() })
    }

    @Test
    fun usesTheMarkerImageAndMapPriorityOfTheCategory() {
        val feature = PlacesLayer.featureCollectionOf(listOf(HOSPITAL)).features()!!.single()

        assertEquals("kit-apagon-place-health_centre", feature.getStringProperty("image"))
        assertEquals(0, feature.getNumberProperty("priority").toInt())
    }

    @Test
    fun everyCategoryHasItsOwnPriorityAndColour() {
        val appearances = PlaceCategory.entries.map { it.appearance() }

        assertEquals(PlaceCategory.entries.size, appearances.map { it.mapPriority }.toSet().size)
        assertEquals(PlaceCategory.entries.size, appearances.map { it.markerColor }.toSet().size)
        assertEquals(PlaceCategory.entries.size, appearances.map { it.icon }.toSet().size)
    }
}
