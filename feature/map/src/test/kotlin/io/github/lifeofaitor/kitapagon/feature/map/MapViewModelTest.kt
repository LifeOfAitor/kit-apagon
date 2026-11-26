package io.github.lifeofaitor.kitapagon.feature.map

import app.cash.turbine.test
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.validHeader
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.HOSPITAL
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.IRUN
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.IRUN_AREA
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.PHARMACY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Replacing the main dispatcher is still marked experimental, but it is the documented way to test
// code that runs in viewModelScope.
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun useTestDispatcherAsMain() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsThatNoPackageIsInstalled() =
        runTest(testDispatcher) {
            createViewModel().state.test {
                assertEquals(MapScreenState.Loading, awaitItem())
                assertEquals(MapScreenState.NoPackageInstalled, awaitItem())
            }
        }

    @Test
    fun showsTheInstalledPackageWithTheBoundsFromItsHeader() =
        runTest(testDispatcher) {
            val mapFile = installMap(validHeader(IRUN_AREA))

            createViewModel().state.test {
                assertEquals(MapScreenState.Loading, awaitItem())
                assertEquals(MapScreenState.Ready(IRUN, mapFile, IRUN_AREA), awaitItem())
            }
        }

    @Test
    fun reportsADamagedPackageInsteadOfShowingIt() =
        runTest(testDispatcher) {
            installMap("not a map".toByteArray())

            createViewModel().state.test {
                assertEquals(MapScreenState.Loading, awaitItem())
                assertEquals(MapScreenState.PackageDamaged(IRUN), awaitItem())
            }
        }

    @Test
    fun loadsThePlacesOfThePackage() =
        runTest(testDispatcher) {
            installMap(validHeader(IRUN_AREA))
            installPlaces(PlacesFileWriter.write(listOf(PHARMACY, HOSPITAL)))

            val ready = awaitReadyState()

            assertEquals(listOf(PHARMACY, HOSPITAL), ready.places)
            assertEquals(false, ready.arePlacesDamaged)
        }

    @Test
    fun keepsTheMapAndWarnsWhenThePlacesFileIsDamaged() =
        runTest(testDispatcher) {
            installMap(validHeader(IRUN_AREA))
            installPlaces("{ this is not json")

            val ready = awaitReadyState()

            assertEquals(emptyList<Any>(), ready.places)
            assertEquals(true, ready.arePlacesDamaged)
        }

    @Test
    fun openingTheListClosesTheDetailsOfThePlace() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.selectPlace(PHARMACY)

            viewModel.showList()

            assertEquals(PlacesInteraction(viewMode = MapViewMode.LIST), viewModel.interaction.value)
        }

    @Test
    fun aPlaceChosenInTheListOpensOnTheMap() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.showList()
            viewModel.filterListBy(PlaceCategory.PHARMACY)

            viewModel.showPlaceOnMap(PHARMACY)

            val expected =
                PlacesInteraction(MapViewMode.MAP, selectedPlace = PHARMACY, listCategory = PlaceCategory.PHARMACY)
            assertEquals(expected, viewModel.interaction.value)
        }

    @Test
    fun closingTheDetailsKeepsTheMapOpen() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.selectPlace(PHARMACY)

            viewModel.clearSelectedPlace()
            viewModel.showMap()

            assertEquals(PlacesInteraction(), viewModel.interaction.value)
        }

    private suspend fun awaitReadyState(): MapScreenState.Ready {
        var ready: MapScreenState.Ready? = null
        createViewModel().state.test {
            assertEquals(MapScreenState.Loading, awaitItem())
            ready = awaitItem() as MapScreenState.Ready
        }
        return checkNotNull(ready)
    }

    private fun createViewModel(): MapViewModel =
        MapViewModel(InstalledPackages(temporaryFolder.root), ioDispatcher = testDispatcher)

    private fun packageDirectory(): File = File(temporaryFolder.root, IRUN.value).apply { mkdirs() }

    private fun installMap(mapContent: ByteArray): File =
        File(packageDirectory(), InstalledPackages.MAP_FILE_NAME).apply { writeBytes(mapContent) }

    private fun installPlaces(placesContent: String) {
        File(packageDirectory(), InstalledPackages.PLACES_FILE_NAME).writeText(placesContent)
    }
}
