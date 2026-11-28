package io.github.lifeofaitor.kitapagon.feature.packages

import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadFailure
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packagedownload.MunicipalityListFetch
import io.github.lifeofaitor.kitapagon.core.packagedownload.ProvinceDetailsFetch
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidIndexReason
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.provinceDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Replacing the main dispatcher is still marked experimental, but it is the documented way to test
// code that runs in viewModelScope.
@OptIn(ExperimentalCoroutinesApi::class)
class ChooseMunicipalityViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val list = municipalityList()
    private var listFetch: MunicipalityListFetch = MunicipalityListFetch.Valid(list)
    private var detailsFetch: ProvinceDetailsFetch = ProvinceDetailsFetch.Valid(provinceDetails())
    private val downloads = FakePackageDownloads()

    @Before
    fun useTestDispatcherAsMain() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun explainsWhyTheListCannotBeUsedAndLoadsItAgain() =
        runTest(testDispatcher) {
            val cases =
                mapOf(
                    MunicipalityListFetch.Failed(DownloadFailure.NETWORK) to ListProblem.NO_CONNECTION,
                    MunicipalityListFetch.Failed(DownloadFailure.DAMAGED) to ListProblem.SERVER,
                    MunicipalityListFetch.NotSigned to ListProblem.NOT_SIGNED,
                    MunicipalityListFetch.Invalid(InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION) to
                        ListProblem.APP_OUTDATED,
                    MunicipalityListFetch.Invalid(InvalidIndexReason.NOT_JSON) to ListProblem.SERVER,
                )
            cases.forEach { (fetched, problem) ->
                listFetch = fetched
                val viewModel = createViewModel()
                advanceUntilIdle()
                assertEquals(ListState.Unavailable(problem), viewModel.state.value.list)
            }
            listFetch = MunicipalityListFetch.Valid(list)
            val viewModel = createViewModel().also { advanceUntilIdle() }
            viewModel.loadList()
            advanceUntilIdle()
            assertEquals(ListState.Ready(list), viewModel.state.value.list)
        }

    @Test
    fun searchesWhatIsTypedEvenBeforeTheListArrives() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.search("irún")
            advanceUntilIdle()

            assertEquals(listOf(IRUN_MATCH), viewModel.state.value.matches)
            viewModel.search("bilbao")
            advanceUntilIdle()
            assertEquals(emptyList<Any>(), viewModel.state.value.matches)
        }

    @Test
    fun choosingAMunicipalityGivesTheSizeOfItsPackage() =
        runTest(testDispatcher) {
            val viewModel = createViewModel().also { advanceUntilIdle() }

            viewModel.choose(IRUN_MATCH)
            assertEquals(Selection.LoadingDetails(IRUN_MATCH), viewModel.state.value.selection)
            advanceUntilIdle()

            val ready = viewModel.state.value.selection as Selection.ReadyToDownload
            assertEquals(3_501_019L + 54_792L, ready.target.totalBytes)
        }

    @Test
    fun detailsThatCannotBeDownloadedLeaveTheChoiceToTryAgain() =
        runTest(testDispatcher) {
            detailsFetch = ProvinceDetailsFetch.Failed(DownloadFailure.NETWORK)
            val viewModel = createViewModel().also { advanceUntilIdle() }

            viewModel.choose(IRUN_MATCH)
            advanceUntilIdle()

            assertEquals(Selection.DetailsUnavailable(IRUN_MATCH), viewModel.state.value.selection)
        }

    @Test
    fun followsTheDownloadUntilThePackageIsInstalled() =
        runTest(testDispatcher) {
            val viewModel = readyToDownload()

            viewModel.download()
            advanceUntilIdle()
            assertEquals(listOf(downloadableIrun()), downloads.started)
            assertEquals(DownloadState.Waiting, downloadState(viewModel))
            downloads.moveLastJobTo(DownloadState.Running(DownloadProgress(1_000_000, 3_555_811)))
            advanceUntilIdle()
            assertEquals(DownloadState.Running(DownloadProgress(1_000_000, 3_555_811)), downloadState(viewModel))
            downloads.moveLastJobTo(DownloadState.Installed)
            advanceUntilIdle()
            assertEquals(DownloadState.Installed, downloadState(viewModel))
        }

    @Test
    fun cancelsTheJobItFollowsAndCanStartAgain() =
        runTest(testDispatcher) {
            val viewModel = readyToDownload()
            viewModel.download()
            advanceUntilIdle()
            val firstJob = downloads.lastJob

            viewModel.cancelDownload()
            downloads.moveLastJobTo(DownloadState.Cancelled)
            advanceUntilIdle()
            viewModel.download()
            advanceUntilIdle()

            assertEquals(listOf(firstJob), downloads.cancelled)
            assertEquals(2, downloads.started.size)
            assertEquals(DownloadState.Waiting, downloadState(viewModel))
        }

    @Test
    fun choosingAnotherGoesBackToTheSearchWithWhatWasTyped() =
        runTest(testDispatcher) {
            val viewModel = readyToDownload()

            viewModel.chooseAnother()

            assertNull(viewModel.state.value.selection)
            assertEquals("irun", viewModel.state.value.query)
        }

    @Test
    fun withoutAListOrASelectionThereIsNothingToChooseOrDownload() =
        runTest(testDispatcher) {
            listFetch = MunicipalityListFetch.NotSigned
            val viewModel = createViewModel().also { advanceUntilIdle() }

            viewModel.choose(IRUN_MATCH)
            viewModel.download()
            viewModel.cancelDownload()

            assertNull(viewModel.state.value.selection)
            assertEquals(emptyList<Any>(), downloads.started)
        }

    private fun kotlinx.coroutines.test.TestScope.readyToDownload(): ChooseMunicipalityViewModel =
        createViewModel().also { viewModel ->
            advanceUntilIdle()
            viewModel.search("irun")
            viewModel.choose(IRUN_MATCH)
            advanceUntilIdle()
        }

    private fun downloadState(viewModel: ChooseMunicipalityViewModel): DownloadState? =
        (viewModel.state.value.selection as? Selection.Downloading)?.download

    private fun createViewModel(): ChooseMunicipalityViewModel =
        ChooseMunicipalityViewModel(
            listLoader = { listFetch },
            detailsLoader = { _, _ -> detailsFetch },
            downloads = downloads,
            ioDispatcher = testDispatcher,
        )
}
