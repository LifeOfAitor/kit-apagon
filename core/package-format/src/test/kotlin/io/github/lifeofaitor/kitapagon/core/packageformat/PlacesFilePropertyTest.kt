package io.github.lifeofaitor.kitapagon.core.packageformat

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** places.json comes from outside the app, so these tests feed it thousands of generated inputs. */
class PlacesFilePropertyTest {
    @Test
    fun neverThrowsWhateverTheText() =
        runTest {
            checkAll(ITERATIONS, Arb.string(maxSize = 300)) { text -> PlacesFileParser.parse(text) }
        }

    @Test
    fun neverThrowsForFeatureCollectionsWithArbitraryFeatures() =
        runTest {
            checkAll(ITERATIONS, Arb.string(maxSize = 200)) { featureText ->
                PlacesFileParser.parse("""{"type":"FeatureCollection","features":[$featureText]}""")
            }
        }

    @Test
    fun anyValidListOfPlacesSurvivesBeingWrittenAndReadBack() =
        runTest {
            checkAll(ITERATIONS, Arb.list(validPlace(), 0..20)) { places ->
                val result = PlacesFileParser.parse(PlacesFileWriter.write(places))

                assertEquals(PlacesFileResult.Valid(PlacesFile(places, skippedFeatureCount = 0)), result)
            }
        }

    private fun validPlace() =
        arbitrary {
            Place(
                category = Arb.enum<PlaceCategory>().bind(),
                location =
                    GeographicPoint(
                        latitude = finiteDouble(-90.0..90.0).bind(),
                        longitude = finiteDouble(-180.0..180.0).bind(),
                    ),
                source = Arb.enum<PlaceSource>().bind(),
                sourceId = "node/" + Arb.string(1..12).bind().ifBlank { "1" },
                details =
                    PlaceDetails(
                        name = nonBlankTextOrNull().bind(),
                        address = nonBlankTextOrNull().bind(),
                        openingHours = nonBlankTextOrNull().bind(),
                        phone = nonBlankTextOrNull().bind(),
                    ),
            )
        }

    // NaN and infinities are not valid JSON numbers, so the generator never writes them.
    private fun finiteDouble(range: ClosedFloatingPointRange<Double>) = Arb.double(range, false)

    // The parser treats blank details as missing, so generated details are never blank.
    private fun nonBlankTextOrNull() = arbitrary { Arb.string(1..40).bind().takeIf { it.isNotBlank() } }.orNull()

    private companion object {
        const val ITERATIONS = 2_000
    }
}
