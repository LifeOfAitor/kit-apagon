package io.github.lifeofaitor.kitapagon.feature.packages

import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Why a package could not be installed, as the screen explains it. */
enum class DownloadProblem {
    NO_CONNECTION,
    SERVER,
    DAMAGED,
    STORAGE,
    NOT_ENOUGH_SPACE,
}

sealed interface DownloadState {
    /** Waiting for a connection, for its turn or for a new attempt after a cut. */
    data object Waiting : DownloadState

    data class Running(
        val progress: DownloadProgress?,
    ) : DownloadState

    data object Installed : DownloadState

    data class Failed(
        val problem: DownloadProblem,
        /** Only for [DownloadProblem.NOT_ENOUGH_SPACE]: the free space the package needs. */
        val neededBytes: Long? = null,
    ) : DownloadState

    data object Cancelled : DownloadState
}

/** The downloads that go on in the background, even with the app closed (decision 0017). */
interface PackageDownloads {
    /** The job that installs [target]: the one already running for that package, or a new one. */
    suspend fun start(target: DownloadablePackage): UUID

    /** Null once the job no longer exists. */
    fun stateOf(job: UUID): Flow<DownloadState?>

    fun cancel(job: UUID)

    /** Changes each time a package is installed, so the map can show it. */
    val installedCount: Flow<Int>
}
