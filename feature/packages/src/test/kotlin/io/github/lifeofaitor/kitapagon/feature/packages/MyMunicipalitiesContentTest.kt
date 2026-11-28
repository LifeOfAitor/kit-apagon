package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyMunicipalitiesContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val requested = mutableListOf<String>()
    private val irun =
        InstalledMunicipality(PackageId.parseOrNull("ES-20045")!!, "Irun", "Gipuzkoa", 3_555_811, isShown = true)
    private val bilbao =
        InstalledMunicipality(PackageId.parseOrNull("ES-48020")!!, "Bilbao", "Bizkaia", 7_000_000, isShown = false)

    @Test
    fun withoutMunicipalitiesOffersToAddOne() {
        show(MyMunicipalitiesState(municipalities = emptyList()))

        composeRule.onNodeWithText("Todavía no hay ningún municipio descargado.").assertIsDisplayed()
        composeRule.onNodeWithText("Añadir municipio").performClick()
        assertEquals(listOf("add"), requested)
    }

    @Test
    fun showsEachMunicipalityWithItsProvinceAndSizeAndWhichOneIsOnTheMap() {
        show(MyMunicipalitiesState(municipalities = listOf(irun, bilbao)))

        composeRule.onNodeWithText("Gipuzkoa · 3,6 MB").assertIsDisplayed()
        composeRule.onNodeWithText("Se muestra en el mapa").assertIsDisplayed()
        composeRule.onNodeWithText("Ver en el mapa").performClick()
        assertEquals(listOf("show ES-48020"), requested)
    }

    @Test
    fun warnsMoreClearlyWhenTheMunicipalityIsTheOnlyMap() {
        show(MyMunicipalitiesState(municipalities = listOf(irun), pendingDeletion = irun))

        composeRule.onNodeWithText("Es tu único mapa", substring = true).assertIsDisplayed()
    }

    @Test
    fun asksBeforeDeletingAndSaysHowMuchSpaceItFrees() {
        show(MyMunicipalitiesState(municipalities = listOf(irun, bilbao), pendingDeletion = irun))

        composeRule.onNodeWithText("¿Borrar Irun?").assertIsDisplayed()
        composeRule.onNodeWithText("Ocupa 3,6 MB", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onAllNodesWithText("Borrar").onFirst().performClick()
        assertEquals(listOf("cancel deletion", "ask to delete ES-20045"), requested)
    }

    @Test
    fun confirmsTheDeletionFromTheDialog() {
        show(MyMunicipalitiesState(municipalities = listOf(irun), pendingDeletion = irun))

        composeRule.onAllNodesWithText("Borrar")[1].performClick()

        assertEquals(listOf("confirm deletion"), requested)
    }

    @Test
    fun saysWhenADeletionFailed() {
        show(MyMunicipalitiesState(municipalities = listOf(irun), deletionFailed = true))

        composeRule.onNodeWithText("No se ha podido borrar el municipio", substring = true).assertIsDisplayed()
    }

    private fun show(state: MyMunicipalitiesState) {
        composeRule.setContent {
            MyMunicipalitiesContent(
                state,
                MyMunicipalitiesActions(
                    onShow = { requested += "show ${it.id}" },
                    onAskToDelete = { requested += "ask to delete ${it.id}" },
                    onConfirmDeletion = { requested += "confirm deletion" },
                    onCancelDeletion = { requested += "cancel deletion" },
                    onAddMunicipality = { requested += "add" },
                ),
            )
        }
    }
}
