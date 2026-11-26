package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OsmPlaceClassifierTest {
    @ParameterizedTest(name = "{0} is {1}")
    @MethodSource("taggedPlaces")
    fun classifiesTheTagsOfDecision0011(
        tags: String,
        expectedCategory: PlaceCategory,
    ) {
        assertEquals(expectedCategory, OsmPlaceClassifier.categoryOf(tagsFrom(tags)))
    }

    @Test
    fun aPharmacyIsNeverAHealthCentreEvenThoughItHasAHealthcareTag() {
        val pharmacy = tagsFrom("amenity=pharmacy;healthcare=pharmacy")

        assertEquals(PlaceCategory.PHARMACY, OsmPlaceClassifier.categoryOf(pharmacy))
    }

    @Test
    fun aVeterinaryHospitalIsNeverAHealthCentre() {
        assertNull(OsmPlaceClassifier.categoryOf(tagsFrom("amenity=veterinary;healthcare=hospital")))
    }

    @ParameterizedTest(name = "{0} is discarded")
    @ValueSource(
        strings = [
            "healthcare=physiotherapist",
            "amenity=dentist;healthcare=dentist",
            "healthcare=podiatrist",
            "healthcare=laboratory",
            "healthcare=yes",
            "amenity=shelter;shelter_type=public_transport",
            "barrier=gate",
            "man_made=surveillance",
        ],
    )
    fun discardsPlacesThatDoNotHelpInAnEmergency(tags: String) {
        assertNull(OsmPlaceClassifier.categoryOf(tagsFrom(tags)))
    }

    companion object {
        @JvmStatic
        fun taggedPlaces(): List<Arguments> =
            listOf(
                Arguments.of("amenity=pharmacy", PlaceCategory.PHARMACY),
                Arguments.of("healthcare=pharmacy", PlaceCategory.PHARMACY),
                Arguments.of("amenity=hospital", PlaceCategory.HEALTH_CENTRE),
                Arguments.of("amenity=clinic;healthcare=clinic", PlaceCategory.HEALTH_CENTRE),
                Arguments.of("amenity=doctors;healthcare=doctor", PlaceCategory.HEALTH_CENTRE),
                Arguments.of("healthcare=centre", PlaceCategory.HEALTH_CENTRE),
                Arguments.of("amenity=drinking_water", PlaceCategory.DRINKING_WATER),
                Arguments.of("amenity=water_point", PlaceCategory.DRINKING_WATER),
                Arguments.of("emergency=assembly_point", PlaceCategory.ASSEMBLY_POINT),
                Arguments.of("emergency=defibrillator", PlaceCategory.DEFIBRILLATOR),
                Arguments.of("amenity=police", PlaceCategory.POLICE),
                Arguments.of("amenity=fire_station", PlaceCategory.FIRE_STATION),
            )

        // "key=value;key=value", so the test data reads like OpenStreetMap tags.
        fun tagsFrom(text: String): Map<String, String> =
            text.split(";").associate { tag -> tag.substringBefore("=") to tag.substringAfter("=") }
    }
}
