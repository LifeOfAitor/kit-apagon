package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityMatch
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets
import java.util.Locale

@Composable
fun ChooseMunicipalityScreen(
    viewModel: ChooseMunicipalityViewModel,
    onInstalled: (PackageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloading = state.selection as? Selection.Downloading
    val installedId =
        downloading
            ?.target
            ?.info
            ?.id
            ?.takeIf { downloading.download == DownloadState.Installed }
    LaunchedEffect(installedId) { installedId?.let(onInstalled) }
    ChooseMunicipalityContent(state, ChooseMunicipalityActions.of(viewModel), modifier)
}

/** Everything the person can do on the screen. */
internal data class ChooseMunicipalityActions(
    val onRetryList: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onChoose: (MunicipalityMatch) -> Unit,
    val onChooseAnother: () -> Unit,
    val onDownload: () -> Unit,
    val onCancelDownload: () -> Unit,
) {
    companion object {
        fun of(viewModel: ChooseMunicipalityViewModel) =
            ChooseMunicipalityActions(
                onRetryList = viewModel::loadList,
                onQueryChange = viewModel::search,
                onChoose = viewModel::choose,
                onChooseAnother = viewModel::chooseAnother,
                onDownload = viewModel::download,
                onCancelDownload = viewModel::cancelDownload,
            )
    }
}

@Composable
internal fun ChooseMunicipalityContent(
    state: ChooseMunicipalityState,
    actions: ChooseMunicipalityActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(stringResource(R.string.choose_title), style = MaterialTheme.typography.headlineSmall)
            when (val list = state.list) {
                ListState.Loading -> Waiting(stringResource(R.string.choose_loading_list))
                is ListState.Unavailable -> Problem(listProblemText(list.problem), actions.onRetryList)
                is ListState.Ready -> SearchOrSelection(state, actions)
            }
        }
    }
}

@Composable
private fun SearchOrSelection(
    state: ChooseMunicipalityState,
    actions: ChooseMunicipalityActions,
) {
    val selection = state.selection
    if (selection == null) {
        Search(state.query, state.matches, actions)
    } else {
        // While a download runs, back leaves the screen and the download goes on in the background.
        BackHandler(enabled = !selection.isDownloadInProgress()) { actions.onChooseAnother() }
        SelectionContent(selection, actions)
    }
}

@Composable
private fun Search(
    query: String,
    matches: List<MunicipalityMatch>,
    actions: ChooseMunicipalityActions,
) {
    OutlinedTextField(
        value = query,
        onValueChange = actions.onQueryChange,
        label = { Text(stringResource(R.string.choose_search_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth(),
    )
    when {
        query.isBlank() -> Text(stringResource(R.string.choose_type_to_search))
        matches.isEmpty() -> Text(stringResource(R.string.choose_no_matches))
        else -> Matches(matches, actions.onChoose)
    }
}

@Composable
private fun Matches(
    matches: List<MunicipalityMatch>,
    onChoose: (MunicipalityMatch) -> Unit,
) {
    LazyColumn {
        items(matches, key = { it.municipality.id.value }) { match ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TouchTargets.minimum)
                        .clickable { onChoose(match) }
                        .padding(vertical = Spacing.small),
            ) {
                Text(match.municipality.names.official, style = MaterialTheme.typography.bodyLarge)
                Text(
                    match.provinceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

private fun Selection.isDownloadInProgress(): Boolean =
    this is Selection.Downloading && (download == DownloadState.Waiting || download is DownloadState.Running)
