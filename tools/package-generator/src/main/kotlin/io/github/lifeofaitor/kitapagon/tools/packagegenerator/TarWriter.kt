package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest

/** A file written into a tar: where its content starts and how to check it (decision 0014). */
data class TarEntry(
    val name: String,
    val offset: Long,
    val sizeBytes: Long,
    val sha256: String,
)

/**
 * Writes an uncompressed tar in the ustar format of POSIX.1-1988, which every tar tool reads. Each
 * file is stored in one piece right after its header, so the app can download it alone.
 */
class TarWriter(
    private val output: OutputStream,
    /** Seconds since 1970 stored as the date of every file, so that the same input gives the same tar. */
    private val modificationTimeSeconds: Long,
) : Closeable {
    private var position = 0L

    fun add(file: File): TarEntry {
        output.write(TarHeader.of(file.name, file.length(), modificationTimeSeconds))
        val contentOffset = position + TarHeader.BLOCK_SIZE
        val digest = MessageDigest.getInstance("SHA-256")
        val copiedBytes = copyCounting(file, digest)
        check(copiedBytes == file.length()) { "${file.name} changed while it was being written into the tar." }
        output.write(ByteArray(paddingOf(copiedBytes)))
        position = contentOffset + copiedBytes + paddingOf(copiedBytes)
        return TarEntry(file.name, contentOffset, copiedBytes, digest.digest().toHexString())
    }

    /** Writes the two empty blocks that end a tar and closes the output. */
    override fun close() {
        output.use { it.write(ByteArray(END_OF_ARCHIVE_SIZE)) }
    }

    private fun copyCounting(
        file: File,
        digest: MessageDigest,
    ): Long {
        var copiedBytes = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            generateSequence { input.read(buffer).takeIf { it >= 0 } }.forEach { read ->
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                copiedBytes += read
            }
        }
        return copiedBytes
    }

    companion object {
        private const val COPY_BUFFER_SIZE = 64 * 1024
        private const val END_OF_ARCHIVE_SIZE = 2 * TarHeader.BLOCK_SIZE

        /** Bytes a file of [sizeBytes] takes inside a tar, header and padding included. */
        fun entrySizeOf(sizeBytes: Long): Long = TarHeader.BLOCK_SIZE + sizeBytes + paddingOf(sizeBytes)

        /** Bytes of a tar holding files of these sizes. */
        fun archiveSizeOf(fileSizes: List<Long>): Long = fileSizes.sumOf(::entrySizeOf) + END_OF_ARCHIVE_SIZE

        private fun paddingOf(sizeBytes: Long): Int =
            (
                (TarHeader.BLOCK_SIZE - sizeBytes % TarHeader.BLOCK_SIZE) %
                    TarHeader.BLOCK_SIZE
            ).toInt()
    }
}

/** The 512 byte header of a regular file in a ustar archive. */
object TarHeader {
    const val BLOCK_SIZE = 512

    private const val NAME_LENGTH = 100
    private const val MODE_OFFSET = 100
    private const val OWNER_OFFSET = 108
    private const val GROUP_OFFSET = 116
    private const val SIZE_OFFSET = 124
    private const val MODIFICATION_TIME_OFFSET = 136
    private const val CHECKSUM_OFFSET = 148
    private const val CHECKSUM_LENGTH = 8
    private const val TYPE_OFFSET = 156
    private const val MAGIC_OFFSET = 257
    private const val NUMBER_FIELD_LENGTH = 8
    private const val LONG_NUMBER_FIELD_LENGTH = 12
    private const val CHECKSUM_DIGITS = 6
    private const val READABLE_BY_EVERYONE = "644"
    private const val REGULAR_FILE: Byte = '0'.code.toByte()
    private const val USTAR_MAGIC_AND_VERSION = "ustar\u000000"

    // 11 octal digits, the most a size field holds: just under 8 GiB.
    private const val MAXIMUM_SIZE_BYTES = 8_589_934_591L

    /** Throws IllegalArgumentException for names the tar would cut or tools would read differently. */
    fun of(
        name: String,
        sizeBytes: Long,
        modificationTimeSeconds: Long,
    ): ByteArray {
        require(
            name.isNotEmpty() && name.length <= NAME_LENGTH && name.all { it.code in PRINTABLE_ASCII } && '/' !in name,
        ) {
            "Tar entry names must be up to 100 printable ASCII characters without folders: $name"
        }
        require(sizeBytes in 0..MAXIMUM_SIZE_BYTES) { "A tar entry cannot hold $sizeBytes bytes." }
        val header = ByteArray(BLOCK_SIZE)
        writeText(header, 0, name)
        writeOctal(header, MODE_OFFSET, NUMBER_FIELD_LENGTH, READABLE_BY_EVERYONE.toLong(OCTAL))
        writeOctal(header, OWNER_OFFSET, NUMBER_FIELD_LENGTH, 0)
        writeOctal(header, GROUP_OFFSET, NUMBER_FIELD_LENGTH, 0)
        writeOctal(header, SIZE_OFFSET, LONG_NUMBER_FIELD_LENGTH, sizeBytes)
        writeOctal(header, MODIFICATION_TIME_OFFSET, LONG_NUMBER_FIELD_LENGTH, modificationTimeSeconds)
        header[TYPE_OFFSET] = REGULAR_FILE
        writeText(header, MAGIC_OFFSET, USTAR_MAGIC_AND_VERSION)
        writeChecksum(header)
        return header
    }

    // The checksum is the sum of every byte of the header, counting its own field as spaces.
    private fun writeChecksum(header: ByteArray) {
        writeText(header, CHECKSUM_OFFSET, " ".repeat(CHECKSUM_LENGTH))
        val checksum = header.sumOf { byte -> java.lang.Byte.toUnsignedInt(byte) }
        writeText(header, CHECKSUM_OFFSET, checksum.toString(OCTAL).padStart(CHECKSUM_DIGITS, '0') + "\u0000 ")
    }

    // Numbers are written in octal, padded with zeros and ended with a null byte.
    private fun writeOctal(
        header: ByteArray,
        offset: Int,
        fieldLength: Int,
        value: Long,
    ) = writeText(header, offset, value.toString(OCTAL).padStart(fieldLength - 1, '0') + "\u0000")

    private fun writeText(
        header: ByteArray,
        offset: Int,
        text: String,
    ) = text.toByteArray(Charsets.US_ASCII).copyInto(header, offset)

    private const val OCTAL = 8
    private val PRINTABLE_ASCII = 0x20..0x7E
}
