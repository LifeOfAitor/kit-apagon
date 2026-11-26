package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MapScreenContent(state = state, modifier = modifier) { readyState -> OfflineMap(readyState) }
}

/** Stateless content of the map screen. The map itself is a slot so tests can run without MapLibre. */
@Composable
internal fun MapScreenContent(
    state: MapScreenState,
    modifier: Modifier = Modifier,
    readyMapContent: @Composable (MapScreenState.Ready) -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            MapScreenState.Loading -> {
                CenteredMessage(stringResource(R.string.map_loading))
            }

            MapScreenState.NoPackageInstalled -> {
                CenteredMessage(stringResource(R.string.map_no_package_installed))
            }

            is MapScreenState.PackageDamaged -> {
                CenteredMessage(stringResource(R.string.map_package_damaged, state.packageId.value))
            }

            is MapScreenState.Ready -> {
                MapWithAttribution(state, readyMapContent)
            }
        }
    }
}

@Composable
private fun MapWithAttribution(
    state: MapScreenState.Ready,
    readyMapContent: @Composable (MapScreenState.Ready) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        readyMapContent(state)
        Surface(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .safeDrawingPadding()
                    .padding(Spacing.small),
            color = MaterialTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_OPACITY),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = stringResource(R.string.map_attribution),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = Spacing.small),
            )
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(Spacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

private const val ATTRIBUTION_BACKGROUND_OPACITY = 0.8f
