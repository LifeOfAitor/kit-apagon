package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileResult
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PackageBuilderTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val template = DownloadAddressTemplate("https://example.org/{data_version}-ES-{province}/{file}")
    private val irun = municipality()
    private val hondarribia =
        municipality("ES-20036", "Hondarribia", GeographicBounds(-1.8306, 43.3395, -1.7628, 43.3940))
    private val bilbao = municipality("ES-48020", "Bilbao", GeographicBounds(-2.98, 43.21, -2.88, 43.29))
    private val extractedAreas = mutableListOf<GeographicBounds>()

    // Writes a small fake map instead of calling pmtiles, and remembers what it was asked for.
    private val fakeExtractor =
        MapExtractor { area, output ->
            extractedAreas += area
            output.writeText("map of ${output.name}")
        }

    @Test
    fun putsTheMapAndThePlacesOfEachMunicipalityInTheTarOfItsProvince() {
        val pharmacy = place(43.3381, -1.7886, "Farmacia")

        val built = build(listOf(irun, bilbao), listOf(pharmacy))

        assertEquals(listOf(irun.packageArea, bilbao.packageArea), extractedAreas)
        assertEquals(listOf("ES-20.tar", "ES-48.tar"), built.bundles.map { it.name })
        assertEquals(listOf("ES-20.tar", "ES-48.tar"), temporaryDirectory.list().orEmpty().sorted())
        val irunFiles =
            built.indexPart.packages
                .first { it.id == irun.id }
                .files
        assertEquals("map of ES-20045.pmtiles", String(bytesOf(irunFiles[0])))
        val placesFile = PlacesFileParser.parse(String(bytesOf(irunFiles[1])))
        assertEquals(listOf(pharmacy), assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places)
        assertEquals(listOf("https://example.org/2026-12-01-ES-20/ES-20.tar"), irunFiles.flatMap { it.urls }.distinct())
        assertEquals(mapOf(irun.id to 1, bilbao.id to 0), built.placeCounts)
    }

    @Test
    fun writesAValidPlacesFileEvenForAMunicipalityWithoutPlaces() {
        val built = build(listOf(irun), emptyList(), outputDirectory = File(temporaryDirectory, "not-created-yet"))

        val placesFile =
            PlacesFileParser.parse(
                String(
                    bytesOf(
                        built.indexPart.packages
                            .single()
                            .files[1],
                    ),
                ),
            )
        assertEquals(emptyList(), assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places)
    }

    @Test
    fun splitsAProvinceThatDoesNotFitInOneTar() {
        // Each package takes 2,048 bytes in a tar: two headers and two files padded to 512 bytes.
        val built = build(listOf(hondarribia, irun), emptyList(), maximumBundleBytes = 3_500)

        assertEquals(listOf("ES-20-1.tar", "ES-20-2.tar"), built.bundles.map { it.name })
        val addresses =
            built.indexPart.packages.associate {
                it.id.value to
                    it.files
                        .first()
                        .urls
                        .single()
            }
        assertEquals("https://example.org/2026-12-01-ES-20/ES-20-1.tar", addresses["ES-20036"])
        assertEquals("https://example.org/2026-12-01-ES-20/ES-20-2.tar", addresses["ES-20045"])
    }

    @Test
    fun refusesADataVersionThatIsNotADate() {
        assertFailsWith<IllegalArgumentException> {
            PackageBuilder(fakeExtractor).build(listOf(irun), emptyList(), temporaryDirectory, "december", template)
        }
    }

    @Test
    fun summarisesSizesBundlesAndMunicipalitiesWithoutPlaces() {
        val built = build(listOf(irun, hondarribia), listOf(place(43.30, -1.80, "Farmacia")))

        assertEquals(
            """
            municipalities: 2
            places in packages: 1
            size: 0.0 MB
            largest: ES-20045 Irun 0.0 MB
            without places: 1 [ES-20036]
            bundles: ES-20.tar 0.0 MB
            """.trimIndent(),
            BuiltPackagesSummary.describe(built),
        )
    }

    private fun build(
        municipalities: List<Municipality>,
        places: List<Place>,
        outputDirectory: File = temporaryDirectory,
        maximumBundleBytes: Long = PackageBuilder.MAXIMUM_BUNDLE_BYTES,
    ): BuiltPackages =
        PackageBuilder(
            fakeExtractor,
            maximumBundleBytes,
        ).build(municipalities, places, outputDirectory, "2026-12-01", template)

    // Reads a file of the index from its tar, as the app will with a partial download.
    private fun bytesOf(file: PackageFile): ByteArray {
        val bundle = temporaryDirectory.walk().first { it.name == file.urls.single().substringAfterLast('/') }
        return RandomAccessFile(bundle, "r").use { input ->
            ByteArray(file.sizeBytes.toInt()).also { bytes ->
                input.seek(file.offset)
                input.readFully(bytes)
            }
        }
    }
}
