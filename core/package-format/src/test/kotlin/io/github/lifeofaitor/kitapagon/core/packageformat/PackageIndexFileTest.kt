package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageIndexFileTest {
    @Test
    fun anIndexReadsBackExactlyAsItWasWritten() {
        val index = packageIndex(indexedPackage(), indexedPackage(id = "ES-20036", officialName = "Hondarribia"))

        assertEquals(
            PackageIndexResult.Valid(index, skippedPackageCount = 0),
            PackageIndexParser.parse(PackageIndexWriter.write(index)),
        )
    }

    @Test
    fun rejectsTextThatIsNotJson() {
        assertEquals(
            PackageIndexResult.Invalid(InvalidPackageIndexReason.NOT_JSON),
            PackageIndexParser.parse("{\"packages\": ["),
        )
    }

    @Test
    fun rejectsAnErrorPageInsteadOfAnIndex() {
        val errorPage = "<!DOCTYPE html><html><body>Too Many Requests</body></html>"

        assertTrue(PackageIndexParser.parse(errorPage) is PackageIndexResult.Invalid)
    }

    @Test
    fun rejectsJsonThatIsNotAnIndex() {
        assertEquals(
            PackageIndexResult.Invalid(InvalidPackageIndexReason.NOT_AN_INDEX),
            PackageIndexParser.parse("""{"type":"FeatureCollection","features":[]}"""),
        )
    }

    @Test
    fun asksForAnUpdateWhenTheIndexComesFromANewerGenerator() {
        val newer = PackageIndexWriter.write(packageIndex()).replace("\"format_version\":1", "\"format_version\":2")

        assertEquals(
            PackageIndexResult.Invalid(InvalidPackageIndexReason.UNSUPPORTED_FORMAT_VERSION),
            PackageIndexParser.parse(newer),
        )
    }

    @Test
    fun skipsAPackageWithABrokenFileAndKeepsTheOthers() {
        val broken =
            indexedPackage(id = "ES-20036").let {
                it.copy(files = listOf(it.files.single().copy(sha256 = "not-a-checksum")))
            }

        val result = PackageIndexParser.parse(PackageIndexWriter.write(packageIndex(indexedPackage(), broken)))

        val valid = result as PackageIndexResult.Valid
        assertEquals(listOf("ES-20045"), valid.index.packages.map { it.id.value })
        assertEquals(1, valid.skippedPackageCount)
    }

    @Test
    fun skipsAPackageWhoseFileHasNoValidPositionInsideTheTar() {
        val file = indexedPackage().files.single()
        val negative = indexedPackage(id = "ES-20036").copy(files = listOf(file.copy(offset = -1)))
        val withoutOffset = PackageIndexWriter.write(packageIndex(indexedPackage())).replace("\"offset\":512,", "")

        assertEquals(1, skippedPackageCountOf(PackageIndexWriter.write(packageIndex(negative))))
        assertEquals(1, skippedPackageCountOf(withoutOffset))
    }

    @Test
    fun theLastByteToRequestIsTheOffsetPlusTheSizeMinusOne() {
        val file = indexedPackage().files.single().copy(offset = 1_024, sizeBytes = 100)

        assertEquals(1_123, file.lastByte)
    }

    @Test
    fun ignoresDownloadAddressesThatAreNotEncrypted() {
        val file = indexedPackage().files.single()
        val mixed =
            indexedPackage().copy(
                files =
                    listOf(
                        file.copy(
                            urls =
                                listOf("http://mirror.example/x.pmtiles") + file.urls,
                        ),
                    ),
            )

        val result = PackageIndexParser.parse(PackageIndexWriter.write(packageIndex(mixed)))

        val urls =
            (result as PackageIndexResult.Valid)
                .index.packages
                .single()
                .files
                .single()
                .urls
        assertEquals(file.urls, urls)
    }

    @Test
    fun skipsAPackageWhoseOnlyAddressIsNotEncrypted() {
        val file = indexedPackage().files.single()
        val insecure =
            indexedPackage().copy(
                files = listOf(file.copy(urls = listOf("http://mirror.example/x.pmtiles"))),
            )

        val result = PackageIndexParser.parse(PackageIndexWriter.write(packageIndex(insecure)))

        assertEquals(1, (result as PackageIndexResult.Valid).skippedPackageCount)
    }

    @Test
    fun skipsAPackageWithAnInvalidAreaOrIdentifier() {
        val outsideTheWorld = indexedPackage().copy(bounds = GeographicBounds(0.0, 0.0, 200.0, 1.0))
        val written = PackageIndexWriter.write(packageIndex(outsideTheWorld, indexedPackage(id = "ES-20036")))
        val withBadIdentifier = written.replace("\"ES-20036\"", "\"../../etc\"")

        val result = PackageIndexParser.parse(withBadIdentifier)

        assertEquals(
            PackageIndexResult.Valid(packageIndex().copy(packages = emptyList()), skippedPackageCount = 2),
            result,
        )
    }

    private fun skippedPackageCountOf(indexText: String): Int =
        (PackageIndexParser.parse(indexText) as PackageIndexResult.Valid).skippedPackageCount
}
