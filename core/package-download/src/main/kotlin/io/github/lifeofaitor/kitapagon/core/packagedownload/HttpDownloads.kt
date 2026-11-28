package io.github.lifeofaitor.kitapagon.core.packagedownload

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** Why a download did not finish. */
enum class DownloadFailure {
    /** No connection, or it was lost: trying again later may work. */
    NETWORK,

    /** The server answered something the app does not accept, such as an error or other bytes. */
    SERVER,

    /** The bytes arrived but are not the published ones: their size or their hash is wrong. */
    DAMAGED,

    /** The phone could not write the file. */
    STORAGE,
}

sealed interface WholeFetch {
    class Fetched(
        val bytes: ByteArray,
    ) : WholeFetch

    data class Failed(
        val failure: DownloadFailure,
    ) : WholeFetch
}

sealed interface RangeFetch {
    data object Complete : RangeFetch

    /** The caller asked to stop; what arrived is kept, so the download resumes from there. */
    data object Stopped : RangeFetch

    data class Failed(
        val failure: DownloadFailure,
    ) : RangeFetch
}

/**
 * The two kinds of request the app makes to GitHub or its mirrors (decision 0017). Every call blocks,
 * so it must run on a background thread.
 */
class HttpDownloads(
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    /** A small file, such as the list of municipalities, refused when it is larger than [maximumBytes]. */
    fun fetchWhole(
        url: String,
        maximumBytes: Long,
    ): WholeFetch =
        execute(url, range = null, onFailure = WholeFetch::Failed) { response ->
            if (response.code == HTTP_OK) {
                readAtMost(response, maximumBytes)?.let(WholeFetch::Fetched)
                    ?: WholeFetch.Failed(DownloadFailure.SERVER)
            } else {
                WholeFetch.Failed(DownloadFailure.SERVER)
            }
        }

    /**
     * Appends the bytes of [range] to [output]. Only a partial answer with exactly those bytes is
     * accepted: the whole tar of a province would take hundreds of megabytes.
     */
    fun fetchRange(
        url: String,
        range: ByteRange,
        output: File,
        transfer: Transfer,
    ): RangeFetch =
        execute(url, range, onFailure = RangeFetch::Failed) { response ->
            if (response.code == HTTP_PARTIAL_CONTENT && range.isDescribedBy(response.header(CONTENT_RANGE))) {
                appendBody(response, range.length, output, transfer)
            } else {
                RangeFetch.Failed(DownloadFailure.SERVER)
            }
        }

    // An address that is not one comes from a wrong index, not from the network.
    private fun <T> execute(
        url: String,
        range: ByteRange?,
        onFailure: (DownloadFailure) -> T,
        handle: (Response) -> T,
    ): T {
        val address = url.toHttpUrlOrNull() ?: return onFailure(DownloadFailure.SERVER)
        return try {
            httpClient.newCall(requestFor(address, range)).execute().use(handle)
        } catch (expectedWithoutConnection: IOException) {
            onFailure(DownloadFailure.NETWORK)
        }
    }

    // The files are already compressed or are maps: compressing them again would only cost battery,
    // and a transparent decompression would change the bytes the signature and the hashes cover.
    private fun requestFor(
        address: HttpUrl,
        range: ByteRange?,
    ): Request =
        Request
            .Builder()
            .url(address)
            .header(ACCEPT_ENCODING, IDENTITY)
            .apply { range?.let { header(RANGE, it.rangeHeader) } }
            .build()

    // Never reads more than one byte past the limit, so a wrong answer cannot fill the memory.
    private fun readAtMost(
        response: Response,
        maximumBytes: Long,
    ): ByteArray? {
        val declaredTooLarge = response.body.contentLength() > maximumBytes
        return if (declaredTooLarge) null else response.body.source().readAtMostOrNull(maximumBytes)
    }

    private fun BufferedSource.readAtMostOrNull(maximumBytes: Long): ByteArray? =
        if (request(maximumBytes + 1)) null else readByteArray()

    private fun appendBody(
        response: Response,
        expectedBytes: Long,
        output: File,
        transfer: Transfer,
    ): RangeFetch =
        try {
            StorageStream.appendingTo(output).use { stream ->
                copy(response.body.byteStream(), stream, expectedBytes, transfer)
            }
        } catch (expectedWhenThePhoneCannotWrite: StorageFailureException) {
            RangeFetch.Failed(DownloadFailure.STORAGE)
        }

    // Reads exactly the expected bytes, and stops between two reads if the caller asks.
    private fun copy(
        body: InputStream,
        output: StorageStream,
        expectedBytes: Long,
        transfer: Transfer,
    ): RangeFetch {
        val buffer = ByteArray(BUFFER_SIZE)
        var remaining = expectedBytes
        var read = 0
        while (remaining > 0 && !transfer.shouldStop() && read >= 0) {
            read = body.read(buffer, 0, minOf(remaining, BUFFER_SIZE.toLong()).toInt())
            if (read > 0) {
                output.write(buffer, read)
                remaining -= read
                transfer.onBytes(read.toLong())
            }
        }
        return when {
            remaining == 0L -> RangeFetch.Complete
            read < 0 -> RangeFetch.Failed(DownloadFailure.NETWORK)
            else -> RangeFetch.Stopped
        }
    }

    // Tells a write that failed on the phone from a read that failed on the network.
    private class StorageStream private constructor(
        private val stream: FileOutputStream,
    ) : AutoCloseable {
        fun write(
            bytes: ByteArray,
            length: Int,
        ) = onStorage { stream.write(bytes, 0, length) }

        override fun close() = onStorage { stream.close() }

        companion object {
            fun appendingTo(file: File): StorageStream = onStorage { StorageStream(FileOutputStream(file, true)) }

            private fun <T> onStorage(operation: () -> T): T =
                try {
                    operation()
                } catch (storageFailure: IOException) {
                    throw StorageFailureException(storageFailure)
                }
        }
    }

    private class StorageFailureException(
        cause: IOException,
    ) : Exception(cause)

    companion object {
        private const val HTTP_OK = 200
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val RANGE = "Range"
        private const val CONTENT_RANGE = "Content-Range"
        private const val ACCEPT_ENCODING = "Accept-Encoding"
        private const val IDENTITY = "identity"
        private const val BUFFER_SIZE = 64 * 1024
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 30L

        // Without a cache: every file is checked with its hash, and the phone has little space.
        fun defaultHttpClient(): OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
    }
}

/** How a long download reports its progress and learns that it must stop. */
interface Transfer {
    fun shouldStop(): Boolean

    fun onBytes(byteCount: Long)
}
