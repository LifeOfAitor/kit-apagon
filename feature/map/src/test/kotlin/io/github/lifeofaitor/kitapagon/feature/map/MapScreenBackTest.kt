package io.github.lifeofaitor.kitapagon.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.PHARMACY
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.readyState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenBackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val requestedActions = mutableListOf<String>()

    @Test
    fun backFirstClosesTheDetailsOfThePlace() {
        showContent(PlacesInteraction(selectedPlace = PHARMACY))

        pressBack()

        assertEquals(listOf("close details"), requestedActions)
    }

    @Test
    fun backReturnsFromTheListToTheMap() {
        showContent(PlacesInteraction(viewMode = MapViewMode.LIST))

        pressBack()

        assertEquals(listOf("show map"), requestedActions)
    }

    @Test
    fun backLeavesTheMapScreenWhenThereIsNothingElseToClose() {
        showContent(PlacesInteraction())

        pressBack()

        assertEquals(emptyList<String>(), requestedActions)
    }

    private fun pressBack() {
        // The back handlers are registered when the screen is composed, before a person could press back.
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun showContent(interaction: PlacesInteraction) {
        composeRule.setContent {
            MapScreenContent(readyState(), interaction, recordingActions()) { _, _: Place? -> Text("fake map") }
        }
    }

    private fun recordingActions() =
        MapScreenActions(
            onShowMap = { requestedActions += "show map" },
            onShowList = { requestedActions += "show list" },
            onCloseDetails = { requestedActions += "close details" },
            onPlaceChosenInList = { requestedActions += "place chosen" },
            onListCategorySelected = { requestedActions += "category selected" },
            onCallPhone = { true },
        )
}
