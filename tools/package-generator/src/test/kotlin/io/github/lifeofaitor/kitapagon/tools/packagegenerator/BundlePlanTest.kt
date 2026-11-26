package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundlePlanTest {
    // A package of two small files takes 2,048 bytes in a tar; the tar adds 1,024 at the end.
    private val smallPackage = listOf(10L, 10L)

    @Test
    fun keepsEveryPackageInOneTarWhenItFits() {
        assertEquals(listOf(listOf("a", "b", "c")), BundlePlan.split(listOf("a", "b", "c"), 10_000) { smallPackage })
    }

    @Test
    fun startsANewTarWhenTheNextPackageDoesNotFit() {
        val parts = BundlePlan.split(listOf("a", "b", "c"), 5_120) { smallPackage }

        assertEquals(listOf(listOf("a", "b"), listOf("c")), parts)
    }

    @Test
    fun aPackageLargerThanTheLimitGoesAloneInItsTar() {
        val sizes = mapOf("small" to smallPackage, "huge" to listOf(50_000L, 10L), "other" to smallPackage)

        val parts = BundlePlan.split(listOf("small", "huge", "other"), 5_120) { sizes.getValue(it) }

        assertEquals(listOf(listOf("small"), listOf("huge"), listOf("other")), parts)
    }

    @Test
    fun namesTheTarsAfterTheProvince() {
        assertEquals(listOf("ES-20.tar"), BundlePlan.bundleNamesOf("20", 1))
        assertEquals(listOf("ES-09-1.tar", "ES-09-2.tar"), BundlePlan.bundleNamesOf("09", 2))
    }

    @Test
    fun neverLosesReordersOrOverfillsPackages() =
        runTest {
            checkAll(ITERATIONS, Arb.list(Arb.long(0L..40_000L), 0..60), Arb.long(4_000L..200_000L)) { sizes, maximum ->
                val packages = sizes.indices.toList()

                val parts = BundlePlan.split(packages, maximum) { index -> listOf(sizes[index], 1L) }

                assertEquals(packages, parts.flatten())
                parts.forEach { part ->
                    val fileSizes = part.flatMap { index -> listOf(sizes[index], 1L) }
                    assertTrue(part.size == 1 || TarWriter.archiveSizeOf(fileSizes) <= maximum)
                }
            }
        }

    private companion object {
        const val ITERATIONS = 500
    }
}
