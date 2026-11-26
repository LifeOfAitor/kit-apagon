package io.github.lifeofaitor.kitapagon.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderReader
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface MapScreenState {
    data object Loading : MapScreenState

    data object NoPackageInstalled : MapScreenState

    data class PackageDamaged(
        val packageId: PackageId,
    ) : MapScreenState

    data class Ready(
        val packageId: PackageId,
        val mapFile: File,
        val bounds: GeographicBounds,
    ) : MapScreenState
}

/**
 * Shows the first installed package. Choosing among several packages arrives with the package
 * download, the third step of the map in decision 0010.
 */
class MapViewModel(
    private val installedPackages: InstalledPackages,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow<MapScreenState>(MapScreenState.Loading)
    val state: StateFlow<MapScreenState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.value = withContext(ioDispatcher) { loadFirstInstalledPackage() }
        }
    }

    private fun loadFirstInstalledPackage(): MapScreenState {
        val firstPackage = installedPackages.list().firstOrNull() ?: return MapScreenState.NoPackageInstalled
        return when (val headerResult = PmtilesHeaderReader.read(firstPackage.mapFile)) {
            is PmtilesHeaderResult.Valid -> {
                MapScreenState.Ready(firstPackage.id, firstPackage.mapFile, headerResult.header.bounds)
            }

            is PmtilesHeaderResult.Invalid -> {
                MapScreenState.PackageDamaged(firstPackage.id)
            }
        }
    }
}
