package io.github.lifeofaitor.kitapagon.feature.packages

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadFailure
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackage
import io.github.lifeofaitor.kitapagon.core.packagedownload.PackageInstallResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WorkManagerPackageDownloadsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val target = downloadableIrun()
    private val installed = mutableListOf<DownloadablePackage>()
    private var nextResult: PackageInstallResult = PackageInstallResult.Installed(target.info)
    private val installation =
        PackageInstallation { received, _ ->
            installed += received
            nextResult
        }
    private lateinit var downloads: WorkManagerPackageDownloads

    @Before
    fun startTestWorkManager() {
        val configuration =
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(PackageDownloadWorkerFactory(installation))
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        downloads = WorkManagerPackageDownloads(WorkManager.getInstance(context), Dispatchers.IO)
    }

    @After
    fun closeTheDatabaseOfWorkManager() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun waitsForAConnectionBeforeDownloading() =
        runBlocking {
            val job = downloads.start(target)

            assertEquals(DownloadState.Waiting, downloads.stateOf(job).first())
            val work = WorkManager.getInstance(context).getWorkInfoById(job).get()
            assertEquals(NetworkType.CONNECTED, work?.constraints?.requiredNetworkType)
            assertEquals(emptyList<DownloadablePackage>(), installed)
        }

    @Test
    fun installsThePackageOnceThereIsAConnection() =
        runBlocking {
            val job = downloads.start(target)

            connect(job)

            assertEquals(DownloadState.Installed, downloads.stateOf(job).first())
            assertEquals(listOf(target), installed)
            assertEquals(1, downloads.installedCount.first())
        }

    @Test
    fun choosingTheSamePackageAgainFollowsTheJobThatIsWaiting() =
        runBlocking {
            assertEquals(downloads.start(target), downloads.start(target))
        }

    @Test
    fun aFailedDownloadSaysWhyAndCanBeStartedAgain() =
        runBlocking {
            nextResult = PackageInstallResult.Failed(DownloadFailure.DAMAGED)
            val failed = downloads.start(target)
            connect(failed)

            assertEquals(DownloadState.Failed(DownloadProblem.DAMAGED), downloads.stateOf(failed).first())
            assertNotEquals(failed, downloads.start(target))
        }

    @Test
    fun aCancelledDownloadIsReportedAsCancelled() =
        runBlocking {
            val job = downloads.start(target)

            downloads.cancel(job)

            assertEquals(DownloadState.Cancelled, downloads.stateOf(job).first())
        }

    @Test
    fun aJobWithoutAPackageItCanReadDoesNotDownloadAnything() {
        val worker = workerWith(Data.EMPTY)

        assertEquals(
            ListenableWorker.Result.failure(PackageDownloadWorker.outputOf(DownloadProblem.DAMAGED)),
            worker.doWork(),
        )
        assertEquals(emptyList<DownloadablePackage>(), installed)
    }

    @Test
    fun theNotificationUpToAndroid11NamesTheMunicipality() {
        val notification = workerWith(WorkManagerPackageDownloads.inputOf(target)).getForegroundInfo().notification

        assertEquals("Descargando el mapa de Irun", notification.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun theProgressAndTheFailureKeepTheirValuesThroughWorkManager() {
        val progress =
            io.github.lifeofaitor.kitapagon.core.packagedownload
                .DownloadProgress(1_000, 4_000)
        val failure = PackageDownloadWorker.outputOf(DownloadProblem.NOT_ENOUGH_SPACE, 30_000_000)

        assertEquals(progress, PackageDownloadWorker.progressOf(PackageDownloadWorker.progressData(progress)))
        assertEquals(null, PackageDownloadWorker.progressOf(Data.EMPTY))
        assertEquals(
            DownloadState.Failed(DownloadProblem.NOT_ENOUGH_SPACE, 30_000_000),
            PackageDownloadWorker.failureOf(failure),
        )
        assertEquals(DownloadState.Failed(DownloadProblem.SERVER), PackageDownloadWorker.failureOf(Data.EMPTY))
    }

    private fun connect(job: UUID) {
        checkNotNull(WorkManagerTestInitHelper.getTestDriver(context)).setAllConstraintsMet(job)
    }

    private fun workerWith(input: Data): PackageDownloadWorker =
        TestListenableWorkerBuilder
            .from(context, PackageDownloadWorker::class.java)
            .setInputData(input)
            .setWorkerFactory(PackageDownloadWorkerFactory(installation))
            .build()
}
