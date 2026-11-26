package io.github.lifeofaitor.kitapagon.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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

    private fun showNavigation() {
        composeRule.setContent {
            KitApagonNavigation(
                homeScreen = { onOpenMap ->
                    Text(HOME_TEXT)
                    Button(onClick = onOpenMap) { Text(OPEN_MAP_TEXT) }
                },
                mapScreen = { Text(MAP_TEXT) },
            )
        }
    }

    private companion object {
        const val HOME_TEXT = "fake home"
        const val OPEN_MAP_TEXT = "open map"
        const val MAP_TEXT = "fake map"
    }
}
