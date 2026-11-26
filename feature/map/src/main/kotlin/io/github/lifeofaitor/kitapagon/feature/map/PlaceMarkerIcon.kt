package io.github.lifeofaitor.kitapagon.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory

/** The same coloured circle and white glyph as on the map, for the list and the details. */
@Composable
internal fun PlaceMarkerIcon(
    category: PlaceCategory,
    modifier: Modifier = Modifier,
) {
    val appearance = category.appearance()
    Box(
        modifier = modifier.size(MARKER_SIZE).background(appearance.markerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative: the category name is always written next to the icon.
        Icon(
            painter = painterResource(appearance.icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(GLYPH_PADDING),
        )
    }
}

private val MARKER_SIZE = 40.dp
private val GLYPH_PADDING = 8.dp
