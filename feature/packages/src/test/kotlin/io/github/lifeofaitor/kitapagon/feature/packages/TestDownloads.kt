package io.github.lifeofaitor.kitapagon.feature.packages

import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackage
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityMatch
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityEntry
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.packageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.provinceEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/** Irun, ready to download: 3,501,019 bytes of map and 54,792 of places. */
internal fun downloadableIrun(): DownloadablePackage =
    DownloadablePackage.of(municipalityList(), provinceEntry(), packageDetails())

internal val IRUN_MATCH = MunicipalityMatch(municipalityEntry(), "Gipuzkoa")

/** Downloads that only change state when a test says so. */
internal class FakePackageDownloads : PackageDownloads {
    val started = mutableListOf<DownloadablePackage>()
    val cancelled = mutableListOf<UUID>()
    private val states = mutableMapOf<UUID, MutableStateFlow<DownloadState?>>()
    var lastJob: UUID? = null
        private set

    override suspend fun start(target: DownloadablePackage): UUID {
        started += target
        return UUID.randomUUID().also { job ->
            states[job] = MutableStateFlow(DownloadState.Waiting)
            lastJob = job
        }
    }

    override fun stateOf(job: UUID): Flow<DownloadState?> = states.getValue(job)

    override fun cancel(job: UUID) {
        cancelled += job
    }

    override val installedCount: Flow<Int> = MutableStateFlow(0)

    override suspend fun packagesBeingDownloaded(): Set<PackageId> = started.map { it.info.id }.toSet()

    fun moveLastJobTo(state: DownloadState) {
        states.getValue(checkNotNull(lastJob)).value = state
    }
}
