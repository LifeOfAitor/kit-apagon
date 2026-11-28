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

@Composable
internal fun Waiting(text: String) {
    CircularProgressIndicator()
    Text(text)
}

@Composable
internal fun Problem(
    text: String,
    onRetry: () -> Unit,
) {
    Text(text)
    Button(onClick = onRetry, modifier = Modifier.heightIn(min = TouchTargets.minimum)) {
        Text(stringResource(R.string.choose_retry))
    }
}

@Composable
internal fun listProblemText(problem: ListProblem): String =
    when (problem) {
        ListProblem.NO_CONNECTION -> stringResource(R.string.choose_list_no_connection)
        ListProblem.SERVER -> stringResource(R.string.choose_list_server)
        ListProblem.NOT_SIGNED -> stringResource(R.string.choose_list_not_signed)
        ListProblem.APP_OUTDATED -> stringResource(R.string.choose_list_app_outdated)
    }

@Composable
internal fun failureText(failed: DownloadState.Failed): String =
    when (failed.problem) {
        DownloadProblem.NO_CONNECTION -> {
            stringResource(R.string.choose_failed_no_connection)
        }

        DownloadProblem.SERVER -> {
            stringResource(R.string.choose_failed_server)
        }

        DownloadProblem.DAMAGED -> {
            stringResource(R.string.choose_failed_damaged)
        }

        DownloadProblem.STORAGE -> {
            stringResource(R.string.choose_failed_storage)
        }

        DownloadProblem.NOT_ENOUGH_SPACE -> {
            stringResource(R.string.choose_failed_not_enough_space, megabytes(failed.neededBytes ?: 0))
        }
    }

private val SPANISH = Locale.forLanguageTag("es-ES")
private const val BYTES_PER_MEGABYTE = 1_000_000.0

/** Sizes as people read them in Spain, such as 3,6 MB. */
internal fun megabytes(bytes: Long): String = String.format(SPANISH, "%.1f MB", bytes / BYTES_PER_MEGABYTE)
