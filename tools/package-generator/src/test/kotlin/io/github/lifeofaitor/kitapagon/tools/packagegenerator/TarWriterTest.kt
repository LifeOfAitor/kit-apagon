package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TarWriterTest {
    @TempDir
    lateinit var temporaryDirectory: File

    // 2026-12-01 at midnight UTC.
    private val modificationTime = 1_796_083_200L

    @Test
    fun theTarToolOfTheSystemListsAndExtractsEveryFile() {
        val map = file("ES-20045.pmtiles", ByteArray(1_300) { (it % 251).toByte() })
        val places = file("ES-20045.places.json", """{"type":"FeatureCollection","features":[]}""".toByteArray())
        val tar = File(temporaryDirectory, "ES-20.tar")

        TarWriter(tar.outputStream(), modificationTime).use { writer ->
            writer.add(map)
            writer.add(places)
        }

        assertEquals(
            listOf("ES-20045.pmtiles", "ES-20045.places.json"),
            systemTar("-tf", tar.path).lines().filter {
                it.isNotBlank()
            },
        )
        val extracted = File(temporaryDirectory, "extracted").apply { mkdirs() }
        systemTar("-xf", tar.path, "-C", extracted.path)
        assertContentEquals(map.readBytes(), File(extracted, map.name).readBytes())
        assertContentEquals(places.readBytes(), File(extracted, places.name).readBytes())
    }

    @Test
    fun eachEntryPointsToTheExactBytesOfItsFileInsideTheTar() {
        val files =
            listOf(
                file("a.pmtiles", ByteArray(513) { 7 }),
                file("b.json", ByteArray(0)),
                file("c.json", "abc".toByteArray()),
            )
        val tar = File(temporaryDirectory, "bundle.tar")

        val entries = TarWriter(tar.outputStream(), modificationTime).use { writer -> files.map(writer::add) }

        files.zip(entries).forEach { (original, entry) ->
            assertEquals(original.name, entry.name)
            assertEquals(original.length(), entry.sizeBytes)
            assertContentEquals(original.readBytes(), bytesOf(tar, entry.offset, entry.sizeBytes))
            assertEquals(sha256(original.readBytes()), entry.sha256)
        }
        assertEquals(listOf(512L, 512L + 1_024 + 512, 512L + 1_024 + 512 + 512), entries.map { it.offset })
    }

    @Test
    fun predictsTheSizeOfTheTarItWrites() {
        val files = listOf(file("a", ByteArray(1)), file("b", ByteArray(512)), file("c", ByteArray(1_000)))
        val tar = File(temporaryDirectory, "bundle.tar")

        TarWriter(tar.outputStream(), modificationTime).use { writer -> files.forEach(writer::add) }

        assertEquals(TarWriter.archiveSizeOf(files.map(File::length)), tar.length())
        assertEquals(0, tar.length() % 512)
    }

    @Test
    fun theSameFilesGiveTheSameTar() {
        val map = file("ES-20045.pmtiles", ByteArray(2_000) { (it % 13).toByte() })
        val first = File(temporaryDirectory, "first.tar")
        val second = File(temporaryDirectory, "second.tar")

        listOf(first, second).forEach { tar -> TarWriter(tar.outputStream(), modificationTime).use { it.add(map) } }

        assertContentEquals(first.readBytes(), second.readBytes())
    }

    @Test
    fun theHeaderFollowsTheUstarFormat() {
        val header = TarHeader.of("ES-20045.pmtiles", 1_300, modificationTime)

        assertEquals(512, header.size)
        assertEquals("ES-20045.pmtiles", String(header, 0, 16, Charsets.US_ASCII))
        assertEquals("00000002424", String(header, 124, 11, Charsets.US_ASCII))
        assertEquals("ustar", String(header, 257, 5, Charsets.US_ASCII))
        val storedChecksum = String(header, 148, 6, Charsets.US_ASCII).toInt(8)
        val recomputed =
            header
                .copyOf()
                .apply {
                    fill(
                        ' '.code.toByte(),
                        148,
                        156,
                    )
                }.sumOf { java.lang.Byte.toUnsignedInt(it) }
        assertEquals(recomputed, storedChecksum)
    }

    @Test
    fun refusesNamesThatTheTarWouldCutOrPlaceInFolders() {
        assertFailsWith<IllegalArgumentException> { TarHeader.of("a".repeat(101), 1, modificationTime) }
        assertFailsWith<IllegalArgumentException> { TarHeader.of("../ES-20045.pmtiles", 1, modificationTime) }
        assertFailsWith<IllegalArgumentException> { TarHeader.of("Irún.json", 1, modificationTime) }
        assertFailsWith<IllegalArgumentException> { TarHeader.of("", 1, modificationTime) }
        assertFailsWith<IllegalArgumentException> { TarHeader.of("too-big", 8_589_934_592L, modificationTime) }
    }

    private fun file(
        name: String,
        content: ByteArray,
    ): File = File(temporaryDirectory, name).apply { writeBytes(content) }

    private fun bytesOf(
        tar: File,
        offset: Long,
        length: Long,
    ): ByteArray =
        RandomAccessFile(tar, "r").use { input ->
            ByteArray(length.toInt()).also { bytes ->
                input.seek(offset)
                input.readFully(bytes)
            }
        }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

    private fun systemTar(vararg arguments: String): String {
        val process = ProcessBuilder(listOf("tar") + arguments).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), output)
        return output
    }
}
