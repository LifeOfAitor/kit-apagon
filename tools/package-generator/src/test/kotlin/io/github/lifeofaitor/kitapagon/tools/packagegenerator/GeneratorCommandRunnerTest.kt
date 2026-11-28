package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListParser
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListResult
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFileKind
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileResult
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsParser
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsResult
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
                    listOf(
                        OfficialMunicipality("20045", "Irun", "Gipuzkoa"),
                        OfficialMunicipality("48020", "Bilbao", "Bizkaia"),
                    ),
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

        assertEquals(listOf("ES-20.tar", "provincia-ES-20.json"), output.list().orEmpty().sorted())
        val part =
            assertIs<ProvinceDetailsResult.Valid>(
                ProvinceDetailsParser.parse(File(output, "provincia-ES-20.json").readText()),
            ).details
        assertEquals("Gipuzkoa", part.provinceName)
        val placesEntry =
            part.packages
                .single()
                .files
                .single { it.kind == PackageFileKind.PLACES }
        val placesText =
            File(output, "ES-20.tar").readBytes().copyOfRange(
                placesEntry.offset.toInt(),
                (
                    placesEntry.offset +
                        placesEntry.sizeBytes
                ).toInt(),
            )
        val placesFile = PlacesFileParser.parse(placesText.decodeToString())
        assertEquals(
            listOf("Farmacia"),
            assertIs<PlacesFileResult.Valid>(placesFile).placesFile.places.map { it.details.name },
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
        assertFailsWith<IllegalArgumentException> { runner().run(buildCommand(output, sourceMap, provinces = "20,9")) }
        assertFailsWith<IllegalArgumentException> { runner().run(buildCommand(output, sourceMap, provinces = "20,31")) }
        assertEquals(false, output.exists())
    }

    @Test
    fun onlyTheMunicipalitiesOfTheOfficialListGetAPackage() {
        val output = File(temporaryDirectory, "out")

        runner().run(buildCommand(output, sourceMapCovering(GeographicBounds(-6.0, 42.0, 0.0, 44.0))))

        val packageIds =
            listOf("20", "48").flatMap { province ->
                val part = File(output, "provincia-ES-$province.json").readText()
                assertIs<ProvinceDetailsResult.Valid>(
                    ProvinceDetailsParser.parse(part),
                ).details.packages.map { it.id.value }
            }
        assertEquals(listOf("ES-20045", "ES-48020"), packageIds)
        assertEquals(
            listOf("ES-20.tar", "ES-48.tar", "provincia-ES-20.json", "provincia-ES-48.json"),
            output.list().orEmpty().sorted(),
        )
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

        runner(signingKey = privateKey).run(signCommand(parts, publicKeyFile, published))

        assertEquals(
            listOf(
                "municipios.json.gz",
                "municipios.json.gz.sig",
                "provincia-ES-20.json.gz",
                "provincia-ES-48.json.gz",
            ),
            published.list().orEmpty().sorted(),
        )
        val listBytes = File(published, "municipios.json.gz").readBytes()
        val appKey = IndexSignature.publicKeyFrom(publicKeyFile.readText().trim())
        assertTrue(IndexSignature.isValid(listBytes, File(published, "municipios.json.gz.sig").readBytes(), appKey))
        val listText = GzipBytes.decompressOrNull(listBytes, maximumBytes = 1_000_000)!!.decodeToString()
        val list = assertIs<MunicipalityListResult.Valid>(MunicipalityListParser.parse(listText)).list
        assertEquals(listOf("ES-20045", "ES-48020"), list.municipalities.map { it.id.value })
        assertEquals(listOf("https://example.org/{data_version}-ES-{province}/{file}"), list.addresses.bundles)
        assertTrue(printed.last().startsWith("packages: 2\nprovinces: 2\ndata version: 2026-12-01\nlist: "))
    }

    @Test
    fun refusesAddressesTheAppWouldIgnore() {
        val command =
            signCommand(
                indexPartsOfTwoProvinces(),
                File(temporaryDirectory, "key.txt"),
                File(temporaryDirectory, "published"),
            )
        val privateKey = Base64.getEncoder().encodeToString(indexSigningKeyPair().private.encoded)
        File(
            temporaryDirectory,
            "key.txt",
        ).writeText(Base64.getEncoder().encodeToString(indexSigningKeyPair().public.encoded))

        assertFailsWith<IllegalArgumentException> {
            runner(signingKey = privateKey).run(command.copy(bundleAddress = "http://example.org/{file}"))
        }
        assertFailsWith<IllegalArgumentException> {
            runner(signingKey = privateKey).run(command.copy(detailsAddress = "https://example.org/always-the-same"))
        }
    }

    @Test
    fun publishesNothingWhenTheSecretDoesNotMatchTheKeyOfTheApp() {
        val keys = File(temporaryDirectory, "keys")
        runner().run(GeneratorCommand.NewSigningKey(keys))
        val anotherPrivateKey = Base64.getEncoder().encodeToString(indexSigningKeyPair().private.encoded)
        val published = File(temporaryDirectory, "published")
        val command =
            signCommand(indexPartsOfTwoProvinces(), File(keys, SigningKeys.PUBLIC_KEY_FILE_NAME), published)

        assertFailsWith<IllegalStateException> { runner(signingKey = anotherPrivateKey).run(command) }
        assertEquals(false, published.exists())
    }

    @Test
    fun refusesToSignWithoutTheKey() {
        val command =
            signCommand(temporaryDirectory, File(temporaryDirectory, "key.txt"), temporaryDirectory)

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
            File(output, "provincia-ES-$province.json").copyTo(File(parts, "provincia-ES-$province.json"))
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
        output = output,
    )

    private fun signCommand(
        parts: File,
        publicKey: File,
        output: File,
    ) = GeneratorCommand.SignIndex(
        parts = parts,
        publicKey = publicKey,
        output = output,
        detailsAddress = "https://example.org/{data_version}/{file}",
        bundleAddress = "https://example.org/{data_version}-ES-{province}/{file}",
    )

    private fun sourceMapCovering(bounds: GeographicBounds): File =
        File(temporaryDirectory, "source.pmtiles").apply { writeBytes(validHeader(bounds) + ByteArray(1_024)) }
}
