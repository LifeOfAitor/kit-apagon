package io.github.lifeofaitor.kitapagon.feature.packages

import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfoWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.installedPackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Replacing the main dispatcher is still marked experimental, but it is the documented way to test
// code that runs in viewModelScope.
@OptIn(ExperimentalCoroutinesApi::class)
class MyMunicipalitiesViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val irun = PackageId.parseOrNull("ES-20045")!!
    private val bilbao = PackageId.parseOrNull("ES-48020")!!
    private val shownId = MutableStateFlow<PackageId?>(null)
    private val shownRequests = mutableListOf<PackageId>()
    private var deletionWorks = true

    @Before
    fun useTestDispatcherAsMain() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun listsEachMunicipalityWithItsProvinceItsSizeAndWhichOneTheMapShows() =
        runTest(testDispatcher) {
            install(irun, mapBytes = 3_000_000, withInfo = true)
            install(bilbao, mapBytes = 1_000)
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    InstalledMunicipality(irun, "Irun", "Gipuzkoa", 3_000_000L + PLACES_BYTES, isShown = true),
                    InstalledMunicipality(bilbao, "ES-48020", null, 1_000L + PLACES_BYTES, isShown = false),
                ),
                viewModel.state.value.municipalities,
            )
        }

    @Test
    fun followsTheMunicipalityChosenForTheMap() =
        runTest(testDispatcher) {
            install(irun)
            install(bilbao)
            val viewModel = createViewModel()

            viewModel.show(InstalledMunicipality(bilbao, "ES-48020", null, 0, isShown = false))
            advanceUntilIdle()

            assertEquals(listOf(bilbao), shownRequests)
            assertEquals(
                listOf(false, true),
                viewModel.state.value.municipalities
                    ?.map { it.isShown },
            )
        }

    @Test
    fun deletesOnlyAfterTheDeletionIsConfirmed() =
        runTest(testDispatcher) {
            install(irun)
            install(bilbao)
            val viewModel = createViewModel().also { advanceUntilIdle() }
            val irunRow =
                viewModel.state.value.municipalities!!
                    .first()

            viewModel.askToDelete(irunRow)
            viewModel.cancelDeletion()
            assertNull(viewModel.state.value.pendingDeletion)
            viewModel.askToDelete(irunRow)
            assertEquals(irunRow, viewModel.state.value.pendingDeletion)
            viewModel.confirmDeletion()
            advanceUntilIdle()

            assertEquals(
                listOf(bilbao),
                viewModel.state.value.municipalities
                    ?.map { it.id },
            )
            assertNull(viewModel.state.value.pendingDeletion)
            assertFalse(viewModel.state.value.deletionFailed)
        }

    @Test
    fun saysWhenTheMunicipalityCouldNotBeDeleted() =
        runTest(testDispatcher) {
            install(irun)
            deletionWorks = false
            val viewModel = createViewModel().also { advanceUntilIdle() }

            viewModel.askToDelete(
                viewModel.state.value.municipalities!!
                    .single(),
            )
            viewModel.confirmDeletion()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.deletionFailed)
            assertEquals(
                listOf(irun),
                viewModel.state.value.municipalities
                    ?.map { it.id },
            )
        }

    @Test
    fun withoutAMunicipalityToDeleteConfirmingDoesNothing() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.confirmDeletion()
            advanceUntilIdle()

            assertEquals(emptyList<InstalledMunicipality>(), viewModel.state.value.municipalities)
        }

    private fun createViewModel(): MyMunicipalitiesViewModel {
        val installedPackages = InstalledPackages(temporaryFolder.root)
        return MyMunicipalitiesViewModel(
            MyMunicipalitiesSources(
                listInstalled = installedPackages::list,
                uninstall = { id -> deletionWorks && File(temporaryFolder.root, id.value).deleteRecursively() },
                show = { id ->
                    shownRequests += id
                    shownId.value = id
                },
                shownPackageId = shownId,
                installedPackagesChanges = flowOf(Unit),
            ),
            testDispatcher,
        )
    }

    private fun install(
        id: PackageId,
        mapBytes: Int = 100,
        withInfo: Boolean = false,
    ) {
        val directory = File(temporaryFolder.root, id.value).apply { mkdirs() }
        File(directory, InstalledPackages.MAP_FILE_NAME).writeBytes(ByteArray(mapBytes))
        File(directory, InstalledPackages.PLACES_FILE_NAME).writeBytes(ByteArray(PLACES_BYTES.toInt()))
        if (withInfo) {
            File(
                directory,
                InstalledPackages.INFO_FILE_NAME,
            ).writeText(InstalledPackageInfoWriter.write(installedPackageInfo()))
        }
    }

    private companion object {
        const val PLACES_BYTES = 10L
    }
}
