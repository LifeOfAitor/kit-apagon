package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.BundledFile
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFileKind
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

    private val provinceNames = mapOf("20" to "Gipuzkoa", "48" to "Bizkaia")
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
        val irunDetails = packageOf(built, "ES-20045")
        assertEquals("ES-20.tar", irunDetails.bundle)
        assertEquals("map of ES-20045.pmtiles", String(bytesOf(irunDetails, PackageFileKind.MAP)))
        val placesFile = PlacesFileParser.parse(String(bytesOf(irunDetails, PackageFileKind.PLACES)))
        assertEquals(listOf(pharmacy), assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places)
        assertEquals(mapOf(irun.id to 1, bilbao.id to 0), built.placeCounts)
    }

    @Test
    fun describesEachProvinceWithItsNameAndItsPackagesInOrder() {
        val built = build(listOf(irun, bilbao, hondarribia), emptyList())

        assertEquals(
            listOf("20" to "Gipuzkoa", "48" to "Bizkaia"),
            built.provinces.map {
                it.provinceCode to
                    it.provinceName
            },
        )
        assertEquals(
            listOf("ES-20036", "ES-20045"),
            built.provinces
                .first()
                .packages
                .map { it.id.value },
        )
        assertEquals(
            listOf(MunicipalityList.CURRENT_FORMAT_VERSION),
            built.provinces.map { it.formatVersion }.distinct(),
        )
        assertEquals(listOf("2026-12-01"), built.provinces.map { it.dataVersion }.distinct())
    }

    @Test
    fun writesAValidPlacesFileEvenForAMunicipalityWithoutPlaces() {
        val built = build(listOf(irun), emptyList(), outputDirectory = File(temporaryDirectory, "not-created-yet"))

        val placesFile = PlacesFileParser.parse(String(bytesOf(packageOf(built, "ES-20045"), PackageFileKind.PLACES)))
        assertEquals(emptyList(), assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places)
    }

    @Test
    fun splitsAProvinceThatDoesNotFitInOneTar() {
        // Each package takes 2,048 bytes in a tar: two headers and two files padded to 512 bytes.
        val built = build(listOf(hondarribia, irun), emptyList(), maximumBundleBytes = 3_500)

        assertEquals(listOf("ES-20-1.tar", "ES-20-2.tar"), built.bundles.map { it.name })
        assertEquals("ES-20-1.tar", packageOf(built, "ES-20036").bundle)
        assertEquals("ES-20-2.tar", packageOf(built, "ES-20045").bundle)
    }

    @Test
    fun refusesADataVersionThatIsNotADateOrAProvinceWithoutName() {
        assertFailsWith<IllegalArgumentException> {
            PackageBuilder(
                fakeExtractor,
            ).build(listOf(irun), emptyList(), temporaryDirectory, "december", provinceNames)
        }
        assertFailsWith<IllegalArgumentException> {
            PackageBuilder(fakeExtractor).build(
                listOf(irun),
                emptyList(),
                temporaryDirectory,
                "2026-12-01",
                mapOf("48" to "Bizkaia"),
            )
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
        ).build(municipalities, places, outputDirectory, "2026-12-01", provinceNames)

    private fun packageOf(
        built: BuiltPackages,
        id: String,
    ): PackageDetails = built.provinces.flatMap { it.packages }.first { it.id.value == id }

    // Reads a file of the package from its tar, as the app will with a partial download.
    private fun bytesOf(
        packageDetails: PackageDetails,
        kind: PackageFileKind,
    ): ByteArray {
        val file: BundledFile = packageDetails.files.single { it.kind == kind }
        val bundle = temporaryDirectory.walk().first { it.name == packageDetails.bundle }
        return RandomAccessFile(bundle, "r").use { input ->
            ByteArray(file.sizeBytes.toInt()).also { bytes ->
                input.seek(file.offset)
                input.readFully(bytes)
            }
        }
    }
}
