package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidPmtilesHeaderReason
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderResult
import io.github.lifeofaitor.kitapagon.core.packageformat.validHeader
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MapExtractorTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun asksPmtilesForThePackageAreaUpToZoomFourteen() {
        // A stand-in for pmtiles that writes the arguments it receives into the output file.
        val fakePmtiles = executableScript("echo \"$@\" > \"$3\"")
        val output = File(temporaryDirectory, "ES-20045.pmtiles")

        PmtilesMapExtractor(
            File("spain.pmtiles"),
            fakePmtiles.path,
        ).extract(GeographicBounds(-1.8893, 43.25947, -1.69223, 43.38024), output)

        assertEquals(
            "extract spain.pmtiles ${output.path} --bbox=-1.8893000,43.2594700,-1.6922300,43.3802400 --maxzoom=14",
            output.readText().trim(),
        )
    }

    @Test
    fun failsWithTheMessageOfPmtilesWhenItCannotExtract() {
        val failingPmtiles = executableScript("echo 'archive not found' >&2; exit 1")

        val failure =
            assertFailsWith<IllegalStateException> {
                PmtilesMapExtractor(
                    File("missing.pmtiles"),
                    failingPmtiles.path,
                ).extract(IRUN_BOUNDS, File(temporaryDirectory, "out.pmtiles"))
            }

        assertTrue("archive not found" in failure.message.orEmpty())
    }

    @Test
    fun writesDecimalPointsWhateverTheLanguageOfTheComputer() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        try {
            assertEquals(
                "-1.5000000,43.0000000,-1.2500000,43.1000000",
                PmtilesMapExtractor.boundingBoxOf(GeographicBounds(-1.5, 43.0, -1.25, 43.1)),
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun acceptsASourceMapThatCoversTheRegion() {
        SourceMapCoverage.check(headerOf(GeographicBounds(-2.0, 42.0, 0.0, 44.0)), IRUN_BOUNDS.expandedBy(3.0))
    }

    @Test
    fun acceptsEdgesThatPmtilesRoundedByAFractionOfAMetre() {
        val region = GeographicBounds(-1.8893, 43.25947, -1.69223, 43.38024)
        val roundedInwards = GeographicBounds(-1.8892999, 43.2594701, -1.6922301, 43.3802399)

        SourceMapCoverage.check(headerOf(roundedInwards), region)
    }

    @Test
    fun refusesASourceMapThatEndsBeforeTheMarginOfTheBorder() {
        // The regional archive of docs/investigacion/09 ended at -1.72, the margin of Irun reaches -1.69.
        val failure =
            assertFailsWith<IllegalStateException> {
                SourceMapCoverage.check(
                    headerOf(GeographicBounds(-3.5, 42.4, -1.72, 43.5)),
                    GeographicBounds(-1.8893, 43.25947, -1.69223, 43.38024),
                )
            }

        assertTrue("-1.7200000" in failure.message.orEmpty())
    }

    @Test
    fun refusesASourceMapWithoutTheDetailOfZoomFourteen() {
        assertFailsWith<IllegalStateException> {
            SourceMapCoverage.check(headerOf(GeographicBounds(-2.0, 42.0, 0.0, 44.0), maximumZoom = 12), IRUN_BOUNDS)
        }
    }

    @Test
    fun refusesASourceMapThatIsNotPmtiles() {
        assertFailsWith<IllegalStateException> {
            SourceMapCoverage.check(PmtilesHeaderResult.Invalid(InvalidPmtilesHeaderReason.NOT_PMTILES), IRUN_BOUNDS)
        }
    }

    private fun headerOf(
        bounds: GeographicBounds,
        maximumZoom: Int = 15,
    ): PmtilesHeaderResult = PmtilesHeaderParser.parse(validHeader(bounds, maximumZoom = maximumZoom))

    private fun executableScript(body: String): File =
        File(temporaryDirectory, "fake-pmtiles").apply {
            writeText("#!/bin/sh\n$body\n")
            setExecutable(true)
        }
}
