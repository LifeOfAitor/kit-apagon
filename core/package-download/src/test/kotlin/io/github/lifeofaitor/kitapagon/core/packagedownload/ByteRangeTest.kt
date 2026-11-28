package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ByteRangeTest {
    @Test
    fun writesTheRangeHeaderWithBothEndsIncluded() {
        val range = ByteRange(512, 3_501_530)

        assertEquals("bytes=512-3501530", range.rangeHeader)
        assertEquals(3_501_019, range.length)
    }

    @Test
    fun acceptsOnlyAContentRangeWithExactlyTheBytesAskedFor() {
        val range = ByteRange(512, 1_023)

        assertTrue(range.isDescribedBy("bytes 512-1023/359406592"))
        assertTrue(range.isDescribedBy("bytes 512-1023/*"))
        assertFalse(range.isDescribedBy(null))
        assertFalse(range.isDescribedBy("bytes 0-1023/359406592"))
        assertFalse(range.isDescribedBy("bytes 512-1024/359406592"))
        assertFalse(range.isDescribedBy("bytes 512-1023/1000"))
        assertFalse(range.isDescribedBy("bytes */359406592"))
        assertFalse(range.isDescribedBy("items 512-1023/359406592"))
    }

    @Test
    fun refusesRangesThatAreNotBytes() {
        assertFailsWith<IllegalArgumentException> { ByteRange(10, 9) }
        assertFailsWith<IllegalArgumentException> { ByteRange(-1, 9) }
    }

    @Test
    fun anyRangeIsDescribedByItsOwnContentRangeAndByNoOther() =
        runTest {
            checkAll(
                ITERATIONS,
                Arb.long(0L..1_000_000_000_000L),
                Arb.long(0L..1_000_000L),
                Arb.long(1L..1_000L),
            ) { first, extra, shift ->
                val range = ByteRange(first, first + extra)
                val total = range.last + 1

                assertTrue(range.isDescribedBy("bytes ${range.first}-${range.last}/$total"))
                assertFalse(range.isDescribedBy("bytes ${range.first + shift}-${range.last}/$total"))
                assertFalse(range.isDescribedBy("bytes ${range.first}-${range.last + shift}/${total + shift}"))
            }
        }

    private companion object {
        const val ITERATIONS = 2_000
    }
}
