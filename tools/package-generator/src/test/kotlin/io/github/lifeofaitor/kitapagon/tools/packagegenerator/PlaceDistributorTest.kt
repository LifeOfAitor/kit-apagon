package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceDistributorTest {
    private val irun = municipality()
    private val hondarribia =
        municipality("ES-20036", "Hondarribia", GeographicBounds(-1.8306, 43.3395, -1.7628, 43.3940))

    @Test
    fun aPlaceNearTheBorderBelongsToBothMunicipalities() {
        val nearTheBorder = place(43.345, -1.79)

        val distributed = PlaceDistributor.distribute(listOf(nearTheBorder), listOf(irun, hondarribia))

        assertEquals(listOf(nearTheBorder), distributed.getValue(irun.id))
        assertEquals(listOf(nearTheBorder), distributed.getValue(hondarribia.id))
    }

    @Test
    fun aPlaceInsideTheMarginButOutsideTheMunicipalityIsIncluded() {
        val inHendaye = place(43.36, -1.76)

        assertEquals(listOf(inHendaye), PlaceDistributor.distribute(listOf(inHendaye), listOf(irun)).getValue(irun.id))
    }

    @Test
    fun everyMunicipalityGetsAListEvenWithoutPlaces() {
        val inMadrid = place(40.4168, -3.7038)

        val distributed = PlaceDistributor.distribute(listOf(inMadrid), listOf(irun, hondarribia))

        assertEquals(mapOf(irun.id to emptyList(), hondarribia.id to emptyList()), distributed)
    }

    @Test
    fun keepsTheOrderOfThePlacesAndRemovesDuplicates() {
        val pharmacy = place(43.33810, -1.78860, "Farmacia Larrea", sourceId = "node/1")
        val samePharmacyAgain = place(43.33814, -1.78862, "farmacia larrea", sourceId = "node/2")
        val fountain = place(43.30, -1.80)

        val distributed = PlaceDistributor.distribute(listOf(pharmacy, fountain, samePharmacyAgain), listOf(irun))

        assertEquals(listOf(pharmacy, fountain), distributed.getValue(irun.id))
    }

    @Test
    fun findsTheSameMunicipalitiesAsComparingEveryPlaceWithEveryMunicipality() =
        runTest {
            checkAll(ITERATIONS, Arb.list(municipalities, 1..5), Arb.list(places, 0..30)) { municipalities, places ->
                val uniqueMunicipalities = municipalities.distinctBy { it.id }

                val distributed = PlaceDistributor.distribute(places, uniqueMunicipalities)

                uniqueMunicipalities.forEach { municipality ->
                    val expected =
                        DuplicatePlaces.remove(
                            places.filter { municipality.packageArea.contains(it.location) },
                        )
                    assertEquals(expected, distributed.getValue(municipality.id))
                }
            }
        }

    private companion object {
        const val ITERATIONS = 500

        // Around the Bidasoa, including negative and positive longitudes and cell edges.
        val municipalities =
            arbitrary {
                val west = Arb.double(-2.0, 0.5, false).bind()
                val south = Arb.double(42.0, 44.0, false).bind()
                val width = Arb.double(0.001, 0.4, false).bind()
                val height = Arb.double(0.001, 0.4, false).bind()
                val code = (it.random.nextInt(1, 99_999)).toString().padStart(5, '0')
                municipality(
                    "ES-$code",
                    "Municipality $code",
                    GeographicBounds(
                        west,
                        south,
                        west + width,
                        south + height,
                    ),
                )
            }

        val places =
            arbitrary {
                place(latitude = Arb.double(41.9, 44.5, false).bind(), longitude = Arb.double(-2.2, 1.0, false).bind())
            }
    }
}
