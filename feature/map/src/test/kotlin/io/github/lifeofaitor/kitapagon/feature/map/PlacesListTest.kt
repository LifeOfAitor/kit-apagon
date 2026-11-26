package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.HOSPITAL
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.PHARMACY
import io.github.lifeofaitor.kitapagon.feature.map.TestPlaces.UNNAMED_FOUNTAIN
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlacesListTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val places = listOf(UNNAMED_FOUNTAIN, PHARMACY, HOSPITAL)

    @Test
    fun groupsPlacesByCategoryInMapPriorityOrder() {
        val categories = groupForList(places).keys.toList()

        assertEquals(
            listOf(PlaceCategory.HEALTH_CENTRE, PlaceCategory.PHARMACY, PlaceCategory.DRINKING_WATER),
            categories,
        )
    }

    @Test
    fun putsNamedPlacesFirstAndInAlphabeticalOrder() {
        val pharmacies =
            listOf(
                PHARMACY.copy(sourceId = "node/3", details = PHARMACY.details.copy(name = null)),
                PHARMACY.copy(sourceId = "node/4", details = PHARMACY.details.copy(name = "Zubia")),
                PHARMACY.copy(sourceId = "node/5", details = PHARMACY.details.copy(name = "Aroia")),
            )

        val sourceIds = groupForList(pharmacies).getValue(PlaceCategory.PHARMACY).map { it.sourceId }

        assertEquals(listOf("node/5", "node/4", "node/3"), sourceIds)
    }

    @Test
    fun showsEachGroupAsAHeadingWithItsCount() {
        showList()

        composeRule
            .onAllNodesWithText("Farmacias (1)")
            .filterToOne(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }

    @Test
    fun namesUnnamedPlacesByTheirCategory() {
        showList()

        composeRule.onNodeWithText("Agua potable").assertIsDisplayed()
    }

    @Test
    fun explainsThatTheListIsNotSortedByDistanceYet() {
        showList()

        composeRule.onNodeWithText("no está ordenada por cercanía", substring = true).assertIsDisplayed()
    }

    @Test
    fun choosingAPlaceReportsIt() {
        val chosenPlaces = mutableListOf<Place>()
        showList(onPlaceChosen = { chosenPlaces += it })

        composeRule.onNodeWithText("Aroia Larrea").performClick()

        assertEquals(listOf(PHARMACY), chosenPlaces)
    }

    @Test
    fun showsOnlyTheSelectedCategory() {
        showList(selectedCategory = PlaceCategory.PHARMACY)

        composeRule.onNodeWithText("Aroia Larrea").assertIsDisplayed()
        composeRule.onNodeWithText("Bidasoko Ospitalea").assertDoesNotExist()
    }

    @Test
    fun theCategoryFiltersReportTheChosenCategory() {
        val selectedCategories = mutableListOf<PlaceCategory?>()
        showList(onCategorySelected = { selectedCategories += it })

        composeRule
            .onAllNodesWithText("Centros sanitarios (1)")
            .filterToOne(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .performClick()
        composeRule.onNodeWithText("Todos").performClick()

        assertEquals(listOf(PlaceCategory.HEALTH_CENTRE, null), selectedCategories)
    }

    @Test
    fun saysSoWhenThePackageHasNoPlacesOfTheChosenCategory() {
        showList(selectedCategory = PlaceCategory.DEFIBRILLATOR)

        composeRule.onNodeWithText("Este mapa no tiene lugares de ese tipo.").assertIsDisplayed()
    }

    private fun showList(
        selectedCategory: PlaceCategory? = null,
        onCategorySelected: (PlaceCategory?) -> Unit = {},
        onPlaceChosen: (Place) -> Unit = {},
    ) {
        composeRule.setContent { PlacesList(places, selectedCategory, onCategorySelected, onPlaceChosen) }
    }
}
