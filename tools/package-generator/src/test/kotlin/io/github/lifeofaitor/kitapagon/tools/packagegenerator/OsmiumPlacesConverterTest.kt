package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OsmiumPlacesConverterTest {
    @Test
    fun convertsARealPharmacyFromIrunWithItsAddress() {
        val pharmacy =
            feature(
                id = "n479837125",
                geometry = """{"type":"Point","coordinates":[-1.7983931,43.3697216]}""",
                properties =
                    """{"addr:city":"Hondarribia","addr:housenumber":"25","addr:street":"Donostia kalea",""" +
                        """"amenity":"pharmacy","healthcare":"pharmacy","name":"Aroia Larrea"}""",
            )

        val places = OsmiumPlacesConverter.convert(collectionOf(pharmacy))

        val expected =
            Place(
                category = PlaceCategory.PHARMACY,
                location = GeographicPoint(latitude = 43.369722, longitude = -1.798393),
                source = PlaceSource.OPENSTREETMAP,
                sourceId = "node/479837125",
                details = PlaceDetails(name = "Aroia Larrea", address = "Donostia kalea 25"),
            )
        assertEquals(listOf(expected), places)
    }

    @Test
    fun reducesAnOutlineToItsCentreAndKeepsTheAreaIdentifier() {
        val clinic =
            feature(
                id = "a76491606",
                geometry = """{"type":"MultiPolygon","coordinates":[[[[0,0],[2,0],[2,2],[0,2],[0,0]]]]}""",
                properties = """{"amenity":"clinic","healthcare":"clinic","name":"Lartzabalgo Anbulatorioa"}""",
            )

        val place = OsmiumPlacesConverter.convert(collectionOf(clinic)).single()

        assertEquals(GeographicPoint(1.0, 1.0), place.location)
        assertEquals("area/76491606", place.sourceId)
    }

    @Test
    fun discardsNoiseAndFeaturesWithoutAUsableIdentifier() {
        val gate = feature("n1", POINT, """{"barrier":"gate"}""")
        val withoutIdentifier = """{"type":"Feature","geometry":$POINT,"properties":{"amenity":"police"}}"""
        val unknownIdentifierType = feature("x5", POINT, """{"amenity":"police"}""")

        val places = OsmiumPlacesConverter.convert(collectionOf(gate, withoutIdentifier, unknownIdentifierType))

        assertEquals(emptyList(), places)
    }

    @Test
    fun usesTheContactPhoneWhenThereIsNoPhoneAndKeepsOpeningHoursAsWritten() {
        val station =
            feature(
                "n2",
                POINT,
                """{"amenity":"fire_station","contact:phone":"+34 943 000 001","opening_hours":"24/7"}""",
            )

        val details = OsmiumPlacesConverter.convert(collectionOf(station)).single().details

        assertEquals(PlaceDetails(phone = "+34 943 000 001", openingHours = "24/7"), details)
    }

    @Test
    fun ordersPlacesByCategoryAndThenByIdentifier() {
        val water = feature("n9", POINT, """{"amenity":"drinking_water"}""")
        val secondPharmacy = feature("n8", POINT, """{"amenity":"pharmacy"}""")
        val firstPharmacy = feature("n10", POINT, """{"amenity":"pharmacy"}""")

        val sourceIds =
            OsmiumPlacesConverter
                .convert(
                    collectionOf(water, secondPharmacy, firstPharmacy),
                ).map { it.sourceId }

        assertEquals(listOf("node/10", "node/8", "node/9"), sourceIds)
    }

    @Test
    fun refusesInputThatIsNotAFeatureCollection() {
        assertFailsWith<IllegalArgumentException> { OsmiumPlacesConverter.convert("""{"type":"Feature"}""") }
        assertFailsWith<IllegalArgumentException> { OsmiumPlacesConverter.convert("not json") }
    }

    private fun collectionOf(vararg features: String) =
        """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

    private fun feature(
        id: String,
        geometry: String,
        properties: String,
    ) = """{"type":"Feature","id":"$id","geometry":$geometry,"properties":$properties}"""

    private companion object {
        const val POINT = """{"type":"Point","coordinates":[-1.79,43.34]}"""
    }
}
