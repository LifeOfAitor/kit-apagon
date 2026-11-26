package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexBuilderTest {
    private val template =
        DownloadAddressTemplate(
            "https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}-ES-{province}/{file}",
        )

    @Test
    fun describesEachFileWithItsPlaceInsideTheTar() {
        val entries =
            listOf(
                TarEntry("ES-20045.pmtiles", offset = 512, sizeBytes = 3, sha256 = "a".repeat(64)),
                TarEntry("ES-20045.places.json", offset = 1_536, sizeBytes = 2, sha256 = "b".repeat(64)),
            )
        val address = template.addressOf("2026-12-01", "20", "ES-20.tar")

        val indexed = IndexBuilder.indexedPackageOf(municipality(), entries, address)

        val tarAddress = "https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/2026-12-01-ES-20/ES-20.tar"
        assertEquals(
            listOf(
                PackageFile(
                    "ES-20045.pmtiles",
                    offset = 512,
                    sizeBytes = 3,
                    sha256 = "a".repeat(64),
                    urls = listOf(tarAddress),
                ),
                PackageFile(
                    "ES-20045.places.json",
                    offset = 1_536,
                    sizeBytes = 2,
                    sha256 = "b".repeat(64),
                    urls = listOf(tarAddress),
                ),
            ),
            indexed.files,
        )
    }

    @Test
    fun theIndexListsTheMunicipalityWithItsMargin() {
        val irun = municipality()

        val indexed = IndexBuilder.indexedPackageOf(irun, emptyList(), "https://example.org/ES-20.tar")

        assertEquals(irun.id, indexed.id)
        assertEquals(irun.names, indexed.names)
        assertEquals("20", indexed.provinceCode)
        assertEquals(irun.packageArea, indexed.bounds)
        assertEquals(irun.centre, indexed.centre)
    }

    @Test
    fun theDataVersionIsARealDate() {
        assertTrue(DataVersion.isValid("2026-12-01"))
        assertFalse(DataVersion.isValid("2026-12"))
        assertFalse(DataVersion.isValid("2026-13-45"))
        assertFalse(DataVersion.isValid("2026-12-01/../x"))
        assertEquals(1_796_083_200L, DataVersion.epochSecondsOf("2026-12-01"))
    }

    @Test
    fun refusesAddressesTheAppWouldIgnore() {
        assertFailsWith<IllegalArgumentException> { DownloadAddressTemplate("http://example.org/{file}") }
        assertFailsWith<IllegalArgumentException> {
            DownloadAddressTemplate(
                "https://example.org/always-the-same-file",
            )
        }
    }
}
