package io.github.lifeofaitor.kitapagon.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KitApagonNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun startsOnTheHomeScreen() {
        showNavigation()

        composeRule.onNodeWithText(HOME_TEXT).assertIsDisplayed()
    }

    @Test
    fun opensTheMapFromTheHomeScreen() {
        showNavigation()

        composeRule.onNodeWithText(OPEN_MAP_TEXT).performClick()

        composeRule.onNodeWithText(MAP_TEXT).assertIsDisplayed()
    }

    @Test
    fun theBackButtonReturnsFromTheMapToTheHomeScreen() {
        showNavigation()
        composeRule.onNodeWithText(OPEN_MAP_TEXT).performClick()
        // The map screen registers its back handler when it is composed; a person cannot press back
        // faster than that, so the test waits for the screen to settle first.
        composeRule.waitForIdle()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText(HOME_TEXT).assertIsDisplayed()
    }

    @Test
    fun choosingAMunicipalityFromTheMapReturnsToTheMapOnceInstalled() {
        showNavigation()
        composeRule.onNodeWithText(OPEN_MAP_TEXT).performClick()
        composeRule.onNodeWithText(CHOOSE_TEXT).performClick()
        composeRule.onNodeWithText(CHOOSE_SCREEN_TEXT).assertIsDisplayed()

        composeRule.onNodeWithText(INSTALLED_TEXT).performClick()

        composeRule.onNodeWithText(MAP_TEXT).assertIsDisplayed()
    }

    @Test
    fun aMunicipalityAddedFromTheListReturnsToTheMapOnceInstalled() {
        showNavigation()
        composeRule.onNodeWithText(OPEN_MAP_TEXT).performClick()
        composeRule.onNodeWithText(MY_MUNICIPALITIES_TEXT).performClick()
        composeRule.onNodeWithText(ADD_TEXT).performClick()

        composeRule.onNodeWithText(INSTALLED_TEXT).performClick()

        composeRule.onNodeWithText(MAP_TEXT).assertIsDisplayed()
    }

    @Test
    fun choosingWhichMunicipalityToShowReturnsToTheMap() {
        showNavigation()
        composeRule.onNodeWithText(OPEN_MAP_TEXT).performClick()
        composeRule.onNodeWithText(MY_MUNICIPALITIES_TEXT).performClick()
        composeRule.onNodeWithText(MY_MUNICIPALITIES_SCREEN_TEXT).assertIsDisplayed()

        composeRule.onNodeWithText(SHOW_TEXT).performClick()

        composeRule.onNodeWithText(MAP_TEXT).assertIsDisplayed()
    }

    private fun showNavigation() {
        composeRule.setContent {
            KitApagonNavigation(
                homeScreen = { onOpenMap -> FakeScreen(HOME_TEXT, OPEN_MAP_TEXT to onOpenMap) },
                mapScreen = { navigation ->
                    FakeScreen(
                        MAP_TEXT,
                        CHOOSE_TEXT to navigation.onChooseMunicipality,
                        MY_MUNICIPALITIES_TEXT to navigation.onOpenMunicipalities,
                    )
                },
                chooseMunicipalityScreen = { onInstalled ->
                    FakeScreen(CHOOSE_SCREEN_TEXT, INSTALLED_TEXT to onInstalled)
                },
                myMunicipalitiesScreen = { navigation ->
                    FakeScreen(
                        MY_MUNICIPALITIES_SCREEN_TEXT,
                        ADD_TEXT to navigation.onAddMunicipality,
                        SHOW_TEXT to navigation.onShown,
                    )
                },
            )
        }
    }

    // A screen with its name and one button for each place it can go.
    @Composable
    private fun FakeScreen(
        name: String,
        vararg buttons: Pair<String, () -> Unit>,
    ) {
        Column {
            Text(name)
            buttons.forEach { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
        }
    }

    private companion object {
        const val HOME_TEXT = "fake home"
        const val OPEN_MAP_TEXT = "open map"
        const val MAP_TEXT = "fake map"
        const val CHOOSE_TEXT = "choose municipality"
        const val CHOOSE_SCREEN_TEXT = "fake choose municipality"
        const val INSTALLED_TEXT = "installed"
        const val MY_MUNICIPALITIES_TEXT = "my municipalities"
        const val MY_MUNICIPALITIES_SCREEN_TEXT = "fake my municipalities"
        const val ADD_TEXT = "add municipality"
        const val SHOW_TEXT = "show municipality"
    }
}
