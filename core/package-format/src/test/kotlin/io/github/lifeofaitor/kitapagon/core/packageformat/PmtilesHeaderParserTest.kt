package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertEquals

class PmtilesHeaderParserTest {
    @Test
    fun readsTheRealHeaderOfTheIrunPackageAsThePmtilesToolReportsIt() {
        val result = PmtilesHeaderParser.parse(IRUN_HEADER_HEX.hexToByteArray())

        val expected =
            PmtilesHeader(
                minimumZoom = 0,
                maximumZoom = 14,
                bounds = GeographicBounds(west = -1.8893, south = 43.25947, east = -1.69223, north = 43.38024),
            )
        assertEquals(PmtilesHeaderResult.Valid(expected), result)
    }

    @Test
    fun rejectsHeadersShorterThan127Bytes() {
        assertInvalid(InvalidPmtilesHeaderReason.TOO_SHORT, validHeader().copyOf(126))
    }

    @Test
    fun rejectsFilesThatAreNotPmtiles() {
        assertInvalid(InvalidPmtilesHeaderReason.NOT_PMTILES, validHeader().also { it[0] = 'X'.code.toByte() })
    }

    @Test
    fun rejectsOtherPmtilesVersions() {
        assertInvalid(InvalidPmtilesHeaderReason.UNSUPPORTED_VERSION, validHeader().also { it[7] = 2 })
    }

    @Test
    fun rejectsRasterTiles() {
        val pngTileType: Byte = 2
        assertInvalid(InvalidPmtilesHeaderReason.NOT_VECTOR_TILES, validHeader().also { it[99] = pngTileType })
    }

    @Test
    fun rejectsAMinimumZoomAboveTheMaximumZoom() {
        assertInvalid(InvalidPmtilesHeaderReason.INVALID_ZOOM_RANGE, validHeader(minimumZoom = 15, maximumZoom = 14))
    }

    @Test
    fun readsZoomLevelsAsUnsignedNumbers() {
        val result = PmtilesHeaderParser.parse(validHeader(maximumZoom = 200))

        assertEquals(200, (result as PmtilesHeaderResult.Valid).header.maximumZoom)
    }

    @Test
    fun rejectsBoundsWhoseWestEdgeIsEastOfTheEastEdge() {
        assertInvalid(
            InvalidPmtilesHeaderReason.INVALID_BOUNDS,
            validHeader(DEFAULT_TEST_BOUNDS.copy(west = 10.0, east = 5.0)),
        )
    }

    @Test
    fun rejectsLatitudesBeyondThePoles() {
        assertInvalid(InvalidPmtilesHeaderReason.INVALID_BOUNDS, validHeader(DEFAULT_TEST_BOUNDS.copy(north = 91.0)))
    }

    private fun assertInvalid(
        expectedReason: InvalidPmtilesHeaderReason,
        headerBytes: ByteArray,
    ) {
        assertEquals(PmtilesHeaderResult.Invalid(expectedReason), PmtilesHeaderParser.parse(headerBytes))
    }

    private companion object {
        // First 127 bytes of the Irun package (ES-20045) extracted from the Protomaps build of 2026-11-26.
        const val IRUN_HEADER_HEX =
            "504d54696c6573037f00000000000000da0100000000000059020000000000009b04000000000000f406000000000000" +
                "0000000000000000f406000000000000b39b3900000000008500000000000000850000000000000085000000000000" +
                "0001020201000e38b7dffe0cdfc81944c9fdfea04cdb19003ec0eefed615d219"
    }
}
