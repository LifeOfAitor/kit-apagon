package io.github.lifeofaitor.kitapagon.feature.packages

import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadFailure
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packagedownload.PackageInstallResult
import io.github.lifeofaitor.kitapagon.core.packageformat.installedPackageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkerOutcomeTest {
    @Test
    fun anInstalledPackageIsASuccessAndAStopIsResumedLater() {
        assertEquals(
            WorkerOutcome.Success,
            WorkerOutcome.of(PackageInstallResult.Installed(installedPackageInfo()), attempt = 0),
        )
        assertEquals(WorkerOutcome.Retry, WorkerOutcome.of(PackageInstallResult.Stopped, attempt = 0))
    }

    @Test
    fun aCutConnectionOrABusyServerIsTriedFiveTimes() {
        val cut = PackageInstallResult.Failed(DownloadFailure.NETWORK)
        val busy = PackageInstallResult.Failed(DownloadFailure.SERVER)

        assertEquals(WorkerOutcome.Retry, WorkerOutcome.of(cut, attempt = 3))
        assertEquals(WorkerOutcome.Failure(DownloadProblem.NO_CONNECTION), WorkerOutcome.of(cut, attempt = 4))
        assertEquals(WorkerOutcome.Retry, WorkerOutcome.of(busy, attempt = 0))
        assertEquals(WorkerOutcome.Failure(DownloadProblem.SERVER), WorkerOutcome.of(busy, attempt = 4))
    }

    @Test
    fun whatWouldOnlySpendDataAgainIsNotRetried() {
        assertEquals(
            WorkerOutcome.Failure(DownloadProblem.DAMAGED),
            WorkerOutcome.of(PackageInstallResult.Failed(DownloadFailure.DAMAGED), attempt = 0),
        )
        assertEquals(
            WorkerOutcome.Failure(DownloadProblem.STORAGE),
            WorkerOutcome.of(PackageInstallResult.Failed(DownloadFailure.STORAGE), attempt = 0),
        )
        assertEquals(
            WorkerOutcome.Failure(DownloadProblem.NOT_ENOUGH_SPACE, neededBytes = 30_000_000),
            WorkerOutcome.of(PackageInstallResult.NotEnoughSpace(30_000_000, 1_000), attempt = 0),
        )
    }

    @Test
    fun reportsEachPercentageOnlyOnce() {
        val throttle = PercentThrottle()

        assertEquals(0, throttle.newPercentOrNull(DownloadProgress(0, 1_000)))
        assertNull(throttle.newPercentOrNull(DownloadProgress(9, 1_000)))
        assertEquals(1, throttle.newPercentOrNull(DownloadProgress(10, 1_000)))
        assertEquals(100, throttle.newPercentOrNull(DownloadProgress(1_000, 1_000)))
        assertEquals(0, DownloadProgress(0, 0).percent())
    }
}
