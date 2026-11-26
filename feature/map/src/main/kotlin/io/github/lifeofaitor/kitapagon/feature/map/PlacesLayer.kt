package io.github.lifeofaitor.kitapagon.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** The layer of places drawn on top of the basemap (decision 0011). */
internal object PlacesLayer {
    private const val SOURCE_ID = "kit-apagon-places"
    private const val LAYER_ID = "kit-apagon-places"
    private const val IMAGE_PROPERTY = "image"
    private const val PRIORITY_PROPERTY = "priority"
    private const val INDEX_PROPERTY = "index"

    fun imageNameOf(category: PlaceCategory): String = "kit-apagon-place-${category.wireName}"

    /** Each feature keeps the index of its place, so a tap can be traced back to the place. */
    fun featureCollectionOf(places: List<Place>): FeatureCollection =
        FeatureCollection.fromFeatures(
            places.mapIndexed { index, place ->
                Feature
                    .fromGeometry(Point.fromLngLat(place.location.longitude, place.location.latitude))
                    .apply {
                        addStringProperty(IMAGE_PROPERTY, imageNameOf(place.category))
                        addNumberProperty(PRIORITY_PROPERTY, place.category.appearance().mapPriority)
                        addNumberProperty(INDEX_PROPERTY, index)
                    }
            },
        )

    fun addTo(
        style: Style,
        context: Context,
        places: List<Place>,
    ) {
        PlaceCategory.entries.forEach { category -> style.addImage(imageNameOf(category), markerOf(context, category)) }
        style.addSource(GeoJsonSource(SOURCE_ID, featureCollectionOf(places)))
        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.iconImage(Expression.get(IMAGE_PROPERTY)),
                // Overlapping markers hide each other; the lowest priority number stays visible.
                PropertyFactory.iconAllowOverlap(false),
                PropertyFactory.symbolSortKey(Expression.get(PRIORITY_PROPERTY)),
            ),
        )
    }

    fun placeIndexAt(
        map: MapLibreMap,
        screenPoint: PointF,
    ): Int? =
        map
            .queryRenderedFeatures(screenPoint, LAYER_ID)
            .firstOrNull()
            ?.getNumberProperty(INDEX_PROPERTY)
            ?.toInt()

    // A coloured circle with a white outline, so it stands out on both the light and the dark map,
    // and the white glyph of the category in the middle.
    private fun markerOf(
        context: Context,
        category: PlaceCategory,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (MARKER_SIZE_DP * density).toInt()
        val radius = size / 2f
        val outline = OUTLINE_WIDTH_DP * density
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(radius, radius, radius, paint)
        paint.color = category.appearance().markerColor.toArgb()
        canvas.drawCircle(radius, radius, radius - outline, paint)
        val glyphInset = (size * (1 - GLYPH_SIZE_RATIO) / 2).toInt()
        ContextCompat.getDrawable(context, category.appearance().icon)?.apply {
            setBounds(glyphInset, glyphInset, size - glyphInset, size - glyphInset)
            draw(canvas)
        }
        return bitmap
    }

    private const val MARKER_SIZE_DP = 30
    private const val OUTLINE_WIDTH_DP = 2
    private const val GLYPH_SIZE_RATIO = 0.6f
}
