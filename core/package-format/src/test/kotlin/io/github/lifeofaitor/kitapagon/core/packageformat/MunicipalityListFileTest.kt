package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MunicipalityListFileTest {
    @Test
    fun aListReadsBackExactlyAsItWasWritten() {
        val list =
            municipalityList(
                provinces = listOf(provinceEntry(), provinceEntry("48", "Bizkaia")),
                municipalities = listOf(municipalityEntry(), municipalityEntry("ES-48020", "Bilbao", emptyList())),
            )

        assertEquals(
            MunicipalityListResult.Valid(list, skippedEntryCount = 0),
            MunicipalityListParser.parse(MunicipalityListWriter.write(list)),
        )
    }

    @Test
    fun rejectsTextThatIsNotJsonOrNotAList() {
        assertEquals(
            MunicipalityListResult.Invalid(InvalidIndexReason.NOT_JSON),
            MunicipalityListParser.parse("{\"municipalities\": ["),
        )
        assertEquals(
            MunicipalityListResult.Invalid(InvalidIndexReason.NOT_AN_INDEX),
            MunicipalityListParser.parse("""{"type":"FeatureCollection","features":[]}"""),
        )
        assertIs<MunicipalityListResult.Invalid>(
            MunicipalityListParser.parse("<!DOCTYPE html><html><body>Too Many Requests</body></html>"),
        )
    }

    @Test
    fun asksForAnUpdateWhenTheListComesFromANewerGenerator() {
        val newer =
            MunicipalityListWriter
                .write(
                    municipalityList(),
                ).replace("\"format_version\":2", "\"format_version\":3")

        assertEquals(
            MunicipalityListResult.Invalid(InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION),
            MunicipalityListParser.parse(newer),
        )
    }

    @Test
    fun aListWithoutAnyEncryptedAddressCannotBeUsed() {
        val insecure =
            municipalityList().copy(
                addresses = AddressTemplates(listOf("http://example.org/{file}"), TEST_ADDRESSES.bundles),
            )

        assertEquals(
            MunicipalityListResult.Invalid(InvalidIndexReason.NOT_AN_INDEX),
            MunicipalityListParser.parse(MunicipalityListWriter.write(insecure)),
        )
    }

    @Test
    fun ignoresAddressesThatAreNotEncryptedAndKeepsTheOthers() {
        val mixed =
            municipalityList().copy(
                addresses =
                    TEST_ADDRESSES.copy(
                        bundles =
                            listOf("http://mirror.example/{file}") + TEST_ADDRESSES.bundles,
                    ),
            )

        val list =
            (
                MunicipalityListParser.parse(
                    MunicipalityListWriter.write(mixed),
                ) as MunicipalityListResult.Valid
            ).list

        assertEquals(TEST_ADDRESSES, list.addresses)
    }

    @Test
    fun skipsBrokenEntriesAndKeepsTheRest() {
        val brokenProvince =
            provinceEntry(
                "48",
                "Bizkaia",
            ).let { it.copy(details = it.details.copy(sha256 = "not-a-hash")) }
        val unsafeDetailsName =
            provinceEntry("01", "Araba/Álava").let {
                it.copy(details = it.details.copy(name = "../../etc/passwd"))
            }
        val written =
            MunicipalityListWriter
                .write(
                    municipalityList(
                        listOf(provinceEntry(), brokenProvince, unsafeDetailsName),
                        listOf(municipalityEntry(), municipalityEntry("ES-20036")),
                    ),
                ).replace("\"ES-20036\"", "\"../x\"")

        val valid = MunicipalityListParser.parse(written) as MunicipalityListResult.Valid

        assertEquals(listOf("20"), valid.list.provinces.map { it.code })
        assertEquals(listOf("ES-20045"), valid.list.municipalities.map { it.id.value })
        assertEquals(3, valid.skippedEntryCount)
    }

    @Test
    fun resolvesAPackageWithTheAddressesOfItsTar() {
        val details = packageDetails()

        val resolved = municipalityList().resolve("20", details)

        assertEquals(listOf(PackageFileKind.MAP, PackageFileKind.PLACES), resolved.files.map { it.kind })
        assertEquals(
            listOf("https://example.org/kit-apagon-datos/2026-12-01-ES-20/ES-20.tar"),
            resolved.files
                .flatMap {
                    it.urls
                }.distinct(),
        )
        assertEquals(details.files.map { it.offset }, resolved.files.map { it.offset })
        assertEquals(512L + 3_501_019 - 1, resolved.files.first().lastByte)
    }

    @Test
    fun theDetailsOfAProvinceAreAddressedByTheDateOfTheData() {
        assertEquals(
            listOf("https://example.org/kit-apagon-datos/2026-12-01/provincia-ES-20.json.gz"),
            TEST_ADDRESSES.detailsAddressesOf(TEST_DATA_VERSION, MunicipalityList.detailsFileNameOf("20")),
        )
        assertEquals(provinceEntry(), municipalityList().provinceOf(municipalityEntry()))
    }
}
