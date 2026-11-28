package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidIndexReason
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsParser
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsResult
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceEntry
import java.security.MessageDigest

sealed interface ProvinceDetailsFetch {
    data class Valid(
        val details: ProvinceDetails,
    ) : ProvinceDetailsFetch

    data class Failed(
        val failure: DownloadFailure,
    ) : ProvinceDetailsFetch

    data class Invalid(
        val reason: InvalidIndexReason,
    ) : ProvinceDetailsFetch
}

/**
 * Downloads the details of the province of the chosen municipality, checked with the size and the hash
 * that the signed list gives for them (decision 0017).
 */
class ProvinceDetailsSource(
    private val downloads: HttpDownloads,
) {
    fun fetch(
        list: MunicipalityList,
        province: ProvinceEntry,
    ): ProvinceDetailsFetch {
        val addresses = list.addresses.detailsAddressesOf(list.dataVersion, province.details.name)
        var outcome: ProvinceDetailsFetch = ProvinceDetailsFetch.Failed(DownloadFailure.SERVER)
        for (address in addresses) {
            outcome = fetchFrom(address, list, province)
            if (outcome !is ProvinceDetailsFetch.Failed) break
        }
        return outcome
    }

    // A mirror that fails or serves other bytes is skipped: the next one may have the published file.
    private fun fetchFrom(
        address: String,
        list: MunicipalityList,
        province: ProvinceEntry,
    ): ProvinceDetailsFetch =
        when (val fetched = downloads.fetchWhole(address, province.details.sizeBytes)) {
            is WholeFetch.Failed -> ProvinceDetailsFetch.Failed(fetched.failure)
            is WholeFetch.Fetched -> readIfPublished(fetched.bytes, list, province)
        }

    private fun readIfPublished(
        bytes: ByteArray,
        list: MunicipalityList,
        province: ProvinceEntry,
    ): ProvinceDetailsFetch = if (isPublished(bytes, province)) read(bytes, list, province) else DAMAGED

    private fun isPublished(
        bytes: ByteArray,
        province: ProvinceEntry,
    ): Boolean =
        bytes.size.toLong() == province.details.sizeBytes &&
            MessageDigest.getInstance("SHA-256").digest(bytes).toHexString() == province.details.sha256

    private fun read(
        compressed: ByteArray,
        list: MunicipalityList,
        province: ProvinceEntry,
    ): ProvinceDetailsFetch {
        val text = GzipBytes.decompressOrNull(compressed, MAXIMUM_DECOMPRESSED_BYTES)?.decodeToString()
        return when (val result = text?.let(ProvinceDetailsParser::parse)) {
            null -> DAMAGED
            is ProvinceDetailsResult.Invalid -> ProvinceDetailsFetch.Invalid(result.reason)
            is ProvinceDetailsResult.Valid -> matching(result.details, list, province)
        }
    }

    // The hash already proves these are the published bytes; this catches a mistake when publishing.
    private fun matching(
        details: ProvinceDetails,
        list: MunicipalityList,
        province: ProvinceEntry,
    ): ProvinceDetailsFetch =
        if (details.dataVersion == list.dataVersion && details.provinceCode == province.code) {
            ProvinceDetailsFetch.Valid(details)
        } else {
            ProvinceDetailsFetch.Invalid(InvalidIndexReason.NOT_AN_INDEX)
        }

    private companion object {
        val DAMAGED = ProvinceDetailsFetch.Failed(DownloadFailure.DAMAGED)

        // The details of a province take at most 0.05 MB compressed, those of Burgos, and grow about three
        // times when decompressed, as measured with the Basque Country.
        const val MAXIMUM_DECOMPRESSED_BYTES = 8_000_000
    }
}
