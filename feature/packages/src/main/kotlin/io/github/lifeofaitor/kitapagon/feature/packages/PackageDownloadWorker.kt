package io.github.lifeofaitor.kitapagon.feature.packages

import android.content.Context
import android.os.Build
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadFailure
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackage
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackageJson
import io.github.lifeofaitor.kitapagon.core.packagedownload.InstallObserver
import io.github.lifeofaitor.kitapagon.core.packagedownload.PackageInstallResult

/** Installs a package; [io.github.lifeofaitor.kitapagon.core.packagedownload.PackageInstaller] in the app. */
fun interface PackageInstallation {
    fun install(
        target: DownloadablePackage,
        observer: InstallObserver,
    ): PackageInstallResult
}

/**
 * Downloads and installs one package in the background. The installation blocks, which is what a
 * [Worker] expects, and stops between two reads when WorkManager asks.
 */
class PackageDownloadWorker(
    context: Context,
    parameters: WorkerParameters,
    private val installation: PackageInstallation,
) : Worker(context, parameters) {
    private val target: DownloadablePackage? by lazy {
        inputData.getString(PACKAGE)?.let(DownloadablePackageJson::parseOrNull)
    }

    override fun doWork(): Result {
        val target = target ?: return Result.failure(outputOf(DownloadProblem.DAMAGED))
        return resultOf(installation.install(target, ProgressReporter(target)))
    }

    override fun getForegroundInfo(): ForegroundInfo =
        DownloadNotifications(
            applicationContext,
        ).foregroundInfo(
            target
                ?.info
                ?.names
                ?.official
                .orEmpty(),
            id,
            percent = null,
        )

    private fun resultOf(installed: PackageInstallResult): Result =
        when (val outcome = WorkerOutcome.of(installed, runAttemptCount)) {
            WorkerOutcome.Success -> Result.success()
            WorkerOutcome.Retry -> Result.retry()
            is WorkerOutcome.Failure -> Result.failure(outputOf(outcome.problem, outcome.neededBytes))
        }

    private inner class ProgressReporter(
        private val target: DownloadablePackage,
    ) : InstallObserver {
        private val throttle = PercentThrottle()

        override fun shouldStop(): Boolean = isStopped

        override fun onProgress(progress: DownloadProgress) {
            val percent = throttle.newPercentOrNull(progress) ?: return
            setProgressAsync(progressData(progress))
            updateNotification(percent)
        }

        // From Android 12 the urgent job is not a foreground service, and its notification does not exist.
        private fun updateNotification(percent: Int) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && percent % NOTIFICATION_STEP_PERCENT == 0) {
                setForegroundAsync(
                    DownloadNotifications(applicationContext).foregroundInfo(target.info.names.official, id, percent),
                )
            }
        }
    }

    companion object {
        const val PACKAGE = "package"
        private const val DOWNLOADED_BYTES = "downloaded_bytes"
        private const val TOTAL_BYTES = "total_bytes"
        private const val PROBLEM = "problem"
        private const val NEEDED_BYTES = "needed_bytes"
        private const val NOTIFICATION_STEP_PERCENT = 5

        internal fun progressData(progress: DownloadProgress): Data =
            Data
                .Builder()
                .putLong(DOWNLOADED_BYTES, progress.downloadedBytes)
                .putLong(TOTAL_BYTES, progress.totalBytes)
                .build()

        internal fun progressOf(data: Data): DownloadProgress? =
            data.getLong(TOTAL_BYTES, 0).takeIf { it > 0 }?.let { total ->
                DownloadProgress(data.getLong(DOWNLOADED_BYTES, 0), total)
            }

        internal fun outputOf(
            problem: DownloadProblem,
            neededBytes: Long? = null,
        ): Data =
            Data
                .Builder()
                .putString(PROBLEM, problem.name)
                .apply { neededBytes?.let { putLong(NEEDED_BYTES, it) } }
                .build()

        internal fun failureOf(data: Data): DownloadState.Failed {
            val problem =
                DownloadProblem.entries.firstOrNull { it.name == data.getString(PROBLEM) } ?: DownloadProblem.SERVER
            return DownloadState.Failed(problem, data.getLong(NEEDED_BYTES, 0).takeIf { it > 0 })
        }
    }
}

/** Each report is a write to the database of WorkManager, so only a new percentage is reported. */
internal class PercentThrottle {
    private var reportedPercent = -1

    fun newPercentOrNull(progress: DownloadProgress): Int? =
        progress.percent().takeIf { it != reportedPercent }?.also { reportedPercent = it }
}

/** What WorkManager does after an attempt: a cut connection or a busy server is tried again a few times. */
internal sealed interface WorkerOutcome {
    data object Success : WorkerOutcome

    data object Retry : WorkerOutcome

    data class Failure(
        val problem: DownloadProblem,
        val neededBytes: Long? = null,
    ) : WorkerOutcome

    companion object {
        // With the waits of WorkManager doubling from 30 seconds, five attempts cover about eight minutes.
        const val MAXIMUM_ATTEMPTS = 5

        fun of(
            installed: PackageInstallResult,
            attempt: Int,
        ): WorkerOutcome =
            when (installed) {
                is PackageInstallResult.Installed -> {
                    Success
                }

                PackageInstallResult.Stopped -> {
                    Retry
                }

                is PackageInstallResult.NotEnoughSpace -> {
                    Failure(
                        DownloadProblem.NOT_ENOUGH_SPACE,
                        installed.neededBytes,
                    )
                }

                is PackageInstallResult.Failed -> {
                    afterFailure(installed.failure, attempt)
                }
            }

        // A damaged file or a phone that cannot write would only spend data again.
        private fun afterFailure(
            failure: DownloadFailure,
            attempt: Int,
        ): WorkerOutcome {
            val problem =
                when (failure) {
                    DownloadFailure.NETWORK -> DownloadProblem.NO_CONNECTION
                    DownloadFailure.SERVER -> DownloadProblem.SERVER
                    DownloadFailure.DAMAGED -> DownloadProblem.DAMAGED
                    DownloadFailure.STORAGE -> DownloadProblem.STORAGE
                }
            val isTemporary = failure == DownloadFailure.NETWORK || failure == DownloadFailure.SERVER
            return if (isTemporary && attempt + 1 < MAXIMUM_ATTEMPTS) Retry else Failure(problem)
        }
    }
}

/** Gives WorkManager the download job with what it needs, wired by hand (decision 0008). */
class PackageDownloadWorkerFactory(
    private val installation: PackageInstallation,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == PackageDownloadWorker::class.java.name) {
            PackageDownloadWorker(appContext, workerParameters, installation)
        } else {
            null
        }
}
