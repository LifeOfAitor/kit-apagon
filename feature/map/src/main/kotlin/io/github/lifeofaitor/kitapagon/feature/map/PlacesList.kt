package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets

/**
 * Every place of the package, grouped by category. It is the way to reach the places for people
 * who use a screen reader, because the content of the map is not accessible (decision 0011).
 */
@Composable
internal fun PlacesList(
    places: List<Place>,
    selectedCategory: PlaceCategory?,
    onCategorySelected: (PlaceCategory?) -> Unit,
    onPlaceChosen: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    val placesByCategory = groupForList(places)
    Column(modifier = modifier.fillMaxSize()) {
        CategoryFilters(placesByCategory, selectedCategory, onCategorySelected)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { ListIntroduction() }
            val shownGroups = placesByCategory.filterKeys { selectedCategory == null || it == selectedCategory }
            if (shownGroups.isEmpty()) {
                item {
                    Text(stringResource(R.string.places_none_in_package), Modifier.padding(Spacing.medium))
                }
            }
            shownGroups.forEach { (category, categoryPlaces) -> placeGroup(category, categoryPlaces, onPlaceChosen) }
        }
    }
}

/** Categories in map priority order; inside each, named places first and alphabetically. */
internal fun groupForList(places: List<Place>): Map<PlaceCategory, List<Place>> =
    places
        .sortedWith(compareBy<Place>({ it.details.name == null }, { it.details.name }, { it.sourceId }))
        .groupBy { place -> place.category }
        .toSortedMap(compareBy { category -> category.appearance().mapPriority })

@Composable
private fun CategoryFilters(
    placesByCategory: Map<PlaceCategory, List<Place>>,
    selectedCategory: PlaceCategory?,
    onCategorySelected: (PlaceCategory?) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text(stringResource(R.string.places_all)) },
            modifier = Modifier.heightIn(min = TouchTargets.minimum),
        )
        placesByCategory.forEach { (category, categoryPlaces) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(groupTitle(category, categoryPlaces.size)) },
                modifier = Modifier.heightIn(min = TouchTargets.minimum),
            )
        }
    }
}

@Composable
private fun ListIntroduction() {
    Text(
        text = stringResource(R.string.places_unavailable_without_location),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(Spacing.medium),
    )
}

private fun LazyListScope.placeGroup(
    category: PlaceCategory,
    categoryPlaces: List<Place>,
    onPlaceChosen: (Place) -> Unit,
) {
    item(key = "header-${category.wireName}") {
        Text(
            text = groupTitle(category, categoryPlaces.size),
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                    .semantics { heading() },
        )
    }
    items(categoryPlaces, key = { place -> place.sourceId }) { place -> PlaceRow(place, onPlaceChosen) }
}

@Composable
private fun PlaceRow(
    place: Place,
    onPlaceChosen: (Place) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TouchTargets.minimum)
                .clickable { onPlaceChosen(place) }
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        PlaceMarkerIcon(place.category)
        Column {
            Text(
                place.details.name ?: stringResource(place.category.appearance().singularName),
                style = MaterialTheme.typography.bodyLarge,
            )
            place.details.address?.let { address -> Text(address, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun groupTitle(
    category: PlaceCategory,
    count: Int,
): String = stringResource(R.string.places_group_title, stringResource(category.appearance().pluralName), count)
