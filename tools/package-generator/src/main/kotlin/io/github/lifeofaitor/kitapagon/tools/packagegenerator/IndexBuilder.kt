package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.BundledFile
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFileKind
import java.time.LocalDate
import java.time.ZoneOffset

object IndexBuilder {
    /** The details of a municipality whose files were written into the tar named [bundleName]. */
    fun packageDetailsOf(
        municipality: Municipality,
        entries: Map<PackageFileKind, TarEntry>,
        bundleName: String,
    ): PackageDetails =
        PackageDetails(
            id = municipality.id,
            names = municipality.names,
            bounds = municipality.packageArea,
            centre = municipality.centre,
            bundle = bundleName,
            files =
                entries.map { (kind, entry) ->
                    BundledFile(kind, entry.name, entry.offset, entry.sizeBytes, entry.sha256)
                },
        )

    // Files are named after their package so that they never collide inside a tar.
    fun mapFileNameOf(municipality: Municipality): String = "${municipality.id.value}.pmtiles"

    fun placesFileNameOf(municipality: Municipality): String = "${municipality.id.value}.places.json"

    /** The part a build writes for each province; [IndexParts] joins them into the published index. */
    fun partFileNameOf(provinceCode: String): String = "provincia-ES-$provinceCode.json"
}

/** The version of a month of data: the date it was generated, such as 2026-12-01. */
object DataVersion {
    private val FORMAT = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    // It becomes part of release names and download addresses, so only real dates are accepted.
    fun isValid(dataVersion: String): Boolean =
        FORMAT.matches(dataVersion) && runCatching { LocalDate.parse(dataVersion) }.isSuccess

    /** Midnight of that date in UTC, in seconds since 1970. */
    fun epochSecondsOf(dataVersion: String): Long =
        LocalDate.parse(dataVersion).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
}
