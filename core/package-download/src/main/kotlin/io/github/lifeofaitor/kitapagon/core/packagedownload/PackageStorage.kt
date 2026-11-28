package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import java.io.File
import java.io.IOException

/**
 * The packages on the device and the directories that keep each one complete while it changes
 * (decision 0017). Their names are not package identifiers, so [InstalledPackages] ignores them:
 *
 * - `.downloads/<id>/`: the files being downloaded, kept to resume, and the new package before it
 *   is moved in;
 * - `.replaced/<id>/`: the previous package, only while it is replaced;
 * - `.deleted/<id>/`: a package being deleted, moved out first so that it never shows half deleted.
 *
 * Every call blocks, so it must run on a background thread.
 */
class PackageStorage(
    internal val rootDirectory: File,
) {
    private val downloadsDirectory = File(rootDirectory, DOWNLOADS_DIRECTORY_NAME)
    private val replacedDirectory = File(rootDirectory, REPLACED_DIRECTORY_NAME)
    private val deletedDirectory = File(rootDirectory, DELETED_DIRECTORY_NAME)

    /**
     * Puts back a previous package whose replacement was cut, for example because the phone turned
     * off, and finishes deleting what was being deleted. The app calls it before listing the packages.
     */
    fun finishInterruptedOperations() {
        replacedDirectory.listFiles().orEmpty().forEach { replaced ->
            val installed = File(rootDirectory, replaced.name)
            if (installed.exists()) replaced.deleteRecursively() else replaced.renameTo(installed)
        }
        deletedDirectory.deleteRecursively()
    }

    /** Deletes an installed package and what was downloaded of it. False if the phone could not. */
    fun uninstall(id: PackageId): Boolean =
        try {
            val installed = installedDirectoryOf(id)
            val deleted = File(deletedDirectory, id.value).apply { deleteRecursively() }
            if (installed.exists()) {
                createDirectory(deletedDirectory)
                move(installed, deleted)
            }
            downloadDirectoryOf(id).deleteRecursively()
            deleted.deleteRecursively()
            true
        } catch (expectedWhenThePhoneCannotWrite: IOException) {
            false
        }

    /**
     * Deletes what was downloaded of the packages that are no longer being downloaded, such as one
     * whose download was cancelled, so it does not take space for nothing.
     */
    fun discardDownloadsExcept(beingDownloaded: Set<PackageId>) {
        downloadsDirectory
            .listFiles()
            .orEmpty()
            .filter { directory -> PackageId.parseOrNull(directory.name) !in beingDownloaded }
            .forEach { directory -> directory.deleteRecursively() }
    }

    internal fun downloadDirectoryOf(id: PackageId): File = File(downloadsDirectory, id.value)

    // The map on the screen keeps working while its files are replaced: they stay readable until closed.
    internal fun replace(
        id: PackageId,
        newPackage: File,
    ) {
        val installed = installedDirectoryOf(id)
        if (installed.exists()) {
            createDirectory(replacedDirectory)
            move(installed, File(replacedDirectory, installed.name))
        }
        move(newPackage, installed)
        File(replacedDirectory, installed.name).deleteRecursively()
    }

    internal fun move(
        from: File,
        to: File,
    ) {
        if (!from.renameTo(to)) throw IOException("Cannot move $from to $to")
    }

    internal fun createDirectory(directory: File) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create $directory")
    }

    private fun installedDirectoryOf(id: PackageId): File = File(rootDirectory, id.value)

    companion object {
        const val DOWNLOADS_DIRECTORY_NAME = ".downloads"
        const val REPLACED_DIRECTORY_NAME = ".replaced"
        const val DELETED_DIRECTORY_NAME = ".deleted"
    }
}
