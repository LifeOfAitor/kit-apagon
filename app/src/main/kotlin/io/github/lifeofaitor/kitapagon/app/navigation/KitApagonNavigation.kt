package io.github.lifeofaitor.kitapagon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

/**
 * Screens of the app and how to move between them (Navigation 3, decision 0010). The screens are
 * parameters so the navigation can be tested without MapLibre.
 */
@Composable
fun KitApagonNavigation(
    homeScreen: @Composable (onOpenMap: () -> Unit) -> Unit,
    mapScreen: @Composable (MapNavigation) -> Unit,
    chooseMunicipalityScreen: @Composable (onInstalled: () -> Unit) -> Unit,
    myMunicipalitiesScreen: @Composable (MyMunicipalitiesNavigation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberAppBackStack()
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastUnlessOnlyHomeRemains() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = { destination ->
            when (destination) {
                AppDestination.HOME -> {
                    NavEntry(destination) { homeScreen { backStack.add(AppDestination.MAP) } }
                }

                AppDestination.MAP -> {
                    NavEntry(destination) { mapScreen(backStack.mapNavigation()) }
                }

                // Once installed, the map shows the new municipality.
                AppDestination.CHOOSE_MUNICIPALITY -> {
                    NavEntry(destination) { chooseMunicipalityScreen { backStack.returnToTheMap() } }
                }

                AppDestination.MY_MUNICIPALITIES -> {
                    NavEntry(destination) { myMunicipalitiesScreen(backStack.myMunicipalitiesNavigation()) }
                }
            }
        },
    )
}

// Saved by name, so the open screens survive a screen rotation or the system closing the app.
@Composable
private fun rememberAppBackStack(): SnapshotStateList<AppDestination> =
    rememberSaveable(
        saver =
            listSaver(
                save = { backStack -> backStack.map { destination -> destination.name } },
                restore = { names -> names.map(AppDestination::valueOf).toMutableStateList() },
            ),
    ) { mutableStateListOf(AppDestination.HOME) }

/** Where the map screen can go. */
class MapNavigation(
    val onChooseMunicipality: () -> Unit,
    val onOpenMunicipalities: () -> Unit,
)

/** Where the list of municipalities can go. */
class MyMunicipalitiesNavigation(
    val onShown: () -> Unit,
    val onAddMunicipality: () -> Unit,
)

private fun SnapshotStateList<AppDestination>.mapNavigation() =
    MapNavigation(
        onChooseMunicipality = { add(AppDestination.CHOOSE_MUNICIPALITY) },
        onOpenMunicipalities = { add(AppDestination.MY_MUNICIPALITIES) },
    )

private fun SnapshotStateList<AppDestination>.myMunicipalitiesNavigation() =
    MyMunicipalitiesNavigation(
        onShown = { returnToTheMap() },
        onAddMunicipality = { add(AppDestination.CHOOSE_MUNICIPALITY) },
    )

private fun SnapshotStateList<AppDestination>.returnToTheMap() {
    removeAll { it == AppDestination.CHOOSE_MUNICIPALITY || it == AppDestination.MY_MUNICIPALITIES }
}

private fun SnapshotStateList<AppDestination>.removeLastUnlessOnlyHomeRemains() {
    if (size > 1) removeAt(lastIndex)
}
