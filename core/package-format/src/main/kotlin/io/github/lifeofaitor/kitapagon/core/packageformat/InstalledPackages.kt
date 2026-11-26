package io.github.lifeofaitor.kitapagon.core.packageformat

import java.io.File

data class InstalledPackage(
    val id: PackageId,
    val mapFile: File,
)

/**
 * Municipal packages stored on the device, one directory per package named after its identifier:
 * `<rootDirectory>/<package id>/map.pmtiles` (decision 0010).
 */
class InstalledPackages(
    private val rootDirectory: File,
) {
    /** Complete packages sorted by identifier. Unknown directories and incomplete packages are ignored. */
    fun list(): List<InstalledPackage> =
        rootDirectory
            .listFiles()
            .orEmpty()
            .filter { candidate -> candidate.isDirectory }
            .mapNotNull { directory -> toInstalledPackage(directory) }
            .filter { installedPackage -> installedPackage.mapFile.isFile }
            .sortedBy { installedPackage -> installedPackage.id.value }

    private fun toInstalledPackage(directory: File): InstalledPackage? =
        PackageId.parseOrNull(directory.name)?.let { id -> InstalledPackage(id, File(directory, MAP_FILE_NAME)) }

    companion object {
        const val MAP_FILE_NAME = "map.pmtiles"
    }
}
