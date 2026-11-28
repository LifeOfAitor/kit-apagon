package io.github.lifeofaitor.kitapagon.core.packageformat

/** What a file of a package is for: the app stores each kind under its own name (decisions 0010 and 0011). */
enum class PackageFileKind(
    val wireName: String,
) {
    MAP("map"),
    PLACES("places"),
    ;

    companion object {
        fun fromWireName(wireName: String): PackageFileKind? = entries.firstOrNull { it.wireName == wireName }
    }
}

/**
 * One file of a package, ready to download: the files of a province are published together in a tar,
 * and each one is downloaded alone with a partial request (decisions 0013 and 0014).
 */
data class PackageFile(
    val kind: PackageFileKind,
    val name: String,
    /** Position of the first byte of the file inside the resource at [urls]. */
    val offset: Long,
    val sizeBytes: Long,
    /** Lowercase hexadecimal SHA-256 of the file. */
    val sha256: String,
    /** Mirrors to try in order; any of them serves the same bytes, checked with the SHA-256. */
    val urls: List<String>,
) {
    /** The last byte to request, as written in an HTTP Range header. */
    val lastByte: Long get() = offset + sizeBytes - 1
}

/** The official local name and other names people may search for, such as the Spanish one. */
data class MunicipalityNames(
    val official: String,
    val alternatives: List<String> = emptyList(),
)

/** A package with everything needed to download it, resolved from the published index. */
data class IndexedPackage(
    val id: PackageId,
    val names: MunicipalityNames,
    /** The first two digits of the INE code. */
    val provinceCode: String,
    /** Area of the package: the municipality plus its margin. */
    val bounds: GeographicBounds,
    val centre: GeographicPoint,
    val files: List<PackageFile>,
)

/** A file the app downloads whole and checks with its SHA-256, such as the details of a province. */
data class PublishedFile(
    val name: String,
    val sizeBytes: Long,
    /** Lowercase hexadecimal SHA-256 of the file as it is downloaded, compressed. */
    val sha256: String,
)

/**
 * Where the files are published. Each list holds mirrors to try in order, all serving the same bytes.
 * Templates use {data_version}, {province} and {file}.
 */
data class AddressTemplates(
    val details: List<String>,
    val bundles: List<String>,
) {
    fun detailsAddressesOf(
        dataVersion: String,
        fileName: String,
    ): List<String> = details.map { template -> template.filled(dataVersion, provinceCode = "", fileName) }

    fun bundleAddressesOf(
        dataVersion: String,
        provinceCode: String,
        fileName: String,
    ): List<String> = bundles.map { template -> template.filled(dataVersion, provinceCode, fileName) }

    private fun String.filled(
        dataVersion: String,
        provinceCode: String,
        fileName: String,
    ): String =
        replace("{data_version}", dataVersion)
            .replace("{province}", provinceCode)
            .replace("{file}", fileName)
}

data class MunicipalityEntry(
    val id: PackageId,
    val names: MunicipalityNames,
    val provinceCode: String,
)

data class ProvinceEntry(
    val code: String,
    val name: String,
    val details: PublishedFile,
)

/**
 * The signed list the app downloads first, with enough to search every municipality (decision 0017).
 * The details of each province are downloaded only for the municipality the person chooses.
 */
data class MunicipalityList(
    val formatVersion: Int,
    /** Date of the data, such as 2026-12-01. */
    val dataVersion: String,
    val addresses: AddressTemplates,
    val provinces: List<ProvinceEntry>,
    val municipalities: List<MunicipalityEntry>,
) {
    fun provinceOf(municipality: MunicipalityEntry): ProvinceEntry? =
        provinces.firstOrNull {
            it.code ==
                municipality.provinceCode
        }

    /** The package of [details], with the addresses of its tar filled in. */
    fun resolve(
        provinceCode: String,
        details: PackageDetails,
    ): IndexedPackage =
        IndexedPackage(
            id = details.id,
            names = details.names,
            provinceCode = provinceCode,
            bounds = details.bounds,
            centre = details.centre,
            files =
                details.files.map { file ->
                    PackageFile(
                        kind = file.kind,
                        name = file.name,
                        offset = file.offset,
                        sizeBytes = file.sizeBytes,
                        sha256 = file.sha256,
                        urls = addresses.bundleAddressesOf(dataVersion, provinceCode, details.bundle),
                    )
                },
        )

    companion object {
        const val CURRENT_FORMAT_VERSION = 2
        const val FILE_NAME = "municipios.json.gz"
        const val SIGNATURE_FILE_NAME = "municipios.json.gz.sig"

        fun detailsFileNameOf(provinceCode: String): String = "provincia-ES-$provinceCode.json.gz"
    }
}

/** A file of a package inside the tar of its province (decision 0014). */
data class BundledFile(
    val kind: PackageFileKind,
    /** The name of the file inside the tar. */
    val name: String,
    val offset: Long,
    val sizeBytes: Long,
    val sha256: String,
)

data class PackageDetails(
    val id: PackageId,
    val names: MunicipalityNames,
    /** Area of the package: the municipality plus its margin. */
    val bounds: GeographicBounds,
    val centre: GeographicPoint,
    /** The name of the tar of the province that holds the files. */
    val bundle: String,
    val files: List<BundledFile>,
)

/** The details of every package of a province, checked with the hash the signed list gives for it. */
data class ProvinceDetails(
    val formatVersion: Int,
    val dataVersion: String,
    val provinceCode: String,
    val provinceName: String,
    val packages: List<PackageDetails>,
)
