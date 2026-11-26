package io.github.lifeofaitor.kitapagon.core.packageformat

import java.io.File
import java.io.IOException
import java.io.InputStream

/** Reads and validates the header of a PMTiles file without loading the rest of it. */
object PmtilesHeaderReader {
    fun read(file: File): PmtilesHeaderResult {
        val headerBytes =
            readHeaderBytes(file)
                ?: return PmtilesHeaderResult.Invalid(InvalidPmtilesHeaderReason.UNREADABLE_FILE)
        return PmtilesHeaderParser.parse(headerBytes)
    }

    private fun readHeaderBytes(file: File): ByteArray? =
        try {
            file.inputStream().use { input -> input.readUpTo(PmtilesHeaderParser.HEADER_LENGTH) }
        } catch (expectedWhenTheFileCannotBeOpened: IOException) {
            null
        }

    // InputStream.readNBytes needs Android 13, and this module also runs on Android 8 (decision 0002).
    private fun InputStream.readUpTo(maximumLength: Int): ByteArray {
        val buffer = ByteArray(maximumLength)
        var totalRead = 0
        var lastRead = 0
        while (totalRead < maximumLength && lastRead != END_OF_STREAM) {
            lastRead = read(buffer, totalRead, maximumLength - totalRead)
            if (lastRead != END_OF_STREAM) totalRead += lastRead
        }
        return buffer.copyOf(totalRead)
    }

    private const val END_OF_STREAM = -1
}
