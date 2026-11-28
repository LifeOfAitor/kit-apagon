package io.github.lifeofaitor.kitapagon.feature.packages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packagedownload.DownloadProgress
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChooseMunicipalityContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val requested = mutableListOf<String>()
    private val ready = ListState.Ready(municipalityList())

    @Test
    fun explainsThatTheListNeedsAConnectionAndOffersToTryAgain() {
        show(ChooseMunicipalityState(list = ListState.Unavailable(ListProblem.NO_CONNECTION)))

        composeRule.onNodeWithText("Comprueba que tienes conexión", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(listOf("retry list"), requested)
    }

    @Test
    fun asksForTheNameOfTheMunicipality() {
        show(ChooseMunicipalityState(list = ready))

        composeRule.onNodeWithText("Escribe el nombre de tu municipio", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Nombre del municipio").performTextInput("iru")
        assertEquals(listOf("search iru"), requested)
    }

    @Test
    fun showsEachMatchWithItsProvince() {
        show(ChooseMunicipalityState(list = ready, query = "iru", matches = listOf(IRUN_MATCH)))

        composeRule.onNodeWithText("Gipuzkoa").assertIsDisplayed()
        composeRule.onNodeWithText("Irun").performClick()
        assertEquals(listOf("choose ES-20045"), requested)
    }

    @Test
    fun saysWhenNoMunicipalityHasThatName() {
        show(ChooseMunicipalityState(list = ready, query = "xyz"))

        composeRule.onNodeWithText("No hay ningún municipio con ese nombre.").assertIsDisplayed()
    }

    @Test
    fun beforeDownloadingShowsTheSizeAndWhoWillSeeTheAddressOfThePhone() {
        show(
            ChooseMunicipalityState(
                list = ready,
                selection = Selection.ReadyToDownload(IRUN_MATCH, downloadableIrun()),
            ),
        )

        composeRule.onNodeWithText("Irun (Gipuzkoa)").assertIsDisplayed()
        composeRule.onNodeWithText("Tamaño de la descarga: 3,6 MB").assertIsDisplayed()
        composeRule.onNodeWithText("Al descargar, GitHub verá la dirección IP de tu móvil.").assertIsDisplayed()
        composeRule.onNodeWithText("Descargar").performClick()
        composeRule.onNodeWithText("Elegir otro").performClick()
        assertEquals(listOf("download", "choose another"), requested)
    }

    @Test
    fun showsTheProgressInMegabytesAndLetsTheDownloadBeCancelled() {
        val running = DownloadState.Running(DownloadProgress(1_200_000, 3_555_811))
        show(
            ChooseMunicipalityState(
                list = ready,
                selection = Selection.Downloading(IRUN_MATCH, downloadableIrun(), running),
            ),
        )

        composeRule.onNodeWithText("Descargando: 1,2 MB de 3,6 MB").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar la descarga").performClick()
        assertEquals(listOf("cancel"), requested)
    }

    @Test
    fun saysHowMuchSpaceToFreeAndOffersToTryAgain() {
        val failed = DownloadState.Failed(DownloadProblem.NOT_ENOUGH_SPACE, neededBytes = 23_555_811)
        show(
            ChooseMunicipalityState(
                list = ready,
                selection = Selection.Downloading(IRUN_MATCH, downloadableIrun(), failed),
            ),
        )

        composeRule.onNodeWithText("Libera 23,6 MB en el móvil", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Intentarlo de nuevo").performClick()
        assertEquals(listOf("download"), requested)
    }

    @Test
    fun detailsThatFailedCanBeAskedForAgain() {
        show(ChooseMunicipalityState(list = ready, selection = Selection.DetailsUnavailable(IRUN_MATCH)))

        composeRule.onNodeWithText("No se ha podido preparar la descarga de Irun", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(listOf("choose ES-20045"), requested)
    }

    @Test
    fun writesSizesAsTheyAreReadInSpain() {
        assertEquals("3,6 MB", megabytes(3_555_811))
        assertEquals("0,1 MB", megabytes(54_792))
    }

    private fun show(state: ChooseMunicipalityState) {
        composeRule.setContent { ChooseMunicipalityContent(state, actions) }
    }

    private val actions =
        ChooseMunicipalityActions(
            onRetryList = { requested += "retry list" },
            onQueryChange = { requested += "search $it" },
            onChoose = { requested += "choose ${it.municipality.id}" },
            onChooseAnother = { requested += "choose another" },
            onDownload = { requested += "download" },
            onCancelDownload = { requested += "cancel" },
        )
}
