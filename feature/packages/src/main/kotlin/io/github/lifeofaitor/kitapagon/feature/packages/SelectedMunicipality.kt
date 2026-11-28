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
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets
import java.util.Locale

/** What the screen shows once a municipality is chosen: its size, then its download and its problems. */
@Composable
internal fun SelectionContent(
    selection: Selection,
    actions: ChooseMunicipalityActions,
) {
    val name = selection.match.municipality.names.official
    Text(
        stringResource(R.string.choose_municipality_and_province, name, selection.match.provinceName),
        style = MaterialTheme.typography.titleLarge,
    )
    when (selection) {
        is Selection.LoadingDetails -> Waiting(stringResource(R.string.choose_loading_details, name))
        is Selection.DetailsUnavailable -> DetailsUnavailable(selection.match, actions)
        is Selection.ReadyToDownload -> ReadyToDownload(selection, actions)
        is Selection.Downloading -> Downloading(selection.download, actions)
    }
}

@Composable
private fun DetailsUnavailable(
    match: MunicipalityMatch,
    actions: ChooseMunicipalityActions,
) {
    Text(stringResource(R.string.choose_details_failed, match.municipality.names.official))
    ActionButtons(
        primary = stringResource(R.string.choose_retry) to { actions.onChoose(match) },
        secondary = stringResource(R.string.choose_other_municipality) to actions.onChooseAnother,
    )
}

@Composable
private fun ReadyToDownload(
    selection: Selection.ReadyToDownload,
    actions: ChooseMunicipalityActions,
) {
    Text(stringResource(R.string.choose_download_size, megabytes(selection.target.totalBytes)))
    Text(stringResource(R.string.choose_privacy), style = MaterialTheme.typography.bodyMedium)
    ActionButtons(
        primary = stringResource(R.string.choose_download) to actions.onDownload,
        secondary = stringResource(R.string.choose_other_municipality) to actions.onChooseAnother,
    )
}

@Composable
private fun Downloading(
    download: DownloadState,
    actions: ChooseMunicipalityActions,
) {
    when (download) {
        DownloadState.Waiting -> InProgress(stringResource(R.string.choose_waiting), null, actions)
        is DownloadState.Running -> Running(download, actions)
        DownloadState.Installed -> Text(stringResource(R.string.choose_installed))
        is DownloadState.Failed -> Retry(failureText(download), actions)
        DownloadState.Cancelled -> Retry(stringResource(R.string.choose_cancelled), actions)
    }
}

@Composable
private fun Running(
    running: DownloadState.Running,
    actions: ChooseMunicipalityActions,
) {
    val progress = running.progress
    val text =
        if (progress == null) {
            stringResource(R.string.choose_downloading_starting)
        } else {
            stringResource(
                R.string.choose_downloading,
                megabytes(progress.downloadedBytes),
                megabytes(progress.totalBytes),
            )
        }
    InProgress(text, progress?.let { it.downloadedBytes.toFloat() / it.totalBytes }, actions)
}

@Composable
private fun InProgress(
    text: String,
    fraction: Float?,
    actions: ChooseMunicipalityActions,
) {
    Text(text)
    if (fraction == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    }
    OutlinedButton(onClick = actions.onCancelDownload, modifier = Modifier.heightIn(min = TouchTargets.minimum)) {
        Text(stringResource(R.string.choose_cancel_download))
    }
}

@Composable
private fun Retry(
    text: String,
    actions: ChooseMunicipalityActions,
) {
    Text(text, color = MaterialTheme.colorScheme.error)
    ActionButtons(
        primary = stringResource(R.string.choose_try_again) to actions.onDownload,
        secondary = stringResource(R.string.choose_other_municipality) to actions.onChooseAnother,
    )
}

@Composable
private fun ActionButtons(
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
) {
    Button(onClick = primary.second, modifier = Modifier.fillMaxWidth().heightIn(min = TouchTargets.minimum)) {
        Text(primary.first)
    }
    OutlinedButton(
        onClick = secondary.second,
        modifier = Modifier.fillMaxWidth().heightIn(min = TouchTargets.minimum),
    ) {
        Text(secondary.first)
    }
}
