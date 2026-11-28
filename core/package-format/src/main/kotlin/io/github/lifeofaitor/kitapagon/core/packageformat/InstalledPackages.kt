package io.github.lifeofaitor.kitapagon.core.packageformat

import java.io.File

data class InstalledPackage(
    val id: PackageId,
    val mapFile: File,
    /** Optional: packages made before decision 0011 have no places file. */
    val placesFile: File,
    /** Null for packages installed by hand, such as the test packages of scripts/install-test-package.sh. */
    val info: InstalledPackageInfo?,
)

/** The package the map shows: the one the person chose if it is installed, otherwise the first one. */
fun List<InstalledPackage>.shownPackage(chosenId: PackageId?): InstalledPackage? =
    firstOrNull { it.id == chosenId } ?: firstOrNull()

/**
 * Municipal packages stored on the device, one directory per package named after its identifier:
 * `<rootDirectory>/<package id>/map.pmtiles` (decision 0010), optionally `places.json` (decision 0011)
 * and, for packages the app downloaded, `package.json` (decision 0017). Directories whose name is not
 * a package identifier, such as the ones of downloads in progress, are not packages.
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
            .sortedBy { installedPackage -> installedPackage.id.value }

    private fun toInstalledPackage(directory: File): InstalledPackage? {
        val id = PackageId.parseOrNull(directory.name)
        val mapFile = File(directory, MAP_FILE_NAME)
        return if (id != null && mapFile.isFile) {
            InstalledPackage(id, mapFile, File(directory, PLACES_FILE_NAME), infoOf(File(directory, INFO_FILE_NAME)))
        } else {
            null
        }
    }

    private fun infoOf(infoFile: File): InstalledPackageInfo? =
        infoFile.takeIf { it.isFile }?.let { InstalledPackageInfoParser.parseOrNull(it.readText()) }

    companion object {
        const val MAP_FILE_NAME = "map.pmtiles"
        const val PLACES_FILE_NAME = "places.json"
        const val INFO_FILE_NAME = "package.json"

        /** The name each kind of file of a package takes on the device. */
        fun fileNameOf(kind: PackageFileKind): String =
            when (kind) {
                PackageFileKind.MAP -> MAP_FILE_NAME
                PackageFileKind.PLACES -> PLACES_FILE_NAME
            }
    }
}
