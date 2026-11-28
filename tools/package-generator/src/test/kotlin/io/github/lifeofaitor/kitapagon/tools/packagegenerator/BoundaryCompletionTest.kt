package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoundaryCompletionTest {
    private val ceutaBounds = GeographicBounds(-5.42346, 35.87086, -5.2783, 35.918)

    @Test
    fun aCompletedBoundaryIsReadLikeAnyMunicipalityAndKeepsWhereItCameFrom() {
        // Casariche, as Overpass returned it after an edit changed its level from 8 to 7.
        val fromOverpass = boundaryLine(ceutaBounds, municipalityTags("41026", "Casariche") + ("admin_level" to "7"))

        val completed = BoundaryCompleter.complete(setOf("41026"), sequenceOf(fromOverpass))

        val municipality = MunicipalityReader.read(completed.asSequence()).single()
        assertEquals("ES-41026", municipality.id.value)
        assertEquals(BoundaryCompletion(originalAdminLevel = "7"), municipality.completion)
        assertEquals(
            "etiquetado con admin_level 7 en OpenStreetMap, conviene corregirlo",
            municipality.completion?.reason,
        )
    }

    @Test
    fun explainsEachKindOfCompletion() {
        assertEquals("contorno incompleto en el extracto", BoundaryCompletion("8").reason)
        assertEquals("sin admin_level en OpenStreetMap, conviene corregirlo", BoundaryCompletion(null).reason)
    }

    @Test
    fun aBoundaryWithoutAnyLevelIsCompletedToo() {
        // Campo lost boundary=administrative and admin_level in an edit.
        val withoutLevel =
            boundaryLine(
                ceutaBounds,
                mapOf(
                    "ine:municipio" to "22074",
                    "name" to "Campo",
                    "type" to "boundary",
                ),
            )

        val municipality =
            MunicipalityReader
                .read(
                    BoundaryCompleter.complete(setOf("22074"), sequenceOf(withoutLevel)).asSequence(),
                ).single()

        assertEquals(BoundaryCompletion(originalAdminLevel = null), municipality.completion)
    }

    @Test
    fun onlyTheMissingMunicipalitiesAreCompleted() {
        val ceuta = boundaryLine(ceutaBounds, municipalityTags("51001", "Ceuta"))
        val irun = boundaryLine(IRUN_BOUNDS, municipalityTags("20045", "Irun"))

        val completed = BoundaryCompleter.complete(setOf("51001"), sequenceOf(ceuta, irun))

        assertEquals(listOf("ES-51001"), MunicipalityReader.read(completed.asSequence()).map { it.id.value })
    }

    @Test
    fun withSeveralRelationsForOneCodeOnlyTheOneOfLevelEightIsUsed() {
        val municipalityOfLevelEight = boundaryLine(ceutaBounds, municipalityTags("51001", "Ceuta"))
        val sameCodeOnAnotherLevel =
            boundaryLine(
                IRUN_BOUNDS,
                municipalityTags("51001", "Ceuta") + ("admin_level" to "4"),
            )

        val completed =
            BoundaryCompleter.complete(
                setOf("51001"),
                sequenceOf(sameCodeOnAnotherLevel, municipalityOfLevelEight),
            )

        assertEquals(ceutaBounds, MunicipalityReader.read(completed.asSequence()).single().bounds)
    }

    @Test
    fun withSeveralRelationsAndNoneOfLevelEightNoneIsUsed() {
        val comarca = boundaryLine(ceutaBounds, municipalityTags("51001", "A") + ("admin_level" to "7"))
        val district = boundaryLine(IRUN_BOUNDS, municipalityTags("51001", "B") + ("admin_level" to "9"))

        assertEquals(emptyList(), BoundaryCompleter.complete(setOf("51001"), sequenceOf(comarca, district)))
    }

    @Test
    fun ignoresWhatIsNotAPolygon() {
        val coastline =
            """{"type":"Feature","geometry":{"type":"LineString","coordinates":[[-5.4,35.9],[-5.3,35.9]]},""" +
                """"properties":{"ine:municipio":"51001","name":"Ceuta"}}"""

        assertEquals(emptyList(), BoundaryCompleter.complete(setOf("51001"), sequenceOf(coastline)))
    }

    @Test
    fun aBoundaryFromTheExtractHasNoCompletion() {
        assertNull(
            MunicipalityReader
                .read(
                    sequenceOf(boundaryLine(IRUN_BOUNDS, municipalityTags("20045", "Irun"))),
                ).single()
                .completion,
        )
    }
}
