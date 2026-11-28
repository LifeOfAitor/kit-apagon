package io.github.lifeofaitor.kitapagon.core.packagedownload

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PackageFileDownloaderTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val server = MockWebServer().apply { start() }
    private val mirror = MockWebServer().apply { start() }
    private val bundle = TestBundle()
    private val downloader = PackageFileDownloader(HttpDownloads())
    private val partFile by lazy { File(temporaryDirectory, "map.part") }

    @AfterEach
    fun stopServers() {
        server.close()
        mirror.close()
    }

    @Test
    fun resumesFromWhatAlreadyArrived() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        partFile.writeBytes(bundle.map.copyOf(100_000))
        val map = bundle.files(server.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Complete, downloader.download(map, partFile, CountingTransfer()))
        assertContentEquals(bundle.map, partFile.readBytes())
        assertEquals("bytes=100512-300511", server.takeRequest().headers["Range"])
    }

    @Test
    fun continuesFromTheNextMirrorWhereTheFirstOneFailed() {
        server.enqueue(MockResponse.Builder().code(500).build())
        mirror.dispatcher = BundleDispatcher(bundle.bytes)
        val map = bundle.files(server.url("/ES-20.tar").toString(), mirror.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Complete, downloader.download(map, partFile, CountingTransfer()))
        assertContentEquals(bundle.map, partFile.readBytes())
    }

    @Test
    fun aDamagedFileIsDeletedAndDownloadedAgainFromTheNextMirror() {
        server.dispatcher = BundleDispatcher(bundle.bytes.copyOf().also { it[1_000] = (it[1_000] + 1).toByte() })
        mirror.dispatcher = BundleDispatcher(bundle.bytes)
        val map = bundle.files(server.url("/ES-20.tar").toString(), mirror.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Complete, downloader.download(map, partFile, CountingTransfer()))
        assertContentEquals(bundle.map, partFile.readBytes())
        assertEquals("bytes=512-300511", mirror.takeRequest().headers["Range"])
    }

    @Test
    fun aDamagedFileOnEveryMirrorIsNotKept() {
        server.dispatcher = BundleDispatcher(bundle.bytes.copyOf().also { it[1_000] = (it[1_000] + 1).toByte() })
        val map = bundle.files(server.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Failed(DownloadFailure.DAMAGED), downloader.download(map, partFile, CountingTransfer()))
        assertFalse(partFile.exists())
    }

    @Test
    fun aCompleteFileIsOnlyChecked() {
        partFile.writeBytes(bundle.map)
        val map = bundle.files(server.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Complete, downloader.download(map, partFile, CountingTransfer()))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun aFileLongerThanExpectedIsStartedAgain() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        partFile.writeBytes(bundle.map + 0)
        val map = bundle.files(server.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Complete, downloader.download(map, partFile, CountingTransfer()))
        assertContentEquals(bundle.map, partFile.readBytes())
    }

    @Test
    fun keepsWhatArrivedWhenAskedToStop() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        val map = bundle.files(server.url("/ES-20.tar").toString(), mirror.url("/ES-20.tar").toString()).first()

        assertEquals(RangeFetch.Stopped, downloader.download(map, partFile, CountingTransfer(stopAfterBytes = 1)))
        assertEquals(0, mirror.requestCount)
    }

    @Test
    fun withoutMirrorsThereIsNothingToDownload() {
        val map = bundle.files().first()

        assertEquals(RangeFetch.Failed(DownloadFailure.SERVER), downloader.download(map, partFile, CountingTransfer()))
    }
}
