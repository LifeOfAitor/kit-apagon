package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class DuplicatePlacesTest {
    // 0.00001 degrees of latitude are about 1.1 m.
    private val pharmacy = place(43.33810, -1.78860, "Plaza Nueva", PlaceCategory.PHARMACY, "node/12489313956")

    @Test
    fun theSamePlaceMappedTwiceIsKeptOnceAsItFirstAppears() {
        // A real case from Bizkaia: the same pharmacy mapped twice, 8 m apart.
        val samePharmacyAgain = place(43.33817, -1.78860, " plaza nueva", PlaceCategory.PHARMACY, "node/3213350186")

        assertEquals(listOf(pharmacy), DuplicatePlaces.remove(listOf(pharmacy, samePharmacyAgain)))
    }

    @Test
    fun fountainsWithTheSameGenericNameAreDifferentFountains() {
        // "Iturria" means fountain: in the Basque Country there are real ones 37 m to 146 m apart.
        val fountain = place(43.33810, -1.78860, "Iturria", PlaceCategory.DRINKING_WATER, "node/1")
        val anotherFountain = place(43.33827, -1.78860, "Iturria", PlaceCategory.DRINKING_WATER, "node/2")

        assertEquals(listOf(fountain, anotherFountain), DuplicatePlaces.remove(listOf(fountain, anotherFountain)))
    }

    @Test
    fun placesOfDifferentCategoriesAreDifferentPlaces() {
        val healthCentreInside = place(43.33810, -1.78860, "Plaza Nueva", PlaceCategory.HEALTH_CENTRE, "node/4")

        assertEquals(listOf(pharmacy, healthCentreInside), DuplicatePlaces.remove(listOf(pharmacy, healthCentreInside)))
    }

    @Test
    fun unnamedPlacesAreNeverMerged() {
        val fountains =
            listOf(
                place(43.3381, -1.7886, null, PlaceCategory.DRINKING_WATER, "node/5"),
                place(43.3381, -1.7886, null, PlaceCategory.DRINKING_WATER, "node/6"),
                place(43.3381, -1.7886, "  ", PlaceCategory.DRINKING_WATER, "node/7"),
                place(43.3381, -1.7886, "", PlaceCategory.DRINKING_WATER, "node/8"),
            )

        assertEquals(fountains, DuplicatePlaces.remove(fountains))
    }

    @Test
    fun theDistanceAlsoCountsAlongTheLongitude() {
        // At 43 degrees north, 0.0001 degrees of longitude are about 8 m, and 0.0002 about 16 m.
        val close = place(43.33810, -1.78850, "Plaza Nueva", PlaceCategory.PHARMACY, "node/9")
        val far = place(43.33810, -1.78840, "Plaza Nueva", PlaceCategory.PHARMACY, "node/10")

        assertEquals(listOf(pharmacy), DuplicatePlaces.remove(listOf(pharmacy, close)))
        assertEquals(listOf(pharmacy, far), DuplicatePlaces.remove(listOf(pharmacy, far)))
    }
}
