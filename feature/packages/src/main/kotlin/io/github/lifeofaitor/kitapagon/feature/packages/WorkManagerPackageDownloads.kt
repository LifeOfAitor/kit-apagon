package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackage
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackageJson
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * One urgent job per package that runs only with a connection. Without quota for urgent jobs it runs
 * as a normal one instead of being lost (decision 0017).
 */
class WorkManagerPackageDownloads(
    private val workManager: WorkManager,
    private val ioDispatcher: CoroutineDispatcher,
) : PackageDownloads {
    // Waits for the database of WorkManager, so it runs away from the main thread.
    override suspend fun start(target: DownloadablePackage): UUID =
        withContext(ioDispatcher) {
            val uniqueName = uniqueNameOf(target.info.id)
            val running = workManager.getWorkInfosForUniqueWork(uniqueName).get().firstOrNull { !it.state.isFinished }
            running?.id ?: requestFor(target)
                .also { request ->
                    workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request).result.get()
                }.id
        }

    override fun stateOf(job: UUID): Flow<DownloadState?> =
        workManager.getWorkInfoByIdFlow(job).map { work -> work?.let(::stateOf) }

    override fun cancel(job: UUID) {
        workManager.cancelWorkById(job)
    }

    override suspend fun packagesBeingDownloaded(): Set<PackageId>? =
        withContext(ioDispatcher) {
            try {
                workManager
                    .getWorkInfosByTag(TAG)
                    .get()
                    .filter { !it.state.isFinished }
                    .flatMap { work -> work.tags.mapNotNull(::packageIdOfTag) }
                    .toSet()
            } catch (expectedWhenWorkManagerFails: ExecutionException) {
                null
            }
        }

    override val installedCount: Flow<Int> =
        workManager
            .getWorkInfosByTagFlow(TAG)
            .map { works -> works.count { it.state == WorkInfo.State.SUCCEEDED } }
            .distinctUntilChanged()

    private fun requestFor(target: DownloadablePackage): OneTimeWorkRequest =
        OneTimeWorkRequest
            .Builder(PackageDownloadWorker::class.java)
            .setInputData(inputOf(target))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG)
            .addTag(uniqueNameOf(target.info.id))
            .build()

    companion object {
        private const val TAG = "package-download"
        private const val BACKOFF_SECONDS = 30L

        internal fun uniqueNameOf(id: PackageId): String = "$TAG-${id.value}"

        private fun packageIdOfTag(tag: String): PackageId? =
            tag
                .removePrefix("$TAG-")
                .takeIf {
                    it != tag
                }?.let(PackageId::parseOrNull)

        internal fun inputOf(target: DownloadablePackage): Data =
            Data
                .Builder()
                .putString(PackageDownloadWorker.PACKAGE, DownloadablePackageJson.write(target))
                .build()

        /** What the screen shows for the state of a job. */
        internal fun stateOf(work: WorkInfo): DownloadState =
            when (work.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Waiting
                WorkInfo.State.RUNNING -> DownloadState.Running(PackageDownloadWorker.progressOf(work.progress))
                WorkInfo.State.SUCCEEDED -> DownloadState.Installed
                WorkInfo.State.FAILED -> PackageDownloadWorker.failureOf(work.outputData)
                WorkInfo.State.CANCELLED -> DownloadState.Cancelled
            }
    }
}

private const val FULL_PERCENT = 100

internal fun DownloadProgress.percent(): Int =
    if (totalBytes >
        0
    ) {
        (downloadedBytes * FULL_PERCENT / totalBytes).toInt()
    } else {
        0
    }
