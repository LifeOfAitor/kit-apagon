package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.HOSPITAL
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.PHARMACY
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.UNNAMED_FOUNTAIN
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceDetailsCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEverythingKnownAboutThePlace() {
        showCard(PHARMACY)

        composeRule.onNodeWithText("Aroia Larrea").assertIsDisplayed()
        composeRule.onNodeWithText("Farmacia").assertIsDisplayed()
        composeRule.onNodeWithText("Donostia kalea 25").assertIsDisplayed()
        composeRule.onNodeWithText("Horario: Mo-Fr 09:00-20:00").assertIsDisplayed()
    }

    @Test
    fun alwaysSaysWhereTheDataComesFrom() {
        showCard(PHARMACY)

        composeRule.onNodeWithText("Fuente: OpenStreetMap", substring = true).assertIsDisplayed()
    }

    @Test
    fun saysWhenTheDataIsOfficial() {
        showCard(HOSPITAL)

        composeRule.onNodeWithText("Fuente: datos oficiales del ayuntamiento.").assertIsDisplayed()
    }

    @Test
    fun usesTheCategoryAsTitleWhenThePlaceHasNoName() {
        showCard(UNNAMED_FOUNTAIN)

        composeRule.onNodeWithText("Agua potable").assertIsDisplayed()
    }

    @Test
    fun theCallButtonDialsThePhoneOfThePlace() {
        val dialledNumbers = mutableListOf<String>()
        showCard(PHARMACY, onCallPhone = {
            dialledNumbers += it
            true
        })

        composeRule.onNodeWithText("Llamar al +34 943 000 000").performClick()

        assertEquals(listOf("+34 943 000 000"), dialledNumbers)
    }

    @Test
    fun explainsWhenTheDeviceCannotDial() {
        showCard(PHARMACY, onCallPhone = { false })

        composeRule.onNodeWithText("Llamar al +34 943 000 000").performClick()

        composeRule.onNodeWithText("Este dispositivo no puede abrir el teléfono.").assertIsDisplayed()
    }

    @Test
    fun offersNoCallButtonWithoutAPhone() {
        showCard(HOSPITAL)

        composeRule.onNodeWithText("Llamar", substring = true).assertDoesNotExist()
    }

    @Test
    fun theCloseButtonClosesTheDetails() {
        var closeRequests = 0
        showCard(PHARMACY, onClose = { closeRequests++ })

        composeRule.onNodeWithText("Cerrar").performClick()

        assertEquals(1, closeRequests)
    }

    private fun showCard(
        place: Place,
        onClose: () -> Unit = {},
        onCallPhone: (String) -> Boolean = { true },
    ) {
        composeRule.setContent { PlaceDetailsCard(place, onClose, onCallPhone) }
    }
}
