package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.BundledFile
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFileKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexBuilderTest {
    @Test
    fun describesEachFileWithItsKindAndItsPlaceInsideTheTar() {
        val map = TarEntry("ES-20045.pmtiles", offset = 512, sizeBytes = 3, sha256 = "a".repeat(64))
        val places = TarEntry("ES-20045.places.json", offset = 1_536, sizeBytes = 2, sha256 = "b".repeat(64))

        val details =
            IndexBuilder.packageDetailsOf(
                municipality(),
                linkedMapOf(PackageFileKind.MAP to map, PackageFileKind.PLACES to places),
                "ES-20.tar",
            )

        assertEquals("ES-20.tar", details.bundle)
        assertEquals(
            listOf(
                BundledFile(PackageFileKind.MAP, map.name, map.offset, map.sizeBytes, map.sha256),
                BundledFile(PackageFileKind.PLACES, places.name, places.offset, places.sizeBytes, places.sha256),
            ),
            details.files,
        )
    }

    @Test
    fun theDetailsListTheMunicipalityWithItsMargin() {
        val irun = municipality()

        val details = IndexBuilder.packageDetailsOf(irun, emptyMap(), "ES-20.tar")

        assertEquals(irun.id, details.id)
        assertEquals(irun.names, details.names)
        assertEquals(irun.packageArea, details.bounds)
        assertEquals(irun.centre, details.centre)
    }

    @Test
    fun namesTheFilesAfterTheirPackageAndThePartAfterItsProvince() {
        assertEquals("ES-20045.pmtiles", IndexBuilder.mapFileNameOf(municipality()))
        assertEquals("ES-20045.places.json", IndexBuilder.placesFileNameOf(municipality()))
        assertEquals("provincia-ES-20.json", IndexBuilder.partFileNameOf("20"))
    }

    @Test
    fun theDataVersionIsARealDate() {
        assertTrue(DataVersion.isValid("2026-12-01"))
        assertFalse(DataVersion.isValid("2026-12"))
        assertFalse(DataVersion.isValid("2026-13-45"))
        assertFalse(DataVersion.isValid("2026-12-01/../x"))
        assertEquals(1_796_083_200L, DataVersion.epochSecondsOf("2026-12-01"))
    }
}
