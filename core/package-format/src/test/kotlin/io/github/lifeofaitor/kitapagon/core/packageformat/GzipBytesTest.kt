package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GzipBytesTest {
    private val text = MunicipalityListWriter.write(municipalityList()).toByteArray()

    @Test
    fun decompressesWhatItCompressed() {
        assertContentEquals(text, GzipBytes.decompressOrNull(GzipBytes.compress(text), maximumBytes = 1_000_000))
    }

    @Test
    fun theSameTextAlwaysGivesTheSameBytes() {
        assertContentEquals(GzipBytes.compress(text), GzipBytes.compress(text.copyOf()))
    }

    @Test
    fun refusesWhatIsNotGzip() {
        assertNull(GzipBytes.decompressOrNull("<html>Too Many Requests</html>".toByteArray(), maximumBytes = 1_000_000))
        assertNull(GzipBytes.decompressOrNull(GzipBytes.compress(text).copyOf(20), maximumBytes = 1_000_000))
    }

    @Test
    fun refusesAFileThatWouldGrowPastTheLimit() {
        // A few hundred bytes that become ten megabytes.
        val bomb = GzipBytes.compress(ByteArray(10_000_000))

        assertTrue(bomb.size < 20_000)
        assertNull(GzipBytes.decompressOrNull(bomb, maximumBytes = 1_000_000))
    }
}
