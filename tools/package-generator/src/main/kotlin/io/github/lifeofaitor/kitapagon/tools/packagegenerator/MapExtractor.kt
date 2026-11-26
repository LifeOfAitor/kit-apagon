package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderResult
import java.io.File
import java.util.Locale

/** Cuts the map of one package area out of a larger PMTiles archive. */
fun interface MapExtractor {
    fun extract(
        area: GeographicBounds,
        output: File,
    )
}

/**
 * Uses the `pmtiles` command line tool (BSD-3-Clause). Extracting from a local archive takes a few
 * hundredths of a second per municipality (docs/investigacion/09-paquetes-de-toda-espana.md).
 */
class PmtilesMapExtractor(
    private val sourceArchive: File,
    private val executable: String = DEFAULT_EXECUTABLE,
) : MapExtractor {
    override fun extract(
        area: GeographicBounds,
        output: File,
    ) {
        val process =
            ProcessBuilder(
                executable,
                "extract",
                sourceArchive.path,
                output.path,
                "--bbox=${boundingBoxOf(area)}",
                "--maxzoom=$MAXIMUM_ZOOM",
            ).redirectErrorStream(true).start()
        val toolOutput = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "pmtiles could not extract ${output.name}: $toolOutput" }
    }

    companion object {
        // Decision 0006: tiles up to zoom 14, enlarged by MapLibre beyond that.
        const val MAXIMUM_ZOOM = 14
        private const val DEFAULT_EXECUTABLE = "pmtiles"

        // The root locale keeps the decimal point; the Spanish one would write a comma.
        fun boundingBoxOf(area: GeographicBounds): String =
            listOf(
                area.west,
                area.south,
                area.east,
                area.north,
            ).joinToString(",") { degrees -> "%.7f".format(Locale.ROOT, degrees) }
    }
}

/**
 * The archive the packages are cut from must reach every package area, margins included: a regional
 * archive that ended before the margin of Irun left it without 14 of its 133 tiles
 * (docs/investigacion/09-paquetes-de-toda-espana.md).
 */
object SourceMapCoverage {
    /** Throws IllegalStateException when the archive cannot provide every package of [region]. */
    fun check(
        sourceHeader: PmtilesHeaderResult,
        region: GeographicBounds,
    ) {
        val header = (sourceHeader as? PmtilesHeaderResult.Valid)?.header
        checkNotNull(header) { "The source map is not a usable PMTiles archive: $sourceHeader" }
        check(header.maximumZoom >= PmtilesMapExtractor.MAXIMUM_ZOOM) {
            "The source map stops at zoom ${header.maximumZoom}, the packages need ${PmtilesMapExtractor.MAXIMUM_ZOOM}."
        }
        check(covers(header.bounds, region)) {
            "The source map covers ${PmtilesMapExtractor.boundingBoxOf(header.bounds)}, " +
                "the packages need ${PmtilesMapExtractor.boundingBoxOf(region)}."
        }
    }

    // PMTiles stores bounds as whole multiples of 0.0000001 degrees, so an edge can move by one of them.
    private const val ROUNDING_TOLERANCE_DEGREES = 0.000001

    private fun covers(
        archiveBounds: GeographicBounds,
        region: GeographicBounds,
    ): Boolean =
        region.west >= archiveBounds.west - ROUNDING_TOLERANCE_DEGREES &&
            region.south >= archiveBounds.south - ROUNDING_TOLERANCE_DEGREES &&
            region.east <= archiveBounds.east + ROUNDING_TOLERANCE_DEGREES &&
            region.north <= archiveBounds.north + ROUNDING_TOLERANCE_DEGREES
}
