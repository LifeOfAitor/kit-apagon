package io.github.lifeofaitor.kitapagon.feature.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onCallPhone: (String) -> Boolean,
    onChooseMunicipality: () -> Unit,
    onOpenMunicipalities: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val interaction by viewModel.interaction.collectAsStateWithLifecycle()
    MapScreenContent(
        state = state,
        interaction = interaction,
        actions = MapScreenActions.of(viewModel, onCallPhone, onChooseMunicipality, onOpenMunicipalities),
        modifier = modifier,
    ) { readyState, selectedPlace -> OfflineMap(readyState, selectedPlace, onPlaceTapped = viewModel::selectPlace) }
}

/** Everything the person can do on the map screen. */
internal data class MapScreenActions(
    val onShowMap: () -> Unit,
    val onShowList: () -> Unit,
    val onCloseDetails: () -> Unit,
    val onPlaceChosenInList: (Place) -> Unit,
    val onListCategorySelected: (PlaceCategory?) -> Unit,
    val onCallPhone: (String) -> Boolean,
    val onChooseMunicipality: () -> Unit,
    val onOpenMunicipalities: () -> Unit,
) {
    companion object {
        fun of(
            viewModel: MapViewModel,
            onCallPhone: (String) -> Boolean,
            onChooseMunicipality: () -> Unit,
            onOpenMunicipalities: () -> Unit,
        ) = MapScreenActions(
            onShowMap = viewModel::showMap,
            onShowList = viewModel::showList,
            onCloseDetails = viewModel::clearSelectedPlace,
            onPlaceChosenInList = viewModel::showPlaceOnMap,
            onListCategorySelected = viewModel::filterListBy,
            onCallPhone = onCallPhone,
            onChooseMunicipality = onChooseMunicipality,
            onOpenMunicipalities = onOpenMunicipalities,
        )
    }
}

/** Stateless content of the map screen. The map itself is a slot so tests can run without MapLibre. */
@Composable
internal fun MapScreenContent(
    state: MapScreenState,
    interaction: PlacesInteraction,
    actions: MapScreenActions,
    modifier: Modifier = Modifier,
    readyMapContent: @Composable (MapScreenState.Ready, Place?) -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            MapScreenState.Loading -> {
                CenteredMessage(stringResource(R.string.map_loading))
            }

            MapScreenState.NoPackageInstalled -> {
                CenteredMessage(stringResource(R.string.map_no_package_installed)) {
                    Button(
                        onClick = actions.onChooseMunicipality,
                        modifier = Modifier.heightIn(min = TouchTargets.minimum),
                    ) { Text(stringResource(R.string.map_choose_municipality)) }
                }
            }

            is MapScreenState.PackageDamaged -> {
                CenteredMessage(stringResource(R.string.map_package_damaged, state.packageId.value)) {
                    Button(
                        onClick = actions.onOpenMunicipalities,
                        modifier = Modifier.heightIn(min = TouchTargets.minimum),
                    ) { Text(stringResource(R.string.map_my_municipalities)) }
                }
            }

            is MapScreenState.Ready -> {
                ReadyContent(state, interaction, actions, readyMapContent)
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: MapScreenState.Ready,
    interaction: PlacesInteraction,
    actions: MapScreenActions,
    readyMapContent: @Composable (MapScreenState.Ready, Place?) -> Unit,
) {
    StepBackWithinTheMapScreen(interaction, actions)
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        MunicipalityHeader(state.name, actions.onOpenMunicipalities)
        ViewModeSwitch(interaction.viewMode, actions)
        if (state.arePlacesDamaged) {
            Text(
                text = stringResource(R.string.map_places_damaged),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            when (interaction.viewMode) {
                MapViewMode.MAP -> {
                    MapWithDetails(state, interaction.selectedPlace, actions, readyMapContent)
                }

                MapViewMode.LIST -> {
                    PlacesList(
                        places = state.places,
                        selectedCategory = interaction.listCategory,
                        onCategorySelected = actions.onListCategorySelected,
                        onPlaceChosen = actions.onPlaceChosenInList,
                    )
                }
            }
        }
    }
}

/** The back button first closes the details of a place, then returns from the list to the map. */
@Composable
private fun StepBackWithinTheMapScreen(
    interaction: PlacesInteraction,
    actions: MapScreenActions,
) {
    BackHandler(enabled = interaction.selectedPlace != null, onBack = actions.onCloseDetails)
    BackHandler(
        enabled = interaction.selectedPlace == null && interaction.viewMode == MapViewMode.LIST,
        onBack = actions.onShowMap,
    )
}

/** Which municipality is on the screen, and the way to change it, add another or delete one. */
@Composable
private fun MunicipalityHeader(
    name: String,
    onOpenMunicipalities: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onOpenMunicipalities, modifier = Modifier.heightIn(min = TouchTargets.minimum)) {
            Text(stringResource(R.string.map_my_municipalities))
        }
    }
}

@Composable
private fun ViewModeSwitch(
    viewMode: MapViewMode,
    actions: MapScreenActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        ViewModeButton(
            stringResource(R.string.map_view_map),
            viewMode == MapViewMode.MAP,
            actions.onShowMap,
            Modifier.weight(1f),
        )
        ViewModeButton(
            stringResource(R.string.map_view_list),
            viewMode == MapViewMode.LIST,
            actions.onShowList,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun ViewModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val tabModifier =
        modifier
            .heightIn(min = TouchTargets.minimum)
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
    if (isSelected) {
        Button(onClick = onClick, modifier = tabModifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = tabModifier) { Text(label) }
    }
}

@Composable
private fun MapWithDetails(
    state: MapScreenState.Ready,
    selectedPlace: Place?,
    actions: MapScreenActions,
    readyMapContent: @Composable (MapScreenState.Ready, Place?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        readyMapContent(state, selectedPlace)
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.small)) {
            selectedPlace?.let { place ->
                PlaceDetailsCard(place, onClose = actions.onCloseDetails, onCallPhone = actions.onCallPhone)
            }
            MapAttribution()
        }
    }
}

@Composable
private fun MapAttribution() {
    Surface(
        modifier = Modifier.padding(top = Spacing.small),
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

@Composable
private fun CenteredMessage(
    message: String,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        action()
    }
}

private const val ATTRIBUTION_BACKGROUND_OPACITY = 0.8f
