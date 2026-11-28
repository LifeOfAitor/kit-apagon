package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfo
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfoWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceEntry
import java.io.File
import java.io.IOException

/** A package ready to download: what the app will keep about it and where its files are. */
data class DownloadablePackage(
    val info: InstalledPackageInfo,
    val files: List<PackageFile>,
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }

    companion object {
        fun of(
            list: MunicipalityList,
            province: ProvinceEntry,
            details: PackageDetails,
        ): DownloadablePackage =
            DownloadablePackage(
                InstalledPackageInfo(
                    details.id,
                    details.names,
                    province.code,
                    province.name,
                    list.dataVersion,
                    details.bounds,
                    details.centre,
                ),
                list.resolve(province.code, details).files,
            )
    }
}

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
)

/** How the screen or the background job follows an installation and asks it to stop. */
interface InstallObserver {
    fun shouldStop(): Boolean

    fun onProgress(progress: DownloadProgress)
}

sealed interface PackageInstallResult {
    data class Installed(
        val info: InstalledPackageInfo,
    ) : PackageInstallResult

    data class NotEnoughSpace(
        val neededBytes: Long,
        val availableBytes: Long,
    ) : PackageInstallResult

    /** Stopped on request; the next installation of the same package resumes the download. */
    data object Stopped : PackageInstallResult

    data class Failed(
        val failure: DownloadFailure,
    ) : PackageInstallResult
}

/**
 * Downloads a package next to the installed ones and puts it in place with one rename, so the
 * directory of a package always holds a complete package, old or new (decision 0017). [PackageStorage]
 * describes the directories it uses. One installation per package at a time; every call blocks, so it
 * must run on a background thread.
 */
class PackageInstaller(
    private val storage: PackageStorage,
    private val downloader: PackageFileDownloader,
    private val usableBytesOf: (File) -> Long = File::getUsableSpace,
) {
    fun install(
        target: DownloadablePackage,
        observer: InstallObserver,
    ): PackageInstallResult =
        try {
            storage.finishInterruptedOperations()
            downloadAndReplace(target, observer)
        } catch (expectedWhenThePhoneCannotWrite: IOException) {
            storage.finishInterruptedOperations()
            PackageInstallResult.Failed(DownloadFailure.STORAGE)
        }

    private fun downloadAndReplace(
        target: DownloadablePackage,
        observer: InstallObserver,
    ): PackageInstallResult {
        val workDirectory = storage.downloadDirectoryOf(target.info.id).also(storage::createDirectory)
        val parts =
            target.files.associateWith { file ->
                File(workDirectory, "${file.kind.wireName}-${file.sha256}.part")
            }
        // The hash is in the name of each part, so the parts of another version are never resumed.
        workDirectory
            .listFiles()
            .orEmpty()
            .filter { it !in parts.values }
            .forEach { it.deleteRecursively() }
        val neededBytes = parts.entries.sumOf { (file, part) -> file.sizeBytes - minOf(part.length(), file.sizeBytes) }
        val availableBytes = usableBytesOf(storage.rootDirectory)
        if (availableBytes < neededBytes + SPACE_MARGIN_BYTES) {
            return PackageInstallResult.NotEnoughSpace(neededBytes + SPACE_MARGIN_BYTES, availableBytes)
        }
        return when (val downloaded = downloadAll(parts, target.totalBytes, observer)) {
            RangeFetch.Complete -> replaceWith(target, parts, workDirectory)
            RangeFetch.Stopped -> PackageInstallResult.Stopped
            is RangeFetch.Failed -> PackageInstallResult.Failed(downloaded.failure)
        }
    }

    private fun downloadAll(
        parts: Map<PackageFile, File>,
        totalBytes: Long,
        observer: InstallObserver,
    ): RangeFetch {
        val progress = ProgressCounter(parts.values.sumOf { it.length() }, totalBytes, observer).also { it.report() }
        var outcome: RangeFetch = RangeFetch.Complete
        for ((file, part) in parts) {
            outcome = downloader.download(file, part, progress)
            if (outcome != RangeFetch.Complete) break
        }
        return outcome
    }

    private fun replaceWith(
        target: DownloadablePackage,
        parts: Map<PackageFile, File>,
        workDirectory: File,
    ): PackageInstallResult {
        val newPackage = File(workDirectory, NEW_PACKAGE_DIRECTORY_NAME).also(storage::createDirectory)
        parts.forEach { (file, part) -> storage.move(part, File(newPackage, InstalledPackages.fileNameOf(file.kind))) }
        File(newPackage, InstalledPackages.INFO_FILE_NAME).writeText(InstalledPackageInfoWriter.write(target.info))
        storage.replace(target.info.id, newPackage)
        workDirectory.deleteRecursively()
        return PackageInstallResult.Installed(target.info)
    }

    private class ProgressCounter(
        private var downloadedBytes: Long,
        private val totalBytes: Long,
        private val observer: InstallObserver,
    ) : Transfer {
        override fun shouldStop(): Boolean = observer.shouldStop()

        // A damaged file is downloaded again, so the count is kept within the total.
        override fun onBytes(byteCount: Long) {
            downloadedBytes += byteCount
            report()
        }

        fun report() = observer.onProgress(DownloadProgress(minOf(downloadedBytes, totalBytes), totalBytes))
    }

    companion object {
        private const val NEW_PACKAGE_DIRECTORY_NAME = "package"

        // Room left for the system and other apps: a phone with its storage full stops working well.
        const val SPACE_MARGIN_BYTES = 20_000_000L
    }
}
