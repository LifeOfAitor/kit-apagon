package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PublicationCommandsTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val printed = mutableListOf<String>()
    private val runner = GeneratorCommandRunner(environment = emptyMap(), print = { text -> printed += text })

    private val boundaries by lazy {
        File(temporaryDirectory, "boundaries.geojsonseq").apply {
            writeText(boundaryLine(GeographicBounds(-1.85, 43.28, -1.73, 43.36), municipalityTags("20045", "Irun")))
        }
    }

    @Test
    fun writesTheReportOfTheCheckAndAcceptsAProvinceWithEveryMunicipality() {
        val official = officialFile(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("48020", "Bilbao"))
        val report = File(temporaryDirectory, "informe.txt")

        runner.run(GeneratorCommand.CheckMunicipalities(boundaries, official, "20", report))

        assertEquals("official: 1\nmissing: 0\nunexpected: 0", printed.single())
        assertTrue(report.readText().startsWith("Municipios de la relación oficial del INE: 1\n"))
    }

    @Test
    fun stopsThePublicationButKeepsTheReportWhenTooManyAreMissing() {
        val official = officialFile(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("20036", "Hondarribia"))
        val report = File(temporaryDirectory, "informe.txt")

        assertFailsWith<IllegalStateException> {
            runner.run(GeneratorCommand.CheckMunicipalities(boundaries, official, null, report))
        }
        assertTrue("- 20036 Hondarribia" in report.readText())
    }

    @Test
    fun printsTheReleasesToDeleteOnePerLine() {
        val releases =
            File(temporaryDirectory, "releases.json").apply {
                writeText(
                    """[{"tagName":"2026-11-02","isDraft":false,"isPrerelease":false},""" +
                        """{"tagName":"2026-11-02-ES-20","isDraft":false,"isPrerelease":true},""" +
                        """{"tagName":"2026-12-02","isDraft":false,"isPrerelease":false},""" +
                        """{"tagName":"2027-01-02-ES-20","isDraft":false,"isPrerelease":true}]""",
                )
            }

        runner.run(GeneratorCommand.ReleaseLeftovers(releases, "2027-01-02"))
        runner.run(GeneratorCommand.ObsoleteReleases(releases, keep = 1))

        assertEquals(listOf("2027-01-02-ES-20", "2026-11-02", "2026-11-02-ES-20", "2027-01-02-ES-20"), printed)
        assertFailsWith<IllegalArgumentException> { runner.run(GeneratorCommand.ReleaseLeftovers(releases, "enero")) }
    }

    @Test
    fun convertsTheSpreadsheetOfTheIneIntoTheFileOfTheRepository() {
        val notASpreadsheet = File(temporaryDirectory, "26codmun.xlsx").apply { writeText("not a zip") }

        assertFailsWith<java.util.zip.ZipException> {
            runner.run(
                GeneratorCommand.OfficialMunicipalities(notASpreadsheet, File(temporaryDirectory, "municipios.csv")),
            )
        }
    }

    @Test
    fun printsTheOfficialMunicipalitiesWithoutABoundaryAndCompletesThemFromOverpass() {
        val official = officialFile(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("51001", "Ceuta"))
        val ceutaBounds = GeographicBounds(-5.42346, 35.87086, -5.2783, 35.918)
        val overpass =
            File(temporaryDirectory, "overpass.geojsonseq").apply {
                writeText(boundaryLine(ceutaBounds, municipalityTags("51001", "Ceuta")) + "\n")
            }
        val completed = File(temporaryDirectory, "completed.geojsonseq")

        runner.run(GeneratorCommand.MissingBoundaries(boundaries, official))
        runner.run(GeneratorCommand.CompleteBoundaries(boundaries, official, overpass, completed))

        assertEquals(listOf("51001", "completed: 1 of 1"), printed)
        val ceuta = MunicipalityReader.read(completed.readLines().asSequence()).single()
        assertEquals(BoundaryCompletion(originalAdminLevel = "8"), ceuta.completion)
    }

    @Test
    fun aRegionalExtractOnlyLooksForTheMissingMunicipalitiesOfItsProvinces() {
        val official = officialFile(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("51001", "Ceuta"))

        runner.run(GeneratorCommand.MissingBoundaries(boundaries, official, provinces = "20"))

        assertEquals(emptyList(), printed)
    }

    private fun officialFile(vararg municipalities: OfficialMunicipality): File =
        File(temporaryDirectory, "municipios.csv").apply {
            writeText(OfficialMunicipalitiesFile.write(municipalities.toList()))
        }
}
