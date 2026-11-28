package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets

@Composable
fun MyMunicipalitiesScreen(
    viewModel: MyMunicipalitiesViewModel,
    onShown: () -> Unit,
    onAddMunicipality: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions =
        MyMunicipalitiesActions(
            onShow = { municipality ->
                viewModel.show(municipality)
                onShown()
            },
            onAskToDelete = viewModel::askToDelete,
            onConfirmDeletion = viewModel::confirmDeletion,
            onCancelDeletion = viewModel::cancelDeletion,
            onAddMunicipality = onAddMunicipality,
        )
    MyMunicipalitiesContent(state, actions, modifier)
}

/** Everything the person can do on the list of municipalities. */
internal data class MyMunicipalitiesActions(
    val onShow: (InstalledMunicipality) -> Unit,
    val onAskToDelete: (InstalledMunicipality) -> Unit,
    val onConfirmDeletion: () -> Unit,
    val onCancelDeletion: () -> Unit,
    val onAddMunicipality: () -> Unit,
)

@Composable
internal fun MyMunicipalitiesContent(
    state: MyMunicipalitiesState,
    actions: MyMunicipalitiesActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(stringResource(R.string.my_municipalities_title), style = MaterialTheme.typography.headlineSmall)
            if (state.deletionFailed) {
                Text(stringResource(R.string.my_municipalities_delete_failed), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = actions.onAddMunicipality,
                modifier = Modifier.fillMaxWidth().heightIn(min = TouchTargets.minimum),
            ) { Text(stringResource(R.string.my_municipalities_add)) }
            when (val municipalities = state.municipalities) {
                null -> Waiting(stringResource(R.string.my_municipalities_loading))
                emptyList<InstalledMunicipality>() -> Text(stringResource(R.string.my_municipalities_empty))
                else -> Municipalities(municipalities, actions)
            }
        }
    }
    state.pendingDeletion?.let { DeletionDialog(it, isTheOnlyMap = state.municipalities?.size == 1, actions) }
}

@Composable
private fun Municipalities(
    municipalities: List<InstalledMunicipality>,
    actions: MyMunicipalitiesActions,
) {
    LazyColumn {
        items(municipalities, key = { it.id.value }) { municipality ->
            MunicipalityRow(municipality, actions)
            HorizontalDivider()
        }
    }
}

@Composable
private fun MunicipalityRow(
    municipality: InstalledMunicipality,
    actions: MyMunicipalitiesActions,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
        Text(municipality.name, style = MaterialTheme.typography.titleMedium)
        Text(
            listOfNotNull(municipality.provinceName, megabytes(municipality.sizeBytes)).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RowActions(municipality, actions)
    }
}

// The municipality on the map says so; the others offer to show it. Any of them can be deleted.
@Composable
private fun RowActions(
    municipality: InstalledMunicipality,
    actions: MyMunicipalitiesActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        if (municipality.isShown) {
            Text(
                stringResource(R.string.my_municipalities_shown),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        } else {
            OutlinedButton(
                onClick = { actions.onShow(municipality) },
                modifier = Modifier.weight(1f).heightIn(min = TouchTargets.minimum),
            ) { Text(stringResource(R.string.my_municipalities_show)) }
        }
        TextButton(
            onClick = { actions.onAskToDelete(municipality) },
            modifier = Modifier.heightIn(min = TouchTargets.minimum),
        ) { Text(stringResource(R.string.my_municipalities_delete), color = MaterialTheme.colorScheme.error) }
    }
}

// Without a connection, deleting the only map leaves the person without any map during a blackout.
@Composable
private fun DeletionDialog(
    municipality: InstalledMunicipality,
    isTheOnlyMap: Boolean,
    actions: MyMunicipalitiesActions,
) {
    val warning =
        if (isTheOnlyMap) {
            R.string.my_municipalities_delete_only_map_text
        } else {
            R.string.my_municipalities_delete_text
        }
    AlertDialog(
        onDismissRequest = actions.onCancelDeletion,
        title = { Text(stringResource(R.string.my_municipalities_delete_title, municipality.name)) },
        text = { Text(stringResource(warning, megabytes(municipality.sizeBytes))) },
        confirmButton = {
            TextButton(onClick = actions.onConfirmDeletion) {
                Text(stringResource(R.string.my_municipalities_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancelDeletion) { Text(stringResource(R.string.my_municipalities_cancel)) }
        },
    )
}
