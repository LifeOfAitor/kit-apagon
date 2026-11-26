package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MunicipalityReaderTest {
    @Test
    fun readsAMunicipalityWithItsCodeNamesAreaAndCentre() {
        val irun =
            boundaryLine(
                GeographicBounds(-1.85, 43.28, -1.73, 43.36),
                municipalityTags(
                    "20045",
                    "Irun",
                    "name:es" to "Irún",
                ),
            )

        val municipality = MunicipalityReader.read(sequenceOf(irun)).single()

        assertEquals("ES-20045", municipality.id.value)
        assertEquals(MunicipalityNames("Irun", listOf("Irún")), municipality.names)
        assertEquals("20", municipality.provinceCode)
        assertEquals(GeographicBounds(-1.85, 43.28, -1.73, 43.36), municipality.bounds)
        assertEquals(43.32, municipality.centre.latitude, absoluteTolerance = 0.000001)
        assertEquals(-1.79, municipality.centre.longitude, absoluteTolerance = 0.000001)
    }

    @Test
    fun collectsAlternativeNamesWithoutRepeatingTheOfficialOne() {
        val tags =
            municipalityTags(
                "20069",
                "Donostia/San Sebastián",
                "name:es" to "San Sebastián",
                "name:eu" to "Donostia",
                "official_name" to "Donostia/San Sebastián",
                "alt_name" to "Donostia; San Sebastián ;Easo",
            )

        val names = MunicipalityReader.read(sequenceOf(boundaryLine(IRUN_BOUNDS, tags))).single().names

        assertEquals(MunicipalityNames("Donostia/San Sebastián", listOf("San Sebastián", "Donostia", "Easo")), names)
    }

    @Test
    fun discardsBoundariesThatAreNotSpanishMunicipalities() {
        val lines =
            sequenceOf(
                boundaryLine(IRUN_BOUNDS, municipalityTags("20045", "Irun")),
                boundaryLine(IRUN_BOUNDS, mapOf("admin_level" to "8", "name" to "Hendaye", "ref:INSEE" to "64260")),
                boundaryLine(IRUN_BOUNDS, municipalityTags("20045", "Irun") + ("admin_level" to "7")),
                boundaryLine(IRUN_BOUNDS, municipalityTags("2004", "Short code")),
                boundaryLine(IRUN_BOUNDS, municipalityTags("20045", "Irun") - "name"),
                """{"type":"Feature","properties":{"admin_level":"8","ine:municipio":"20036","name":"No geometry"}}""",
                """{"type":"Feature","geometry":{"type":"Point","coordinates":[]},""" +
                    """"properties":{"admin_level":"8","ine:municipio":"20036","name":"Empty"}}""",
                "[1,2,3]",
            )

        assertEquals(listOf("ES-20045"), MunicipalityReader.read(lines).map { it.id.value })
    }

    @Test
    fun joinsTheBoundariesOfAMunicipalityWithAnExclave() {
        val mainPart =
            boundaryLine(GeographicBounds(-2.0, 42.8, -1.9, 42.9), municipalityTags("01059", "Vitoria-Gasteiz"))
        val exclave =
            boundaryLine(GeographicBounds(-2.5, 42.6, -2.4, 42.7), municipalityTags("01059", "Vitoria-Gasteiz"))

        val municipality = MunicipalityReader.read(sequenceOf(mainPart, exclave)).single()

        assertEquals(GeographicBounds(-2.5, 42.6, -1.9, 42.9), municipality.bounds)
        assertEquals(GeographicPoint(42.85, -1.95).latitude, municipality.centre.latitude, absoluteTolerance = 0.000001)
    }

    @Test
    fun ordersMunicipalitiesByCodeAndSkipsBlankLines() {
        val lines =
            sequenceOf(
                boundaryLine(IRUN_BOUNDS, municipalityTags("48020", "Bilbao")),
                "",
                "\u001E",
                boundaryLine(IRUN_BOUNDS, municipalityTags("01059", "Vitoria-Gasteiz")),
            )

        assertEquals(listOf("ES-01059", "ES-48020"), MunicipalityReader.read(lines).map { it.id.value })
    }

    @Test
    fun refusesALineThatIsNotJson() {
        assertFailsWith<IllegalArgumentException> { MunicipalityReader.read(sequenceOf("not json")) }
    }

    @Test
    fun thePackageAreaAddsTheMarginToTheMunicipality() {
        assertEquals(IRUN_BOUNDS.expandedBy(3.0), municipality().packageArea)
    }
}
