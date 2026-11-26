package io.github.lifeofaitor.kitapagon.feature.map

import app.cash.turbine.test
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.validHeader
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
            val mapFile = installPackage("ES-20045", validHeader(IRUN_AREA))

            createViewModel().state.test {
                assertEquals(MapScreenState.Loading, awaitItem())
                assertEquals(
                    MapScreenState.Ready(
                        packageId = PackageId.parseOrNull("ES-20045")!!,
                        mapFile = mapFile,
                        bounds = IRUN_AREA,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun reportsADamagedPackageInsteadOfShowingIt() =
        runTest(testDispatcher) {
            installPackage("ES-20045", "not a map".toByteArray())

            createViewModel().state.test {
                assertEquals(MapScreenState.Loading, awaitItem())
                assertEquals(MapScreenState.PackageDamaged(PackageId.parseOrNull("ES-20045")!!), awaitItem())
            }
        }

    private fun createViewModel(): MapViewModel =
        MapViewModel(InstalledPackages(temporaryFolder.root), ioDispatcher = testDispatcher)

    private fun installPackage(
        packageId: String,
        mapContent: ByteArray,
    ): File {
        val packageDirectory = temporaryFolder.newFolder(packageId)
        return File(packageDirectory, InstalledPackages.MAP_FILE_NAME).apply { writeBytes(mapContent) }
    }

    private companion object {
        val IRUN_AREA = GeographicBounds(west = -1.9, south = 43.2, east = -1.6, north = 43.4)
    }
}
