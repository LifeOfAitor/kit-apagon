package io.github.lifeofaitor.kitapagon.core.packageformat

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The files of the index are published compressed, because GitHub serves them as they are (decision
 * 0017). Java writes no date in the gzip header, so the same text always gives the same bytes.
 */
object GzipBytes {
    private const val BUFFER_SIZE = 8 * 1024

    fun compress(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream()
            .also { output ->
                GZIPOutputStream(output).use { gzip -> gzip.write(bytes) }
            }.toByteArray()

    /**
     * Null when the bytes are not gzip or would grow past [maximumBytes]: a small download must not be
     * able to fill the memory of a modest phone.
     */
    fun decompressOrNull(
        bytes: ByteArray,
        maximumBytes: Int,
    ): ByteArray? =
        try {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { input -> readAtMost(input, maximumBytes) }
        } catch (expectedForDamagedDownloads: IOException) {
            null
        }

    // Stops reading as soon as the limit is passed, before the extra bytes take any memory.
    private fun readAtMost(
        input: InputStream,
        maximumBytes: Int,
    ): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var read = input.read(buffer)
        while (read >= 0 && output.size() + read <= maximumBytes) {
            output.write(buffer, 0, read)
            read = input.read(buffer)
        }
        return if (read < 0) output.toByteArray() else null
    }
}
