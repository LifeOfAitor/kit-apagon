package io.github.lifeofaitor.kitapagon.core.packageformat

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PmtilesHeaderReaderTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun readsOnlyTheHeaderOfALargerFile() {
        val mapFile = File(temporaryDirectory, "map.pmtiles")
        mapFile.writeBytes(validHeader() + ByteArray(4_096))

        assertIs<PmtilesHeaderResult.Valid>(PmtilesHeaderReader.read(mapFile))
    }

    @Test
    fun reportsAFileShorterThanAHeaderAsTooShort() {
        val mapFile = File(temporaryDirectory, "map.pmtiles")
        mapFile.writeBytes(validHeader().copyOf(40))

        assertEquals(
            PmtilesHeaderResult.Invalid(InvalidPmtilesHeaderReason.TOO_SHORT),
            PmtilesHeaderReader.read(mapFile),
        )
    }

    @Test
    fun reportsAMissingFileAsUnreadableInsteadOfThrowing() {
        val missingFile = File(temporaryDirectory, "missing.pmtiles")

        assertEquals(
            PmtilesHeaderResult.Invalid(InvalidPmtilesHeaderReason.UNREADABLE_FILE),
            PmtilesHeaderReader.read(missingFile),
        )
    }
}
