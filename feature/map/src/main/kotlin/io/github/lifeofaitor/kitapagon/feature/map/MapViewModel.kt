package io.github.lifeofaitor.kitapagon.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackage
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileParser
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileResult
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderReader
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderResult
import io.github.lifeofaitor.kitapagon.core.packageformat.shownPackage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

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
        val places: List<Place> = emptyList(),
        /** The map still works when its places file is broken; the screen says so. */
        val arePlacesDamaged: Boolean = false,
        /** The official name of the municipality, or the identifier of a package installed by hand. */
        val name: String = packageId.value,
    ) : MapScreenState
}

enum class MapViewMode {
    MAP,
    LIST,
}

/** What the person is doing with the places: map or list, which place is open, which type is filtered. */
data class PlacesInteraction(
    val viewMode: MapViewMode = MapViewMode.MAP,
    val selectedPlace: Place? = null,
    val listCategory: PlaceCategory? = null,
)

/**
 * Shows the municipality the person chose, or the first installed one if that one is not installed
 * (decision 0017). It loads it again each time [installedPackagesChanges] emits, which happens first
 * when the packages can be listed and then each time a download installs one, and each time
 * [shownPackageId] changes.
 */
class MapViewModel(
    private val installedPackages: InstalledPackages,
    private val ioDispatcher: CoroutineDispatcher,
    installedPackagesChanges: Flow<Any?> = flowOf(Unit),
    shownPackageId: Flow<PackageId?> = flowOf(null),
) : ViewModel() {
    private val mutableState = MutableStateFlow<MapScreenState>(MapScreenState.Loading)
    val state: StateFlow<MapScreenState> = mutableState.asStateFlow()

    private val mutableInteraction = MutableStateFlow(PlacesInteraction())
    val interaction: StateFlow<PlacesInteraction> = mutableInteraction.asStateFlow()

    init {
        viewModelScope.launch {
            combine(installedPackagesChanges, shownPackageId) { _, shownId -> shownId }.collectLatest { shownId ->
                show(withContext(ioDispatcher) { loadPackage(shownId) })
            }
        }
    }

    // The place open and the filter of the list belong to the municipality that was shown.
    private fun show(newState: MapScreenState) {
        val previousId = (mutableState.value as? MapScreenState.Ready)?.packageId
        if (newState is MapScreenState.Ready && newState.packageId != previousId) {
            mutableInteraction.value = PlacesInteraction()
        }
        mutableState.value = newState
    }

    fun showMap() = mutableInteraction.update { it.copy(viewMode = MapViewMode.MAP) }

    fun showList() = mutableInteraction.update { it.copy(viewMode = MapViewMode.LIST, selectedPlace = null) }

    fun selectPlace(place: Place) = mutableInteraction.update { it.copy(selectedPlace = place) }

    fun clearSelectedPlace() = mutableInteraction.update { it.copy(selectedPlace = null) }

    /** Opens a place chosen in the list on the map, centred and with its details open. */
    fun showPlaceOnMap(place: Place) =
        mutableInteraction.update { it.copy(viewMode = MapViewMode.MAP, selectedPlace = place) }

    fun filterListBy(category: PlaceCategory?) = mutableInteraction.update { it.copy(listCategory = category) }

    private fun loadPackage(shownId: PackageId?): MapScreenState {
        val shown = installedPackages.list().shownPackage(shownId) ?: return MapScreenState.NoPackageInstalled
        return when (val headerResult = PmtilesHeaderReader.read(shown.mapFile)) {
            is PmtilesHeaderResult.Valid -> readyStateOf(shown, headerResult.header.bounds)
            is PmtilesHeaderResult.Invalid -> MapScreenState.PackageDamaged(shown.id)
        }
    }

    private fun readyStateOf(
        installedPackage: InstalledPackage,
        bounds: GeographicBounds,
    ): MapScreenState.Ready {
        val name = installedPackage.info?.names?.official ?: installedPackage.id.value
        val ready = MapScreenState.Ready(installedPackage.id, installedPackage.mapFile, bounds, name = name)
        if (!installedPackage.placesFile.isFile) return ready
        val placesResult = installedPackage.placesFile.readTextOrNull()?.let(PlacesFileParser::parse)
        return if (placesResult is PlacesFileResult.Valid) {
            ready.copy(places = placesResult.placesFile.places)
        } else {
            ready.copy(arePlacesDamaged = true)
        }
    }

    private fun File.readTextOrNull(): String? =
        try {
            readText()
        } catch (expectedWhenTheFileCannotBeRead: IOException) {
            null
        }
}
