package io.github.lifeofaitor.kitapagon.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import io.github.lifeofaitor.kitapagon.core.packagedownload.HttpDownloads
import io.github.lifeofaitor.kitapagon.core.packagedownload.MunicipalityListSource
import io.github.lifeofaitor.kitapagon.core.packagedownload.PackageFileDownloader
import io.github.lifeofaitor.kitapagon.core.packagedownload.PackageInstaller
import io.github.lifeofaitor.kitapagon.core.packagedownload.PackageStorage
import io.github.lifeofaitor.kitapagon.core.packagedownload.ProvinceDetailsSource
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.feature.packages.IndexPublicKey
import io.github.lifeofaitor.kitapagon.feature.packages.MunicipalityListLoader
import io.github.lifeofaitor.kitapagon.feature.packages.MyMunicipalitiesSources
import io.github.lifeofaitor.kitapagon.feature.packages.PackageDownloadWorkerFactory
import io.github.lifeofaitor.kitapagon.feature.packages.PackageDownloads
import io.github.lifeofaitor.kitapagon.feature.packages.ProvinceDetailsLoader
import io.github.lifeofaitor.kitapagon.feature.packages.WorkManagerPackageDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Creates the objects the screens and the background jobs share, once per process. Dependencies are
 * wired by hand (decision 0008), and the ones that use the network are only created when first used.
 */
class AppContainer(
    private val applicationContext: Context,
) {
    private val packagesDirectory = File(applicationContext.filesDir, MUNICIPAL_PACKAGES_DIRECTORY)
    private val httpDownloads by lazy { HttpDownloads() }
    private val packageStorage = PackageStorage(packagesDirectory)
    private val packageInstaller by lazy { PackageInstaller(packageStorage, PackageFileDownloader(httpDownloads)) }
    private val municipalityListSource by lazy { MunicipalityListSource(httpDownloads, IndexPublicKey.load()) }
    private val provinceDetailsSource by lazy { ProvinceDetailsSource(httpDownloads) }

    val installedPackages = InstalledPackages(packagesDirectory)

    val municipalityListLoader = MunicipalityListLoader { municipalityListSource.fetch() }

    val provinceDetailsLoader = ProvinceDetailsLoader { list, province -> provinceDetailsSource.fetch(list, province) }

    val packageDownloadWorkerFactory =
        PackageDownloadWorkerFactory { target, observer -> packageInstaller.install(target, observer) }

    val packageDownloads: PackageDownloads by lazy {
        WorkManagerPackageDownloads(WorkManager.getInstance(applicationContext), Dispatchers.IO)
    }

    // Lives as long as the process: what is saved in it is never cut by leaving a screen.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val shownMunicipality =
        ShownMunicipality(
            PreferenceDataStoreFactory.create(scope = applicationScope) {
                applicationContext.preferencesDataStoreFile(ShownMunicipality.FILE_NAME)
            },
            applicationScope,
        )

    /**
     * Emits once when the packages can be listed, after putting back any package whose replacement
     * was cut and discarding the downloads nobody will finish, and again each time a download
     * installs one. If WorkManager failed, the map would still show what is installed.
     */
    val installedPackagesChanges: Flow<Any?> =
        flow {
            packageStorage.finishInterruptedOperations()
            packageDownloads.packagesBeingDownloaded()?.let(packageStorage::discardDownloadsExcept)
            emit(Unit)
            packageDownloads.installedCount.catch { }.collect { installed -> emit(installed) }
        }.flowOn(Dispatchers.IO)

    val myMunicipalitiesSources =
        MyMunicipalitiesSources(
            listInstalled = installedPackages::list,
            uninstall = packageStorage::uninstall,
            show = shownMunicipality::show,
            shownPackageId = shownMunicipality.id,
            installedPackagesChanges = installedPackagesChanges,
        )

    companion object {
        // Also used by scripts/install-test-package.sh.
        const val MUNICIPAL_PACKAGES_DIRECTORY = "municipal-packages"
    }
}
