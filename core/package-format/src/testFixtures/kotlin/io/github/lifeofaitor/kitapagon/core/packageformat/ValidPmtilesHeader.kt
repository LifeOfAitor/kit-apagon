package io.github.lifeofaitor.kitapagon.core.packageformat

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val VERSION_OFFSET = 7
private const val TILE_TYPE_OFFSET = 99
private const val MINIMUM_ZOOM_OFFSET = 100
private const val MAXIMUM_ZOOM_OFFSET = 101
private const val FIRST_BOUND_OFFSET = 102
private const val PMTILES_VERSION: Byte = 3
private const val VECTOR_TILE_TYPE: Byte = 1
private const val DEGREE_SCALE = 10_000_000
private const val DEFAULT_MAXIMUM_ZOOM = 14

val DEFAULT_TEST_BOUNDS = GeographicBounds(west = -1.0, south = 43.0, east = 1.0, north = 44.0)

/** Builds a valid PMTiles version 3 header for vector tiles, with the given zoom range and bounds. */
fun validHeader(
    bounds: GeographicBounds = DEFAULT_TEST_BOUNDS,
    minimumZoom: Int = 0,
    maximumZoom: Int = DEFAULT_MAXIMUM_ZOOM,
): ByteArray {
    val buffer = ByteBuffer.allocate(PmtilesHeaderParser.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put("PMTiles".toByteArray(Charsets.US_ASCII))
    buffer.put(VERSION_OFFSET, PMTILES_VERSION)
    buffer.put(TILE_TYPE_OFFSET, VECTOR_TILE_TYPE)
    buffer.put(MINIMUM_ZOOM_OFFSET, minimumZoom.toByte())
    buffer.put(MAXIMUM_ZOOM_OFFSET, maximumZoom.toByte())
    listOf(bounds.west, bounds.south, bounds.east, bounds.north).forEachIndexed { index, degrees ->
        buffer.putInt(FIRST_BOUND_OFFSET + index * Int.SIZE_BYTES, (degrees * DEGREE_SCALE).toInt())
    }
    return buffer.array()
}
