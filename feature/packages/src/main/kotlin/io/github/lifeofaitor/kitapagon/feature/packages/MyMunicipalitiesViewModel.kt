package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackage
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.shownPackage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A municipality downloaded on the phone, as the list shows it. */
data class InstalledMunicipality(
    val id: PackageId,
    /** The official name, or the identifier of a package installed by hand. */
    val name: String,
    val provinceName: String?,
    val sizeBytes: Long,
    val isShown: Boolean,
)

data class MyMunicipalitiesState(
    /** Null while the list is read. */
    val municipalities: List<InstalledMunicipality>? = null,
    /** The municipality the person asked to delete, until they confirm or cancel. */
    val pendingDeletion: InstalledMunicipality? = null,
    val deletionFailed: Boolean = false,
)

/** The things the list needs from the app, wired by hand (decision 0008). */
class MyMunicipalitiesSources(
    val listInstalled: () -> List<InstalledPackage>,
    val uninstall: (PackageId) -> Boolean,
    val show: (PackageId) -> Unit,
    val shownPackageId: Flow<PackageId?>,
    val installedPackagesChanges: Flow<Any?>,
)

/**
 * The municipalities downloaded on the phone: which one the map shows, and deleting the ones that are
 * no longer needed (decision 0017).
 */
class MyMunicipalitiesViewModel(
    private val sources: MyMunicipalitiesSources,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MyMunicipalitiesState())
    val state: StateFlow<MyMunicipalitiesState> = mutableState.asStateFlow()

    // Changes after each deletion, so the list is read again.
    private val deletions = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(sources.installedPackagesChanges, sources.shownPackageId, deletions) { _, shownId, _ -> shownId }
                .collectLatest { shownId ->
                    val municipalities =
                        withContext(ioDispatcher) { municipalitiesOf(sources.listInstalled(), shownId) }
                    mutableState.update { it.copy(municipalities = municipalities) }
                }
        }
    }

    fun show(municipality: InstalledMunicipality) = sources.show(municipality.id)

    fun askToDelete(municipality: InstalledMunicipality) =
        mutableState.update { it.copy(pendingDeletion = municipality, deletionFailed = false) }

    fun cancelDeletion() = mutableState.update { it.copy(pendingDeletion = null) }

    fun confirmDeletion() {
        val municipality = state.value.pendingDeletion ?: return
        mutableState.update { it.copy(pendingDeletion = null) }
        viewModelScope.launch {
            val deleted = withContext(ioDispatcher) { sources.uninstall(municipality.id) }
            mutableState.update { it.copy(deletionFailed = !deleted) }
            deletions.update { it + 1 }
        }
    }

    private fun municipalitiesOf(
        installed: List<InstalledPackage>,
        shownId: PackageId?,
    ): List<InstalledMunicipality> {
        val shown = installed.shownPackage(shownId)
        return installed.map { installedPackage ->
            InstalledMunicipality(
                id = installedPackage.id,
                name = installedPackage.info?.names?.official ?: installedPackage.id.value,
                provinceName = installedPackage.info?.provinceName,
                sizeBytes = installedPackage.mapFile.length() + installedPackage.placesFile.length(),
                isShown = installedPackage == shown,
            )
        }
    }
}
