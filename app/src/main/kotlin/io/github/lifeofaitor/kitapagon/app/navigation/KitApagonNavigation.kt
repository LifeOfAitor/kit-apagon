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
    mapScreen: @Composable () -> Unit,
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
                AppDestination.HOME -> NavEntry(destination) { homeScreen { backStack.add(AppDestination.MAP) } }
                AppDestination.MAP -> NavEntry(destination) { mapScreen() }
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

private fun SnapshotStateList<AppDestination>.removeLastUnlessOnlyHomeRemains() {
    if (size > 1) removeAt(lastIndex)
}
