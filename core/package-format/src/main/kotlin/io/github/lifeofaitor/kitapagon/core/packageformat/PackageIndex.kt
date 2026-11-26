package io.github.lifeofaitor.kitapagon.core.packageformat

/** One file of a package as listed in the index: what to download and how to check it (decision 0013). */
data class PackageFile(
    val name: String,
    val sizeBytes: Long,
    /** Lowercase hexadecimal SHA-256 of the file. */
    val sha256: String,
    /** Mirrors to try in order; any of them serves the same bytes, checked with the SHA-256. */
    val urls: List<String>,
)

/** The official local name and other names people may search for, such as the Spanish one. */
data class MunicipalityNames(
    val official: String,
    val alternatives: List<String> = emptyList(),
)

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

/** The signed list of every package that can be downloaded. */
data class PackageIndex(
    val formatVersion: Int,
    /** Date of the data, such as 2026-12-01. */
    val dataVersion: String,
    val packages: List<IndexedPackage>,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val INDEX_FILE_NAME = "index.json"
        const val SIGNATURE_FILE_NAME = "index.json.sig"
    }
}
