package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexedPackage
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Where each published file can be downloaded. The template uses {data_version}, {province} and
 * {file}, for example
 * `https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}-ES-{province}/{file}`.
 */
data class DownloadAddressTemplate(
    val template: String,
) {
    init {
        // The app ignores addresses that are not https (PackageIndexParser), so they are refused here.
        require(template.startsWith("https://") && FILE_PLACEHOLDER in template) {
            "The download address template must start with https:// and contain $FILE_PLACEHOLDER: $template"
        }
    }

    fun addressOf(
        dataVersion: String,
        provinceCode: String,
        fileName: String,
    ): String =
        template
            .replace("{data_version}", dataVersion)
            .replace("{province}", provinceCode)
            .replace(FILE_PLACEHOLDER, fileName)

    private companion object {
        const val FILE_PLACEHOLDER = "{file}"
    }
}

object IndexBuilder {
    /** The entry of a municipality whose files were written into the tar at [bundleAddress]. */
    fun indexedPackageOf(
        municipality: Municipality,
        entries: List<TarEntry>,
        bundleAddress: String,
    ): IndexedPackage =
        IndexedPackage(
            id = municipality.id,
            names = municipality.names,
            provinceCode = municipality.provinceCode,
            bounds = municipality.packageArea,
            centre = municipality.centre,
            files =
                entries.map { entry ->
                    PackageFile(
                        name = entry.name,
                        offset = entry.offset,
                        sizeBytes = entry.sizeBytes,
                        sha256 = entry.sha256,
                        urls = listOf(bundleAddress),
                    )
                },
        )

    // Files are named after their package so that they never collide inside a tar.
    fun mapFileNameOf(municipality: Municipality): String = "${municipality.id.value}.pmtiles"

    fun placesFileNameOf(municipality: Municipality): String = "${municipality.id.value}.places.json"
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
