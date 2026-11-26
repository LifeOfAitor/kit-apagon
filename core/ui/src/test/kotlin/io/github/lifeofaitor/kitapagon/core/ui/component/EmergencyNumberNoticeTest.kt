package io.github.lifeofaitor.kitapagon.core.ui.component

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmergencyNumberNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsThatTheAppDoesNotReplaceTheEmergencyNumber() {
        composeRule.setContent { EmergencyNumberNotice(onCallEmergencyNumber = {}) }

        composeRule
            .onNodeWithText("Esta app no sustituye al 112", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun callButtonInvokesTheCallbackOncePerClick() {
        var callRequests = 0
        composeRule.setContent { EmergencyNumberNotice(onCallEmergencyNumber = { callRequests++ }) }

        composeRule.onNodeWithText("Llamar al 112").assertHasClickAction().performClick()

        assertEquals(1, callRequests)
    }

    @Test
    fun callButtonIsTallerThanTheAccessibilityMinimumOnEveryAndroidVersion() {
        composeRule.setContent { EmergencyNumberNotice(onCallEmergencyNumber = {}) }

        composeRule
            .onNodeWithText("Llamar al 112")
            .assertHasClickAction()
            .assertHeightIsAtLeast(TouchTargets.minimum)
    }
}
