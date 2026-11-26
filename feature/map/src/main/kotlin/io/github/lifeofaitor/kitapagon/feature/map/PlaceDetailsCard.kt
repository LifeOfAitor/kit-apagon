package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing
import io.github.lifeofaitor.kitapagon.core.ui.theme.TouchTargets

/** Everything known about a place, with the origin of the data always visible (decision 0011). */
@Composable
internal fun PlaceDetailsCard(
    place: Place,
    onClose: () -> Unit,
    onCallPhone: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            PlaceTitleRow(place, onClose)
            place.details.address?.let { address -> Text(address, style = MaterialTheme.typography.bodyLarge) }
            place.details.openingHours?.let { hours ->
                Text(stringResource(R.string.place_opening_hours, hours), style = MaterialTheme.typography.bodyLarge)
            }
            place.details.phone?.let { phone -> CallPhoneButton(phone, onCallPhone) }
            Text(text = stringResource(sourceDescriptionOf(place)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaceTitleRow(
    place: Place,
    onClose: () -> Unit,
) {
    val categoryName = stringResource(place.category.appearance().singularName)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        PlaceMarkerIcon(place.category)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.details.name ?: categoryName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (place.details.name != null) Text(categoryName, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onClose, modifier = Modifier.heightIn(min = TouchTargets.minimum)) {
            Text(stringResource(R.string.place_close))
        }
    }
}

@Composable
private fun CallPhoneButton(
    phone: String,
    onCallPhone: (String) -> Boolean,
) {
    var isDialerUnavailable by remember(phone) { mutableStateOf(false) }
    Button(
        onClick = { isDialerUnavailable = !onCallPhone(phone) },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TouchTargets.minimum),
    ) {
        Text(stringResource(R.string.place_call, phone))
    }
    if (isDialerUnavailable) {
        Text(
            text = stringResource(R.string.place_dialer_unavailable),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun sourceDescriptionOf(place: Place): Int =
    when (place.source) {
        PlaceSource.OPENSTREETMAP -> R.string.place_source_openstreetmap
        PlaceSource.OFFICIAL -> R.string.place_source_official
    }
