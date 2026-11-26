package io.github.lifeofaitor.kitapagon.core.packageformat

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads the fixed 127 byte header of a PMTiles version 3 archive, as defined in
 * https://github.com/protomaps/PMTiles/blob/main/spec/v3/spec.md. All numbers are little endian.
 */
object PmtilesHeaderParser {
    const val HEADER_LENGTH = 127

    private val MAGIC_NUMBER = "PMTiles".toByteArray(Charsets.US_ASCII)
    private const val SUPPORTED_VERSION = 3
    private const val VECTOR_TILE_TYPE = 1
    private const val VERSION_OFFSET = 7
    private const val TILE_TYPE_OFFSET = 99
    private const val MINIMUM_ZOOM_OFFSET = 100
    private const val MAXIMUM_ZOOM_OFFSET = 101
    private const val WEST_OFFSET = 102
    private const val SOUTH_OFFSET = 106
    private const val EAST_OFFSET = 110
    private const val NORTH_OFFSET = 114

    // Coordinates are stored as integers in units of one ten-millionth of a degree.
    private const val DEGREE_SCALE = 10_000_000.0

    fun parse(headerBytes: ByteArray): PmtilesHeaderResult {
        val structuralProblem = findStructuralProblem(headerBytes)
        return if (structuralProblem != null) {
            PmtilesHeaderResult.Invalid(structuralProblem)
        } else {
            validateContent(readHeader(headerBytes))
        }
    }

    private fun findStructuralProblem(headerBytes: ByteArray): InvalidPmtilesHeaderReason? =
        when {
            headerBytes.size < HEADER_LENGTH -> InvalidPmtilesHeaderReason.TOO_SHORT
            !headerBytes.startsWith(MAGIC_NUMBER) -> InvalidPmtilesHeaderReason.NOT_PMTILES
            headerBytes[VERSION_OFFSET].toInt() != SUPPORTED_VERSION -> InvalidPmtilesHeaderReason.UNSUPPORTED_VERSION
            headerBytes[TILE_TYPE_OFFSET].toInt() != VECTOR_TILE_TYPE -> InvalidPmtilesHeaderReason.NOT_VECTOR_TILES
            else -> null
        }

    private fun readHeader(headerBytes: ByteArray): PmtilesHeader {
        val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        return PmtilesHeader(
            minimumZoom = buffer.unsignedByteAt(MINIMUM_ZOOM_OFFSET),
            maximumZoom = buffer.unsignedByteAt(MAXIMUM_ZOOM_OFFSET),
            bounds =
                GeographicBounds(
                    west = buffer.degreesAt(WEST_OFFSET),
                    south = buffer.degreesAt(SOUTH_OFFSET),
                    east = buffer.degreesAt(EAST_OFFSET),
                    north = buffer.degreesAt(NORTH_OFFSET),
                ),
        )
    }

    private fun validateContent(header: PmtilesHeader): PmtilesHeaderResult =
        when {
            header.minimumZoom > header.maximumZoom -> {
                PmtilesHeaderResult.Invalid(InvalidPmtilesHeaderReason.INVALID_ZOOM_RANGE)
            }

            !header.bounds.isValid -> {
                PmtilesHeaderResult.Invalid(InvalidPmtilesHeaderReason.INVALID_BOUNDS)
            }

            else -> {
                PmtilesHeaderResult.Valid(header)
            }
        }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean = copyOfRange(0, prefix.size).contentEquals(prefix)

    private fun ByteBuffer.unsignedByteAt(offset: Int): Int = java.lang.Byte.toUnsignedInt(get(offset))

    private fun ByteBuffer.degreesAt(offset: Int): Double = getInt(offset) / DEGREE_SCALE
}
