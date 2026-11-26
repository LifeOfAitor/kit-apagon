package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileResult
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PackageBuilderTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val template = DownloadAddressTemplate("https://example.org/{data_version}/{file}")
    private val irun = municipality()
    private val hondarribia =
        municipality("ES-20036", "Hondarribia", GeographicBounds(-1.8306, 43.3395, -1.7628, 43.3940))
    private val extractedAreas = mutableListOf<GeographicBounds>()

    // Writes a small fake map instead of calling pmtiles, and remembers what it was asked for.
    private val fakeExtractor =
        MapExtractor { area, output ->
            extractedAreas += area
            output.writeText("map of ${output.name}")
        }

    @Test
    fun writesTheMapAndThePlacesOfEachMunicipality() {
        val pharmacy = place(43.3381, -1.7886, "Farmacia")

        val built =
            PackageBuilder(
                fakeExtractor,
            ).build(listOf(irun), listOf(pharmacy), temporaryDirectory, "2026-12-01", template)

        assertEquals(listOf(irun.packageArea), extractedAreas)
        assertEquals("map of ES-20045.pmtiles", File(temporaryDirectory, "ES-20045.pmtiles").readText())
        val placesFile = PlacesFileParser.parse(File(temporaryDirectory, "ES-20045.places.json").readText())
        assertEquals(listOf(pharmacy), assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places)
        assertEquals(mapOf(irun.id to 1), built.placeCounts)
        assertEquals(
            listOf("ES-20045.pmtiles", "ES-20045.places.json"),
            built.indexPart.packages.single().files.map {
                it.name
            },
        )
    }

    @Test
    fun writesAValidPlacesFileEvenForAMunicipalityWithoutPlaces() {
        val outputDirectory = File(temporaryDirectory, "not-created-yet")

        val built =
            PackageBuilder(
                fakeExtractor,
            ).build(listOf(irun), emptyList(), outputDirectory, "2026-12-01", template)

        val placesFile = PlacesFileParser.parse(File(outputDirectory, "ES-20045.places.json").readText())
        assertEquals(emptyList(), assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places)
        assertEquals(mapOf(irun.id to 0), built.placeCounts)
    }

    @Test
    fun summarisesSizesAndMunicipalitiesWithoutPlaces() {
        val pharmacy = place(43.30, -1.80, "Farmacia")

        val built =
            PackageBuilder(
                fakeExtractor,
            ).build(listOf(irun, hondarribia), listOf(pharmacy), temporaryDirectory, "2026-12-01", template)

        assertEquals(
            """
            municipalities: 2
            places in packages: 1
            size: 0.0 MB
            largest: ES-20045 Irun 0.0 MB
            without places: 1 [ES-20036]
            """.trimIndent(),
            BuiltPackagesSummary.describe(built),
        )
    }
}
