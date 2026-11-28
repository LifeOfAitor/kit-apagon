package io.github.lifeofaitor.kitapagon.core.packagedownload

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpDownloadsTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val server = MockWebServer().apply { start() }
    private val mirror = MockWebServer().apply { start() }
    private val downloads = HttpDownloads()
    private val bundle = TestBundle()
    private val range = ByteRange(512, 512L + bundle.map.size - 1)
    private val output by lazy { File(temporaryDirectory, "map.part") }

    @AfterEach
    fun stopServers() {
        server.close()
        mirror.close()
    }

    @Test
    fun fetchesASmallFileAsItIsPublished() {
        server.enqueue(okResponse(byteArrayOf(31, -117, 8)))

        val fetched = downloads.fetchWhole(server.url("/municipios.json.gz").toString(), maximumBytes = 3)

        assertContentEquals(byteArrayOf(31, -117, 8), assertIs<WholeFetch.Fetched>(fetched).bytes)
        assertEquals("identity", server.takeRequest().headers["Accept-Encoding"])
    }

    @Test
    fun refusesASmallFileLargerThanItsLimitWithOrWithoutItsLength() {
        server.enqueue(okResponse(ByteArray(4)))
        server.enqueue(MockResponse.Builder().chunkedBody(Buffer().write(ByteArray(4)), maxChunkSize = 1).build())
        val url = server.url("/municipios.json.gz").toString()

        assertEquals(WholeFetch.Failed(DownloadFailure.SERVER), downloads.fetchWhole(url, maximumBytes = 3))
        assertEquals(WholeFetch.Failed(DownloadFailure.SERVER), downloads.fetchWhole(url, maximumBytes = 3))
    }

    @Test
    fun anErrorOfTheServerIsNotAFile() {
        server.enqueue(
            MockResponse
                .Builder()
                .code(404)
                .body("Not Found")
                .build(),
        )

        assertEquals(
            WholeFetch.Failed(DownloadFailure.SERVER),
            downloads.fetchWhole(server.url("/municipios.json.gz").toString(), maximumBytes = 1_000),
        )
    }

    @Test
    fun withoutConnectionTheFailureIsOfTheNetwork() {
        val unreachable = server.url("/municipios.json.gz").toString()
        server.close()

        assertEquals(
            WholeFetch.Failed(DownloadFailure.NETWORK),
            downloads.fetchWhole(unreachable, maximumBytes = 1_000),
        )
        assertEquals(
            RangeFetch.Failed(DownloadFailure.NETWORK),
            downloads.fetchRange(unreachable, range, output, CountingTransfer()),
        )
    }

    @Test
    fun anAddressThatIsNotOneIsAFailureOfTheIndexNotOfTheNetwork() {
        assertEquals(WholeFetch.Failed(DownloadFailure.SERVER), downloads.fetchWhole("https://exa mple.org/x", 1_000))
        assertEquals(
            RangeFetch.Failed(DownloadFailure.SERVER),
            downloads.fetchRange("not an address", range, output, CountingTransfer()),
        )
    }

    @Test
    fun asksForExactlyTheBytesOfTheFileAndAppendsThem() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        output.writeBytes(byteArrayOf(1, 2))

        val fetched = downloads.fetchRange(server.url("/ES-20.tar").toString(), range, output, CountingTransfer())

        assertEquals(RangeFetch.Complete, fetched)
        assertContentEquals(byteArrayOf(1, 2) + bundle.map, output.readBytes())
        assertEquals("bytes=512-300511", server.takeRequest().headers["Range"])
    }

    @Test
    fun keepsTheRangeWhenGitHubRedirectsToWhereTheFileIs() {
        mirror.dispatcher = BundleDispatcher(bundle.bytes)
        server.enqueue(
            MockResponse
                .Builder()
                .code(302)
                .addHeader("Location", mirror.url("/assets/ES-20.tar").toString())
                .build(),
        )

        val fetched = downloads.fetchRange(server.url("/ES-20.tar").toString(), range, output, CountingTransfer())

        assertEquals(RangeFetch.Complete, fetched)
        assertEquals("bytes=512-300511", mirror.takeRequest().headers["Range"])
        assertContentEquals(bundle.map, output.readBytes())
    }

    @Test
    fun refusesTheWholeTarOrOtherBytesThanTheOnesAskedFor() {
        server.enqueue(okResponse(bundle.bytes))
        server.enqueue(partialResponse(bundle.map, ByteRange(0, bundle.map.size - 1L), bundle.bytes.size.toLong()))
        val url = server.url("/ES-20.tar").toString()

        assertEquals(
            RangeFetch.Failed(DownloadFailure.SERVER),
            downloads.fetchRange(url, range, output, CountingTransfer()),
        )
        assertEquals(
            RangeFetch.Failed(DownloadFailure.SERVER),
            downloads.fetchRange(url, range, output, CountingTransfer()),
        )
        assertEquals(0, output.length())
    }

    @Test
    fun aCutConnectionKeepsWhatArrived() {
        server.enqueue(
            partialResponse(bundle.map, range, bundle.bytes.size.toLong())
                .newBuilder()
                .onResponseBody(SocketEffect.ShutdownConnection)
                .build(),
        )

        val fetched = downloads.fetchRange(server.url("/ES-20.tar").toString(), range, output, CountingTransfer())

        assertEquals(RangeFetch.Failed(DownloadFailure.NETWORK), fetched)
        assertTrue(output.length() < bundle.map.size)
        assertContentEquals(bundle.map.copyOf(output.length().toInt()), output.readBytes())
    }

    @Test
    fun stopsBetweenTwoReadsWhenAsked() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        val transfer = CountingTransfer(stopAfterBytes = 1)

        val fetched = downloads.fetchRange(server.url("/ES-20.tar").toString(), range, output, transfer)

        assertEquals(RangeFetch.Stopped, fetched)
        assertEquals(transfer.receivedBytes, output.length())
        assertTrue(output.length() in 1 until bundle.map.size)
    }

    @Test
    fun aFileThePhoneCannotWriteIsAFailureOfTheStorage() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        val notAFile = File(temporaryDirectory, "a-directory").apply { mkdirs() }

        val fetched = downloads.fetchRange(server.url("/ES-20.tar").toString(), range, notAFile, CountingTransfer())

        assertEquals(RangeFetch.Failed(DownloadFailure.STORAGE), fetched)
    }
}
