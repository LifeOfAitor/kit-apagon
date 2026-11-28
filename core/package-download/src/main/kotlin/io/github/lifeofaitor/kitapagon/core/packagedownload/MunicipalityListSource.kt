package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidIndexReason
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListParser
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListResult
import java.security.PublicKey

/** Where the signed list of municipalities and its signature are published. */
data class PublishedListAddresses(
    val list: String,
    val signature: String,
) {
    companion object {
        private const val LATEST = "https://github.com/LifeOfAitor/kit-apagon-datos/releases/latest/download/"

        /** The latest version of the data repository (decision 0017). */
        val GITHUB =
            PublishedListAddresses(
                list = LATEST + MunicipalityList.FILE_NAME,
                signature = LATEST + MunicipalityList.SIGNATURE_FILE_NAME,
            )
    }
}

sealed interface MunicipalityListFetch {
    data class Valid(
        val list: MunicipalityList,
    ) : MunicipalityListFetch

    data class Failed(
        val failure: DownloadFailure,
    ) : MunicipalityListFetch

    /** Not signed with the key of the app: it is never used, whatever it contains. */
    data object NotSigned : MunicipalityListFetch

    data class Invalid(
        val reason: InvalidIndexReason,
    ) : MunicipalityListFetch
}

/** Downloads the signed list of municipalities, the first thing the app needs to choose one. */
class MunicipalityListSource(
    private val downloads: HttpDownloads,
    private val publicKey: PublicKey,
    private val addresses: PublishedListAddresses = PublishedListAddresses.GITHUB,
) {
    /**
     * The list and its signature are two requests. When a new version is published between them they
     * belong to different versions, and the signature fails: asking again gives a matching pair.
     */
    fun fetch(): MunicipalityListFetch = fetchOnce().takeUnless { it == MunicipalityListFetch.NotSigned } ?: fetchOnce()

    private fun fetchOnce(): MunicipalityListFetch =
        when (val signature = downloads.fetchWhole(addresses.signature, MAXIMUM_SIGNATURE_BYTES)) {
            is WholeFetch.Failed -> MunicipalityListFetch.Failed(signature.failure)
            is WholeFetch.Fetched -> fetchListSignedWith(signature.bytes)
        }

    private fun fetchListSignedWith(signature: ByteArray): MunicipalityListFetch =
        when (val list = downloads.fetchWhole(addresses.list, MAXIMUM_LIST_BYTES)) {
            is WholeFetch.Failed -> MunicipalityListFetch.Failed(list.failure)
            is WholeFetch.Fetched -> checked(list.bytes, signature)
        }

    // The signature covers the compressed bytes, so nothing is decompressed before it is checked.
    private fun checked(
        compressedList: ByteArray,
        signature: ByteArray,
    ): MunicipalityListFetch {
        if (!IndexSignature.isValid(compressedList, signature, publicKey)) return MunicipalityListFetch.NotSigned
        val text = GzipBytes.decompressOrNull(compressedList, MAXIMUM_DECOMPRESSED_LIST_BYTES)?.decodeToString()
        return when (val result = text?.let(MunicipalityListParser::parse)) {
            null -> MunicipalityListFetch.Failed(DownloadFailure.DAMAGED)
            is MunicipalityListResult.Valid -> MunicipalityListFetch.Valid(result.list)
            is MunicipalityListResult.Invalid -> MunicipalityListFetch.Invalid(result.reason)
        }
    }

    private companion object {
        // An ECDSA P-256 signature takes at most 72 bytes.
        const val MAXIMUM_SIGNATURE_BYTES = 1_024L

        // The list of all of Spain takes 0.09 MB compressed and 0.54 MB once decompressed
        // (docs/investigacion/11-descarga-del-municipio-en-la-app.md): the limits leave room to grow.
        const val MAXIMUM_LIST_BYTES = 2_000_000L
        const val MAXIMUM_DECOMPRESSED_LIST_BYTES = 16_000_000
    }
}
