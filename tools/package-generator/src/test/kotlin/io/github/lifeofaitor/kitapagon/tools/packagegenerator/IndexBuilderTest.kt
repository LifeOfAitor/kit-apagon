package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndex
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexBuilderTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val template =
        DownloadAddressTemplate(
            "https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}-ES-{province}/{file}",
        )

    @Test
    fun describesEachFileWithItsSizeChecksumAndAddress() {
        File(temporaryDirectory, "ES-20045.pmtiles").writeText("abc")
        File(temporaryDirectory, "ES-20045.places.json").writeText("")

        val index = IndexBuilder.build(listOf(municipality()), temporaryDirectory, "2026-12-01", template)

        val releaseAddress = "https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/2026-12-01-ES-20"
        assertEquals(
            listOf(
                PackageFile(
                    "ES-20045.pmtiles",
                    3,
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    listOf("$releaseAddress/ES-20045.pmtiles"),
                ),
                PackageFile(
                    "ES-20045.places.json",
                    0,
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    listOf("$releaseAddress/ES-20045.places.json"),
                ),
            ),
            index.packages.single().files,
        )
    }

    @Test
    fun theIndexListsTheMunicipalityWithItsMargin() {
        File(temporaryDirectory, "ES-20045.pmtiles").writeText("map")
        File(temporaryDirectory, "ES-20045.places.json").writeText("places")
        val irun = municipality()

        val index = IndexBuilder.build(listOf(irun), temporaryDirectory, "2026-12-01", template)

        val indexed = index.packages.single()
        assertEquals(PackageIndex.CURRENT_FORMAT_VERSION, index.formatVersion)
        assertEquals("2026-12-01", index.dataVersion)
        assertEquals(irun.id, indexed.id)
        assertEquals(irun.names, indexed.names)
        assertEquals("20", indexed.provinceCode)
        assertEquals(irun.packageArea, indexed.bounds)
        assertEquals(irun.centre, indexed.centre)
    }

    @Test
    fun failsWhenAFileOfThePackageIsMissing() {
        assertFailsWith<FileNotFoundException> {
            IndexBuilder.build(listOf(municipality()), temporaryDirectory, "2026-12-01", template)
        }
    }

    @Test
    fun theDataVersionIsADate() {
        assertTrue(IndexBuilder.isValidDataVersion("2026-12-01"))
        assertFalse(IndexBuilder.isValidDataVersion("2026-12"))
        assertFalse(IndexBuilder.isValidDataVersion("2026-12-01/../x"))
        assertFailsWith<IllegalArgumentException> {
            IndexBuilder.build(
                emptyList(),
                temporaryDirectory,
                "december",
                template,
            )
        }
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

    @Test
    fun checksumsLargerFilesInPieces() {
        val large =
            File(temporaryDirectory, "large.bin").apply {
                writeBytes(ByteArray(200_000) { index -> (index % 251).toByte() })
            }

        assertEquals(64, IndexBuilder.sha256Of(large).length)
        assertEquals(
            IndexBuilder.sha256Of(large),
            IndexBuilder.sha256Of(large.copyTo(File(temporaryDirectory, "copy.bin"))),
        )
    }
}
