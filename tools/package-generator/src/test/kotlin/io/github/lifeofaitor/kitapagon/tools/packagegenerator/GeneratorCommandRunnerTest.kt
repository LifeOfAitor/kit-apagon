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
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
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
                    // Lands shared by two municipalities, tagged like one in OpenStreetMap, with a code
                    // the INE does not have.
                    boundaryLine(
                        GeographicBounds(-5.70, 42.32, -5.62, 42.36),
                        municipalityTags("53053", "Comunidad de Soto de la Vega y Villazala"),
                    ),
                ).joinToString("\n"),
            )
        }
    }

    private val official by lazy {
        File(temporaryDirectory, "municipios.csv").apply {
            writeText(
                OfficialMunicipalitiesFile.write(
                    listOf(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("48020", "Bilbao")),
                ),
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
    fun printsEveryProvinceWithMunicipalitiesOncePerLine() {
        runner().run(GeneratorCommand.Provinces(boundaries, official))

        assertEquals(listOf("20", "48"), printed)
    }

    @Test
    fun printsTheAreaTheSourceMapMustCoverRoundedOutwards() {
        runner().run(GeneratorCommand.RegionArea(boundaries, official, "20"))

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

        assertEquals(listOf("ES-20.tar", "index-part.json"), output.list().orEmpty().sorted())
        val indexPart =
            assertIs<PackageIndexResult.Valid>(PackageIndexParser.parse(File(output, "index-part.json").readText()))
        val placesEntry =
            indexPart.index.packages
                .single()
                .files
                .single { it.name == "ES-20045.places.json" }
        val placesText =
            File(output, "ES-20.tar").readBytes().copyOfRange(
                placesEntry.offset.toInt(),
                placesEntry.lastByte.toInt() + 1,
            )
        val placesFile = PlacesFileParser.parse(placesText.decodeToString())
        assertEquals(
            listOf("Farmacia"),
            assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places.map { it.details.name },
        )
        assertEquals(listOf("https://example.org/2026-12-01-ES-20/ES-20.tar"), placesEntry.urls)
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
    fun onlyTheMunicipalitiesOfTheOfficialListGetAPackage() {
        val output = File(temporaryDirectory, "out")

        runner().run(buildCommand(output, sourceMapCovering(GeographicBounds(-6.0, 42.0, 0.0, 44.0))))

        val indexPart =
            assertIs<PackageIndexResult.Valid>(PackageIndexParser.parse(File(output, "index-part.json").readText()))
        assertEquals(listOf("ES-20045", "ES-48020"), indexPart.index.packages.map { it.id.value })
        assertEquals(listOf("ES-20.tar", "ES-48.tar", "index-part.json"), output.list().orEmpty().sorted())
        assertFailsWith<IllegalArgumentException> {
            runner().run(
                GeneratorCommand.RegionArea(boundaries, official, "53"),
            )
        }
    }

    @Test
    fun refusesBoundariesWithoutMunicipalities() {
        val empty = File(temporaryDirectory, "empty.geojsonseq").apply { writeText("") }

        assertFailsWith<IllegalArgumentException> { runner().run(GeneratorCommand.RegionArea(empty, official, null)) }
    }

    @Test
    fun signsTheJoinedIndexWithANewKeySoThatTheAppAcceptsIt() {
        val keys = File(temporaryDirectory, "keys")
        runner().run(GeneratorCommand.NewSigningKey(keys))
        val parts = indexPartsOfTwoProvinces()
        val published = File(temporaryDirectory, "published")
        val privateKey = File(keys, SigningKeys.PRIVATE_KEY_FILE_NAME).readText()
        val publicKeyFile = File(keys, SigningKeys.PUBLIC_KEY_FILE_NAME)

        runner(signingKey = privateKey).run(GeneratorCommand.SignIndex(parts, publicKeyFile, published))

        val indexBytes = File(published, "index.json").readBytes()
        val appKey = IndexSignature.publicKeyFrom(publicKeyFile.readText().trim())
        assertTrue(IndexSignature.isValid(indexBytes, File(published, "index.json.sig").readBytes(), appKey))
        val index = assertIs<PackageIndexResult.Valid>(PackageIndexParser.parse(indexBytes.decodeToString())).index
        assertEquals(listOf("ES-20045", "ES-48020"), index.packages.map { it.id.value })
        assertEquals("packages: 2\ndata version: 2026-12-01", printed.last())
    }

    @Test
    fun publishesNothingWhenTheSecretDoesNotMatchTheKeyOfTheApp() {
        val keys = File(temporaryDirectory, "keys")
        runner().run(GeneratorCommand.NewSigningKey(keys))
        val anotherPrivateKey = Base64.getEncoder().encodeToString(indexSigningKeyPair().private.encoded)
        val published = File(temporaryDirectory, "published")
        val command =
            GeneratorCommand.SignIndex(
                indexPartsOfTwoProvinces(),
                File(keys, SigningKeys.PUBLIC_KEY_FILE_NAME),
                published,
            )

        assertFailsWith<IllegalStateException> { runner(signingKey = anotherPrivateKey).run(command) }
        assertEquals(false, published.exists())
    }

    @Test
    fun refusesToSignWithoutTheKey() {
        val command =
            GeneratorCommand.SignIndex(
                temporaryDirectory,
                File(temporaryDirectory, "key.txt"),
                temporaryDirectory,
            )

        assertFailsWith<IllegalArgumentException> { runner(signingKey = null).run(command) }
        assertFailsWith<IllegalArgumentException> { runner(signingKey = " ").run(command) }
    }

    @Test
    fun createsTheKeyPairOnceAndOnlyTheOwnerCanReadThePrivateKey() {
        val keys = File(temporaryDirectory, "keys")

        runner().run(GeneratorCommand.NewSigningKey(keys))

        val permissions = Files.getPosixFilePermissions(File(keys, SigningKeys.PRIVATE_KEY_FILE_NAME).toPath())
        assertEquals(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions)
        assertFailsWith<IllegalStateException> { runner().run(GeneratorCommand.NewSigningKey(keys)) }
    }

    private fun indexPartsOfTwoProvinces(): File {
        val parts = File(temporaryDirectory, "parts").apply { mkdirs() }
        val sourceMap = sourceMapCovering(GeographicBounds(-4.0, 42.0, 0.0, 44.0))
        listOf("20", "48").forEach { province ->
            val output = File(temporaryDirectory, "out-$province")
            runner().run(buildCommand(output, sourceMap, provinces = province))
            File(output, "index-part.json").copyTo(File(parts, "$province.json"))
        }
        File(parts, "notes.txt").writeText("not a part")
        return parts
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
        official = official,
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
