package io.github.lifeofaitor.kitapagon.core.packageformat

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** index.json is downloaded from the internet, so it is fed thousands of generated inputs. */
class PackageIndexPropertyTest {
    @Test
    fun neverThrowsWhateverTheText() =
        runTest {
            checkAll(ITERATIONS, Arb.string(maxSize = 300)) { text -> PackageIndexParser.parse(text) }
        }

    @Test
    fun neverThrowsForIndexesWithArbitraryPackages() =
        runTest {
            checkAll(ITERATIONS, Arb.string(maxSize = 200)) { packageText ->
                PackageIndexParser.parse(
                    """{"format_version":1,"data_version":"2026-12-01","packages":[$packageText]}""",
                )
            }
        }

    @Test
    fun anyValidIndexSurvivesBeingWrittenAndReadBack() =
        runTest {
            checkAll(ITERATIONS, Arb.list(validPackage(), 0..10)) { packages ->
                val index = PackageIndex(PackageIndex.CURRENT_FORMAT_VERSION, "2026-12-01", packages)

                assertEquals(
                    PackageIndexResult.Valid(index, skippedPackageCount = 0),
                    PackageIndexParser.parse(PackageIndexWriter.write(index)),
                )
            }
        }

    private fun validPackage() =
        arbitrary {
            val code =
                Arb
                    .int(1_000..99_999)
                    .bind()
                    .toString()
                    .padStart(5, '0')
            indexedPackage(
                id = "ES-$code",
                officialName = Arb.string(1..40).bind().ifBlank { "Irun" },
                alternativeNames = Arb.list(Arb.string(0..20), 0..3).bind(),
            )
        }

    private companion object {
        const val ITERATIONS = 1_000
    }
}
