package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFileKind
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import okio.Buffer
import java.security.MessageDigest
import kotlin.random.Random

internal fun sha256Of(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

internal fun okResponse(bytes: ByteArray): MockResponse =
    MockResponse
        .Builder()
        .code(200)
        .body(Buffer().write(bytes))
        .build()

internal fun partialResponse(
    bytes: ByteArray,
    range: ByteRange,
    totalBytes: Long,
): MockResponse =
    MockResponse
        .Builder()
        .code(206)
        .addHeader("Content-Range", "bytes ${range.first}-${range.last}/$totalBytes")
        .body(Buffer().write(bytes))
        .build()

/** The tar of a province with the two files of one package after their headers, as the generator writes it. */
internal class TestBundle(
    val map: ByteArray = Random(seed = 1).nextBytes(300_000),
    val places: ByteArray = "{\"format_version\":1,\"places\":[]}".toByteArray(),
) {
    private val mapOffset = 512L
    private val placesOffset = mapOffset + map.size + 512

    val bytes: ByteArray = ByteArray(mapOffset.toInt()) + map + ByteArray(512) + places

    fun files(vararg urls: String): List<PackageFile> =
        listOf(
            PackageFile(
                PackageFileKind.MAP,
                "ES-20045.pmtiles",
                mapOffset,
                map.size.toLong(),
                sha256Of(map),
                urls.toList(),
            ),
            PackageFile(
                PackageFileKind.PLACES,
                "ES-20045.places.json",
                placesOffset,
                places.size.toLong(),
                sha256Of(places),
                urls.toList(),
            ),
        )
}

/** Answers every partial request for [bundle] with exactly the bytes asked for, as GitHub does. */
internal class BundleDispatcher(
    private val bundle: ByteArray,
) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        val (first, last) =
            Regex("^bytes=([0-9]+)-([0-9]+)$")
                .matchEntire(request.headers["Range"].orEmpty())
                ?.destructured
                ?: return MockResponse.Builder().code(416).build()
        val range = ByteRange(first.toLong(), last.toLong())
        return partialResponse(bundle.copyOfRange(first.toInt(), last.toInt() + 1), range, bundle.size.toLong())
    }
}

/** Never asks to stop, and adds up the bytes that arrive. */
internal class CountingTransfer(
    private val stopAfterBytes: Long = Long.MAX_VALUE,
) : Transfer {
    var receivedBytes = 0L
        private set

    override fun shouldStop(): Boolean = receivedBytes >= stopAfterBytes

    override fun onBytes(byteCount: Long) {
        receivedBytes += byteCount
    }
}
