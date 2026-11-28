package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadFailure
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadablePackage
import io.github.lifeofaitor.kitapagon.core.packagedownload.MunicipalityListFetch
import io.github.lifeofaitor.kitapagon.core.packagedownload.ProvinceDetailsFetch
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidIndexReason
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityMatch
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalitySearch
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Downloads the signed list of municipalities; MunicipalityListSource in the app. */
fun interface MunicipalityListLoader {
    fun load(): MunicipalityListFetch
}

/** Downloads the details of a province; ProvinceDetailsSource in the app. */
fun interface ProvinceDetailsLoader {
    fun load(
        list: MunicipalityList,
        province: ProvinceEntry,
    ): ProvinceDetailsFetch
}

enum class ListProblem {
    NO_CONNECTION,
    SERVER,
    NOT_SIGNED,
    APP_OUTDATED,
}

sealed interface ListState {
    data object Loading : ListState

    data class Unavailable(
        val problem: ListProblem,
    ) : ListState

    data class Ready(
        val list: MunicipalityList,
    ) : ListState
}

/** The municipality the person chose, from its details to its installation. */
sealed interface Selection {
    val match: MunicipalityMatch

    data class LoadingDetails(
        override val match: MunicipalityMatch,
    ) : Selection

    data class DetailsUnavailable(
        override val match: MunicipalityMatch,
    ) : Selection

    data class ReadyToDownload(
        override val match: MunicipalityMatch,
        val target: DownloadablePackage,
    ) : Selection

    data class Downloading(
        override val match: MunicipalityMatch,
        val target: DownloadablePackage,
        val download: DownloadState,
    ) : Selection
}

data class ChooseMunicipalityState(
    val list: ListState = ListState.Loading,
    val query: String = "",
    val matches: List<MunicipalityMatch> = emptyList(),
    val selection: Selection? = null,
)

/**
 * Finds the municipality of the person and downloads its package (decision 0017). The download goes
 * on in the background if the person leaves the screen.
 */
class ChooseMunicipalityViewModel(
    private val listLoader: MunicipalityListLoader,
    private val detailsLoader: ProvinceDetailsLoader,
    private val downloads: PackageDownloads,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChooseMunicipalityState())
    val state: StateFlow<ChooseMunicipalityState> = mutableState.asStateFlow()

    private var search: MunicipalitySearch? = null
    private var searchJob: Job? = null
    private var followJob: Job? = null
    private var downloadJob: UUID? = null

    init {
        loadList()
    }

    fun loadList() {
        mutableState.update { it.copy(list = ListState.Loading) }
        viewModelScope.launch {
            val listState = withContext(ioDispatcher) { listStateOf(listLoader.load()) }
            mutableState.update { it.copy(list = listState) }
            if (listState is ListState.Ready) {
                search = withContext(ioDispatcher) { MunicipalitySearch(listState.list) }
                search(state.value.query)
            }
        }
    }

    fun search(query: String) {
        mutableState.update { it.copy(query = query) }
        val currentSearch = search ?: return
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                val matches = withContext(ioDispatcher) { currentSearch.search(query) }
                mutableState.update { if (it.query == query) it.copy(matches = matches) else it }
            }
    }

    fun choose(match: MunicipalityMatch) {
        val list = (state.value.list as? ListState.Ready)?.list ?: return
        mutableState.update { it.copy(selection = Selection.LoadingDetails(match)) }
        viewModelScope.launch {
            val selection = withContext(ioDispatcher) { selectionOf(match, list) }
            mutableState.update {
                if (it.selection ==
                    Selection.LoadingDetails(match)
                ) {
                    it.copy(selection = selection)
                } else {
                    it
                }
            }
        }
    }

    /** Back to the search; a download that already started goes on in the background. */
    fun chooseAnother() {
        followJob?.cancel()
        mutableState.update { it.copy(selection = null) }
    }

    /** Starts the download of the chosen package, or starts it again after a failure. */
    fun download() {
        val selection = state.value.selection ?: return
        val target = selection.target ?: return
        mutableState.update {
            it.copy(
                selection = Selection.Downloading(selection.match, target, DownloadState.Waiting),
            )
        }
        followJob?.cancel()
        followJob = viewModelScope.launch { follow(selection.match, target, downloads.start(target)) }
    }

    fun cancelDownload() {
        downloadJob?.let(downloads::cancel)
    }

    private suspend fun follow(
        match: MunicipalityMatch,
        target: DownloadablePackage,
        job: UUID,
    ) {
        downloadJob = job
        downloads.stateOf(job).filterNotNull().collect { download ->
            mutableState.update { it.copy(selection = Selection.Downloading(match, target, download)) }
        }
    }

    private fun selectionOf(
        match: MunicipalityMatch,
        list: MunicipalityList,
    ): Selection {
        val province = list.provinceOf(match.municipality) ?: return Selection.DetailsUnavailable(match)
        val fetched = detailsLoader.load(list, province)
        val details =
            (fetched as? ProvinceDetailsFetch.Valid)?.details?.packages?.firstOrNull {
                it.id ==
                    match.municipality.id
            }
        return details?.let { Selection.ReadyToDownload(match, DownloadablePackage.of(list, province, it)) }
            ?: Selection.DetailsUnavailable(match)
    }
}

/** The package of the selection, once its details have arrived. */
private val Selection.target: DownloadablePackage?
    get() =
        when (this) {
            is Selection.ReadyToDownload -> target
            is Selection.Downloading -> target
            else -> null
        }

private fun listStateOf(fetched: MunicipalityListFetch): ListState =
    when (fetched) {
        is MunicipalityListFetch.Valid -> {
            ListState.Ready(fetched.list)
        }

        MunicipalityListFetch.NotSigned -> {
            ListState.Unavailable(ListProblem.NOT_SIGNED)
        }

        is MunicipalityListFetch.Invalid -> {
            val isNewer = fetched.reason == InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION
            ListState.Unavailable(if (isNewer) ListProblem.APP_OUTDATED else ListProblem.SERVER)
        }

        is MunicipalityListFetch.Failed -> {
            val isOffline = fetched.failure == DownloadFailure.NETWORK
            ListState.Unavailable(if (isOffline) ListProblem.NO_CONNECTION else ListProblem.SERVER)
        }
    }
