package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexedPackage
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndex
import java.io.File
import java.security.MessageDigest

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
    // A date such as 2026-12-01: it becomes part of release names and download addresses.
    private val DATA_VERSION = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    fun isValidDataVersion(dataVersion: String): Boolean = DATA_VERSION.matches(dataVersion)

    /** Describes the files already written to [packageFilesDirectory] for each municipality. */
    fun build(
        municipalities: List<Municipality>,
        packageFilesDirectory: File,
        dataVersion: String,
        addressTemplate: DownloadAddressTemplate,
    ): PackageIndex {
        require(isValidDataVersion(dataVersion)) { "The data version must be a date such as 2026-12-01: $dataVersion" }
        return PackageIndex(
            formatVersion = PackageIndex.CURRENT_FORMAT_VERSION,
            dataVersion = dataVersion,
            packages =
                municipalities.map { municipality ->
                    IndexedPackage(
                        id = municipality.id,
                        names = municipality.names,
                        provinceCode = municipality.provinceCode,
                        bounds = municipality.packageArea,
                        centre = municipality.centre,
                        files =
                            listOf(mapFileNameOf(municipality), placesFileNameOf(municipality)).map { fileName ->
                                val file = File(packageFilesDirectory, fileName)
                                val address =
                                    addressTemplate.addressOf(
                                        dataVersion,
                                        municipality.provinceCode,
                                        fileName,
                                    )
                                PackageFile(fileName, file.length(), sha256Of(file), listOf(address))
                            },
                    )
                },
        )
    }

    // Published files are named after their package so that they never collide in a release.
    fun mapFileNameOf(municipality: Municipality): String = "${municipality.id.value}.pmtiles"

    fun placesFileNameOf(municipality: Municipality): String = "${municipality.id.value}.places.json"

    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            generateSequence { input.read(buffer).takeIf { it >= 0 } }.forEach { read ->
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private const val BUFFER_SIZE = 64 * 1024
}
