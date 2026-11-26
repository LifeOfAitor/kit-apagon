package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertEquals

class PlacesFileTest {
    @Test
    fun readsAPlaceWithAllItsDetails() {
        val result = PlacesFileParser.parse(collectionOf(feature(PHARMACY_PROPERTIES)))

        val expected =
            Place(
                category = PlaceCategory.PHARMACY,
                location = GeographicPoint(latitude = 43.3381, longitude = -1.7896),
                source = PlaceSource.OPENSTREETMAP,
                sourceId = "node/1",
                details =
                    PlaceDetails(
                        name = "Farmacia Centro",
                        address = "Kale Nagusia 1",
                        openingHours = "Mo-Fr 09:00-14:00",
                        phone = "+34 943 000 000",
                    ),
            )
        assertEquals(PlacesFileResult.Valid(PlacesFile(listOf(expected), skippedFeatureCount = 0)), result)
    }

    @Test
    fun rejectsTextThatIsNotJson() {
        assertEquals(PlacesFileResult.Invalid(InvalidPlacesFileReason.NOT_JSON), PlacesFileParser.parse("{not json"))
    }

    @Test
    fun rejectsJsonThatIsNotAFeatureCollection() {
        assertEquals(
            PlacesFileResult.Invalid(InvalidPlacesFileReason.NOT_A_FEATURE_COLLECTION),
            PlacesFileParser.parse("""{"type":"Feature"}"""),
        )
    }

    @Test
    fun skipsCategoriesThisVersionDoesNotKnowAndKeepsTheRest() {
        val unknownCategory = PHARMACY_PROPERTIES.replace("pharmacy", "emergency_shelter_from_the_future")

        val result = PlacesFileParser.parse(collectionOf(feature(unknownCategory), feature(PHARMACY_PROPERTIES)))

        val placesFile = (result as PlacesFileResult.Valid).placesFile
        assertEquals(1, placesFile.places.size)
        assertEquals(1, placesFile.skippedFeatureCount)
    }

    @Test
    fun skipsFeaturesWithMissingSourceOrIdentifier() {
        val withoutSource = """{"category":"pharmacy","source_id":"node/1"}"""
        val withBlankIdentifier = """{"category":"pharmacy","source":"openstreetmap","source_id":"  "}"""

        val result = PlacesFileParser.parse(collectionOf(feature(withoutSource), feature(withBlankIdentifier)))

        assertEquals(PlacesFileResult.Valid(PlacesFile(emptyList(), skippedFeatureCount = 2)), result)
    }

    @Test
    fun skipsGeometriesThatAreNotValidPoints() {
        val line = """{"type":"LineString","coordinates":[[0,0],[1,1]]}"""
        val outsideTheWorld = """{"type":"Point","coordinates":[200,43]}"""
        val coordinatesAsText = """{"type":"Point","coordinates":["-1.78","43.33"]}"""

        val result =
            PlacesFileParser.parse(
                collectionOf(
                    feature(PHARMACY_PROPERTIES, line),
                    feature(PHARMACY_PROPERTIES, outsideTheWorld),
                    feature(PHARMACY_PROPERTIES, coordinatesAsText),
                ),
            )

        assertEquals(PlacesFileResult.Valid(PlacesFile(emptyList(), skippedFeatureCount = 3)), result)
    }

    @Test
    fun treatsBlankOptionalDetailsAsMissing() {
        val blankName = """{"category":"drinking_water","source":"openstreetmap","source_id":"node/2","name":" "}"""

        val result = PlacesFileParser.parse(collectionOf(feature(blankName)))

        val place = (result as PlacesFileResult.Valid).placesFile.places.single()
        assertEquals(PlaceDetails(), place.details)
    }

    @Test
    fun writesAFileThatReadsBackToTheSamePlaces() {
        val places =
            listOf(
                Place(PlaceCategory.DEFIBRILLATOR, GeographicPoint(43.3, -1.8), PlaceSource.OFFICIAL, "irun/aed/1"),
                Place(
                    PlaceCategory.ASSEMBLY_POINT,
                    GeographicPoint(-33.5, 151.2),
                    PlaceSource.OPENSTREETMAP,
                    "way/9",
                    PlaceDetails(name = "Plaza de San Juan \"Harria\""),
                ),
            )

        val result = PlacesFileParser.parse(PlacesFileWriter.write(places))

        assertEquals(PlacesFileResult.Valid(PlacesFile(places, skippedFeatureCount = 0)), result)
    }

    private fun collectionOf(vararg features: String): String =
        """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

    private fun feature(
        properties: String,
        geometry: String = IRUN_CENTRE_POINT,
    ): String = """{"type":"Feature","geometry":$geometry,"properties":$properties}"""

    private companion object {
        const val IRUN_CENTRE_POINT = """{"type":"Point","coordinates":[-1.7896,43.3381]}"""
        const val PHARMACY_PROPERTIES =
            """{"category":"pharmacy","source":"openstreetmap","source_id":"node/1","name":"Farmacia Centro",""" +
                """"address":"Kale Nagusia 1","opening_hours":"Mo-Fr 09:00-14:00","phone":"+34 943 000 000"}"""
    }
}
