package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MapScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val irun = PackageId.parseOrNull("ES-20045")!!

    @Test
    fun explainsWhenNoMapIsInstalled() {
        showContent(MapScreenState.NoPackageInstalled)

        composeRule.onNodeWithText("Todavía no hay ningún mapa instalado", substring = true).assertIsDisplayed()
    }

    @Test
    fun namesTheDamagedPackage() {
        showContent(MapScreenState.PackageDamaged(irun))

        composeRule.onNodeWithText("El mapa ES-20045 está dañado", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsALoadingMessageWhileReadingThePackage() {
        showContent(MapScreenState.Loading)

        composeRule.onNodeWithText("Cargando el mapa", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsTheMapWithTheOpenStreetMapAttributionAlwaysVisible() {
        val ready = MapScreenState.Ready(irun, File("map.pmtiles"), GeographicBounds(-1.9, 43.2, -1.6, 43.4))

        showContent(ready)

        composeRule.onNodeWithText(FAKE_MAP_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap (ODbL)", substring = true).assertIsDisplayed()
    }

    private fun showContent(state: MapScreenState) {
        composeRule.setContent {
            MapScreenContent(state = state) { Text(FAKE_MAP_TEXT) }
        }
    }

    private companion object {
        const val FAKE_MAP_TEXT = "fake map"
    }
}
