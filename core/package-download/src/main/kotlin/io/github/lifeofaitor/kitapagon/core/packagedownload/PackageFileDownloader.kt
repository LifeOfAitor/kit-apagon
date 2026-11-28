package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Downloads one file of a package from the tar of its province into [partFile] (decision 0017). What
 * already arrived is kept, so a cut download resumes from there, from the same mirror or the next one:
 * all of them serve the same bytes, and the hash checks the whole file at the end.
 */
class PackageFileDownloader(
    private val downloads: HttpDownloads,
) {
    fun download(
        file: PackageFile,
        partFile: File,
        transfer: Transfer,
    ): RangeFetch {
        if (partFile.length() > file.sizeBytes) partFile.delete()
        var outcome: RangeFetch = RangeFetch.Failed(DownloadFailure.SERVER)
        for (url in file.urls) {
            outcome = downloadFrom(url, file, partFile, transfer)
            if (outcome !is RangeFetch.Failed) break
        }
        return outcome
    }

    private fun downloadFrom(
        url: String,
        file: PackageFile,
        partFile: File,
        transfer: Transfer,
    ): RangeFetch {
        val received = partFile.length()
        val fetched =
            if (received < file.sizeBytes) {
                downloads.fetchRange(url, ByteRange(file.offset + received, file.lastByte), partFile, transfer)
            } else {
                RangeFetch.Complete
            }
        return if (fetched == RangeFetch.Complete) checked(file, partFile) else fetched
    }

    // A damaged file cannot be repaired by resuming it, so it is started again from the next mirror.
    private fun checked(
        file: PackageFile,
        partFile: File,
    ): RangeFetch =
        when (sha256Of(partFile)) {
            file.sha256 -> {
                RangeFetch.Complete
            }

            null -> {
                RangeFetch.Failed(DownloadFailure.STORAGE)
            }

            else -> {
                partFile.delete()
                RangeFetch.Failed(DownloadFailure.DAMAGED)
            }
        }

    private fun sha256Of(file: File): String? =
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read = input.read(buffer)
                while (read >= 0) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            digest.digest().toHexString()
        } catch (expectedWhenTheFileCannotBeRead: IOException) {
            null
        }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
