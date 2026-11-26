package io.github.lifeofaitor.kitapagon.app.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheAppNameAsAHeading() {
        showHomeScreen(isDialerUnavailable = false)

        composeRule
            .onNodeWithText("Kit Apagón")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun alwaysShowsTheEmergencyNumberNotice() {
        showHomeScreen(isDialerUnavailable = false)

        composeRule.onNodeWithText("Llamar al 112").assertIsDisplayed()
    }

    @Test
    fun explainsWhatToDoWhenTheDeviceCannotDial() {
        showHomeScreen(isDialerUnavailable = true)

        composeRule.onNodeWithText(DIALER_UNAVAILABLE_MESSAGE, substring = true).assertIsDisplayed()
    }

    @Test
    fun hidesTheDialerMessageWhenTheDeviceCanDial() {
        showHomeScreen(isDialerUnavailable = false)

        composeRule.onNodeWithText(DIALER_UNAVAILABLE_MESSAGE, substring = true).assertDoesNotExist()
    }

    private fun showHomeScreen(isDialerUnavailable: Boolean) {
        composeRule.setContent {
            HomeScreen(isDialerUnavailable = isDialerUnavailable, onCallEmergencyNumber = {})
        }
    }

    private companion object {
        const val DIALER_UNAVAILABLE_MESSAGE = "no puede abrir el teléfono"
    }
}
