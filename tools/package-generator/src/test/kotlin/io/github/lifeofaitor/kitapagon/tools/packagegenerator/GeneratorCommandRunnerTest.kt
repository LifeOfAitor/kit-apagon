package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexResult
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileResult
import io.github.lifeofaitor.kitapagon.core.packageformat.indexSigningKeyPair
import io.github.lifeofaitor.kitapagon.core.packageformat.validHeader
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeneratorCommandRunnerTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val printed = mutableListOf<String>()
    private val irunBounds = GeographicBounds(-1.85, 43.28, -1.73, 43.36)

    private val boundaries by lazy {
        File(temporaryDirectory, "boundaries.geojsonseq").apply {
            writeText(
                listOf(
                    boundaryLine(irunBounds, municipalityTags("20045", "Irun", "name:es" to "Irún")),
                    boundaryLine(GeographicBounds(-2.98, 43.21, -2.88, 43.29), municipalityTags("48020", "Bilbao")),
                ).joinToString("\n"),
            )
        }
    }

    private val places by lazy {
        File(temporaryDirectory, "places.geojsonseq").apply {
            writeText(
                "\u001E{\"type\":\"Feature\",\"id\":\"n1\"," +
                    "\"geometry\":{\"type\":\"Point\",\"coordinates\":[-1.79,43.34]}," +
                    "\"properties\":{\"amenity\":\"pharmacy\",\"name\":\"Farmacia\"}}\n",
            )
        }
    }

    @Test
    fun printsTheAreaTheSourceMapMustCoverRoundedOutwards() {
        runner().run(GeneratorCommand.RegionArea(boundaries, "20"))

        val expected = irunBounds.expandedBy(3.0)
        val printedArea = printed.single().split(",").map(String::toDouble)
        assertEquals(4, printedArea.size)
        assertTrue(printedArea[0] <= expected.west && expected.west - printedArea[0] < 0.00001)
        assertTrue(printedArea[1] <= expected.south && expected.south - printedArea[1] < 0.00001)
        assertTrue(printedArea[2] >= expected.east && printedArea[2] - expected.east < 0.00001)
        assertTrue(printedArea[3] >= expected.north && printedArea[3] - expected.north < 0.00001)
    }

    @Test
    fun buildsThePackagesOfTheProvincesAskedForAndTheirIndexPart() {
        val output = File(temporaryDirectory, "out")

        runner().run(buildCommand(output, sourceMapCovering(GeographicBounds(-4.0, 42.0, 0.0, 44.0)), provinces = "20"))

        assertEquals(
            listOf("ES-20045.places.json", "ES-20045.pmtiles", "index-part.json"),
            output.list().orEmpty().sorted(),
        )
        val placesFile = PlacesFileParser.parse(File(output, "ES-20045.places.json").readText())
        assertEquals(
            listOf("Farmacia"),
            assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places.map { it.details.name },
        )
        val indexPart =
            assertIs<PackageIndexResult.Valid>(PackageIndexParser.parse(File(output, "index-part.json").readText()))
        assertEquals(
            "https://example.org/2026-12-01-ES-20/ES-20045.pmtiles",
            indexPart.index.packages
                .single()
                .files
                .first()
                .urls
                .single(),
        )
        assertTrue(printed.single().startsWith("municipalities: 1\n"))
    }

    @Test
    fun refusesASourceMapThatDoesNotReachEveryMargin() {
        val tooSmall = sourceMapCovering(GeographicBounds(-1.86, 43.27, -1.72, 43.37))

        assertFailsWith<IllegalStateException> {
            runner().run(buildCommand(File(temporaryDirectory, "out"), tooSmall, provinces = "20"))
        }
    }

    @Test
    fun refusesWrongValuesBeforeDoingAnyWork() {
        val sourceMap = sourceMapCovering(GeographicBounds(-4.0, 42.0, 0.0, 44.0))
        val output = File(temporaryDirectory, "out")

        assertFailsWith<IllegalArgumentException> {
            runner().run(buildCommand(output, sourceMap).copy(dataVersion = "december"))
        }
        assertFailsWith<IllegalArgumentException> {
            runner().run(buildCommand(output, sourceMap).copy(addressTemplate = "http://example.org/{file}"))
        }
        assertFailsWith<IllegalArgumentException> { runner().run(buildCommand(output, sourceMap, provinces = "20,9")) }
        assertFailsWith<IllegalArgumentException> { runner().run(buildCommand(output, sourceMap, provinces = "20,31")) }
        assertEquals(false, output.exists())
    }

    @Test
    fun refusesBoundariesWithoutMunicipalities() {
        val empty = File(temporaryDirectory, "empty.geojsonseq").apply { writeText("") }

        assertFailsWith<IllegalArgumentException> { runner().run(GeneratorCommand.RegionArea(empty, null)) }
    }

    @Test
    fun signsTheJoinedIndexSoThatTheAppAcceptsIt() {
        val keyPair = indexSigningKeyPair()
        val parts = File(temporaryDirectory, "parts").apply { mkdirs() }
        val sourceMap = sourceMapCovering(GeographicBounds(-4.0, 42.0, 0.0, 44.0))
        listOf("20", "48").forEach { province ->
            val output = File(temporaryDirectory, "out-$province")
            runner().run(buildCommand(output, sourceMap, provinces = province))
            File(output, "index-part.json").copyTo(File(parts, "$province.json"))
        }
        File(parts, "notes.txt").writeText("not a part")
        val published = File(temporaryDirectory, "published")

        runner(
            signingKey = Base64.getEncoder().encodeToString(keyPair.private.encoded),
        ).run(GeneratorCommand.SignIndex(parts, published))

        val indexBytes = File(published, "index.json").readBytes()
        assertTrue(IndexSignature.isValid(indexBytes, File(published, "index.json.sig").readBytes(), keyPair.public))
        val index = assertIs<PackageIndexResult.Valid>(PackageIndexParser.parse(indexBytes.decodeToString())).index
        assertEquals(listOf("ES-20045", "ES-48020"), index.packages.map { it.id.value })
        assertEquals("packages: 2\ndata version: 2026-12-01", printed.last())
    }

    @Test
    fun refusesToSignWithoutTheKey() {
        assertFailsWith<IllegalArgumentException> {
            runner(signingKey = null).run(GeneratorCommand.SignIndex(temporaryDirectory, temporaryDirectory))
        }
        assertFailsWith<IllegalArgumentException> {
            runner(signingKey = " ").run(GeneratorCommand.SignIndex(temporaryDirectory, temporaryDirectory))
        }
    }

    @Test
    fun convertsThePlacesOfOnePackageForDevelopment() {
        val input =
            File(temporaryDirectory, "places.geojson").apply {
                writeText("""{"type":"FeatureCollection","features":[]}""")
            }
        val output = File(temporaryDirectory, "places.json")

        runner().run(GeneratorCommand.Places(input, output))

        assertIs<PlacesFileResult.Valid>(PlacesFileParser.parse(output.readText()))
        assertEquals("total: 0", printed.single())
    }

    private fun runner(signingKey: String? = null) =
        GeneratorCommandRunner(
            environment =
                listOfNotNull(
                    signingKey?.let { GeneratorCommand.SignIndex.SIGNING_KEY_VARIABLE to it },
                ).toMap(),
            print = { text -> printed += text },
            mapExtractorFor = { MapExtractor { _, output -> output.writeText("map") } },
        )

    private fun buildCommand(
        output: File,
        sourceMap: File,
        provinces: String? = null,
    ) = GeneratorCommand.BuildPackages(
        boundaries = boundaries,
        places = places,
        sourceMap = sourceMap,
        provinces = provinces,
        dataVersion = "2026-12-01",
        addressTemplate = "https://example.org/{data_version}-ES-{province}/{file}",
        output = output,
    )

    private fun sourceMapCovering(bounds: GeographicBounds): File =
        File(temporaryDirectory, "source.pmtiles").apply { writeBytes(validHeader(bounds) + ByteArray(1_024)) }
}
