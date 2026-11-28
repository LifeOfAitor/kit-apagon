package io.github.lifeofaitor.kitapagon.core.packageformat

import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MunicipalitySearchTest {
    private val list =
        municipalityList(
            provinces =
                listOf(
                    provinceEntry("20", "Gipuzkoa"),
                    provinceEntry("05", "Ávila"),
                    provinceEntry("48", "Bizkaia"),
                    provinceEntry("31", "Navarra"),
                ),
            municipalities =
                listOf(
                    municipalityEntry("ES-20045", "Irun", listOf("Irún")),
                    municipalityEntry("ES-20069", "Donostia/San Sebastián", listOf("Donostia", "San Sebastián")),
                    municipalityEntry("ES-05190", "San Juan de la Nava", emptyList()),
                    municipalityEntry("ES-48020", "Bilbao", emptyList()),
                    municipalityEntry("ES-31025", "Arguedas", emptyList()),
                    municipalityEntry("ES-31013", "Ibargoiti", emptyList()),
                ),
        )
    private val search = MunicipalitySearch(list)

    @Test
    fun ignoresAccentsCapitalsAndPunctuation() {
        assertEquals(listOf("ES-20069"), idsFor("SAN SEBASTIAN"))
        assertEquals(listOf("ES-20069"), idsFor("donostia san"))
        assertEquals(listOf("ES-20045"), idsFor("irún"))
    }

    @Test
    fun showsTheProvinceOfEachMunicipality() {
        assertEquals(
            listOf(MunicipalityMatch(list.municipalities[3], "Bizkaia")),
            search.search("bilbao"),
        )
    }

    @Test
    fun putsTheWholeNameFirstThenTheStartOfANameOfAWordAndAnyPart() {
        val ids = idsFor("ar")

        assertEquals(listOf("ES-31025", "ES-31013"), ids)
        assertEquals(listOf("ES-20069", "ES-05190"), idsFor("san"))
        assertEquals(listOf("ES-48020"), idsFor("Bilbao"))
    }

    @Test
    fun aSearchWithoutLettersFindsNothing() {
        assertEquals(emptyList(), search.search(""))
        assertEquals(emptyList(), search.search(" -/ "))
    }

    @Test
    fun givesAtMostTheRequestedNumberOfResults() {
        assertEquals(1, search.search("a", maximumResults = 1).size)
    }

    @Test
    fun comparesTextsWithoutAccentsOrPunctuation() {
        assertEquals("donostia san sebastian", MunicipalitySearch.comparable("  Donostia/San Sebastián "))
        assertEquals("a coruna", MunicipalitySearch.comparable("A Coruña"))
        assertEquals("l hospitalet de llobregat", MunicipalitySearch.comparable("L'Hospitalet de Llobregat"))
    }

    @Test
    fun anythingTypedGivesResultsThatContainIt() =
        runTest {
            checkAll(2_000, Arb.string(maxSize = 12)) { typed ->
                val comparableQuery = MunicipalitySearch.comparable(typed)
                search.search(typed).forEach { match ->
                    val names = listOf(match.municipality.names.official) + match.municipality.names.alternatives
                    assertTrue(names.any { MunicipalitySearch.comparable(it).contains(comparableQuery) })
                }
            }
        }

    private fun idsFor(query: String): List<String> = search.search(query).map { it.municipality.id.value }
}
