package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.IRUN
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.PHARMACY
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.readyState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val requestedActions = mutableListOf<String>()

    @Test
    fun explainsWhenNoMapIsInstalled() {
        showContent(MapScreenState.NoPackageInstalled)

        composeRule.onNodeWithText("Todavía no hay ningún mapa instalado", substring = true).assertIsDisplayed()
    }

    @Test
    fun namesTheDamagedPackage() {
        showContent(MapScreenState.PackageDamaged(IRUN))

        composeRule.onNodeWithText("El mapa ES-20045 está dañado", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsALoadingMessageWhileReadingThePackage() {
        showContent(MapScreenState.Loading)

        composeRule.onNodeWithText("Cargando el mapa", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsTheMapWithTheOpenStreetMapAttributionAlwaysVisible() {
        showContent(readyState())

        composeRule.onNodeWithText(FAKE_MAP_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap (ODbL)", substring = true).assertIsDisplayed()
    }

    @Test
    fun marksTheMapAsTheSelectedViewAndOffersTheList() {
        showContent(readyState())

        composeRule.onNodeWithText("Mapa").assertIsSelected()
        composeRule.onNodeWithText("Lista").performClick()

        assertEquals(listOf("show list"), requestedActions)
    }

    @Test
    fun showsTheListInsteadOfTheMapWhenTheListIsSelected() {
        showContent(readyState(), PlacesInteraction(viewMode = MapViewMode.LIST))

        composeRule.onNodeWithText("Aroia Larrea").assertIsDisplayed()
        composeRule.onNodeWithText(FAKE_MAP_TEXT).assertDoesNotExist()
    }

    @Test
    fun showsTheDetailsOfTheSelectedPlaceOnTheMap() {
        showContent(readyState(), PlacesInteraction(selectedPlace = PHARMACY))

        composeRule.onNodeWithText("Aroia Larrea").assertIsDisplayed()
    }

    @Test
    fun warnsWhenThePlacesAreDamagedButKeepsTheMap() {
        showContent(readyState(places = emptyList()).copy(arePlacesDamaged = true))

        composeRule.onNodeWithText("Los lugares de este mapa están dañados", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(FAKE_MAP_TEXT).assertIsDisplayed()
    }

    private fun showContent(
        state: MapScreenState,
        interaction: PlacesInteraction = PlacesInteraction(),
    ) {
        composeRule.setContent {
            MapScreenContent(state = state, interaction = interaction, actions = recordingActions()) { _, _: Place? ->
                Text(FAKE_MAP_TEXT)
            }
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

    private companion object {
        const val FAKE_MAP_TEXT = "fake map"
    }
}
