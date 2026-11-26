package io.github.lifeofaitor.kitapagon.feature.map

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory

/**
 * How each place category looks. The colour is never the only cue: every category also has its own
 * icon and name, for people who do not distinguish colours.
 */
data class PlaceCategoryAppearance(
    @param:StringRes val singularName: Int,
    @param:StringRes val pluralName: Int,
    @param:DrawableRes val icon: Int,
    val markerColor: Color,
    /** Lower numbers win when markers overlap on the map. */
    val mapPriority: Int,
)

fun PlaceCategory.appearance(): PlaceCategoryAppearance =
    when (this) {
        PlaceCategory.HEALTH_CENTRE -> HealthCentreAppearance
        PlaceCategory.ASSEMBLY_POINT -> AssemblyPointAppearance
        PlaceCategory.DEFIBRILLATOR -> DefibrillatorAppearance
        PlaceCategory.PHARMACY -> PharmacyAppearance
        PlaceCategory.FIRE_STATION -> FireStationAppearance
        PlaceCategory.POLICE -> PoliceAppearance
        PlaceCategory.DRINKING_WATER -> DrinkingWaterAppearance
    }

// Every marker colour keeps a contrast of at least 4.5:1 with the white glyph drawn on it
// (checked when they were chosen: the lowest is the pharmacy green, 5.1:1).
private val HealthCentreAppearance =
    PlaceCategoryAppearance(
        R.string.place_health_centre,
        R.string.places_health_centres,
        R.drawable.place_icon_health_centre,
        markerColor = Color(0xFFC62828),
        mapPriority = 0,
    )

private val AssemblyPointAppearance =
    PlaceCategoryAppearance(
        R.string.place_assembly_point,
        R.string.places_assembly_points,
        R.drawable.place_icon_assembly_point,
        markerColor = Color(0xFF00695C),
        mapPriority = 1,
    )

private val DefibrillatorAppearance =
    PlaceCategoryAppearance(
        R.string.place_defibrillator,
        R.string.places_defibrillators,
        R.drawable.place_icon_defibrillator,
        markerColor = Color(0xFFAD1457),
        mapPriority = 2,
    )

private val PharmacyAppearance =
    PlaceCategoryAppearance(
        R.string.place_pharmacy,
        R.string.places_pharmacies,
        R.drawable.place_icon_pharmacy,
        markerColor = Color(0xFF2E7D32),
        mapPriority = 3,
    )

private val FireStationAppearance =
    PlaceCategoryAppearance(
        R.string.place_fire_station,
        R.string.places_fire_stations,
        R.drawable.place_icon_fire_station,
        markerColor = Color(0xFFBF360C),
        mapPriority = 4,
    )

private val PoliceAppearance =
    PlaceCategoryAppearance(
        R.string.place_police,
        R.string.places_police,
        R.drawable.place_icon_police,
        markerColor = Color(0xFF283593),
        mapPriority = 5,
    )

private val DrinkingWaterAppearance =
    PlaceCategoryAppearance(
        R.string.place_drinking_water,
        R.string.places_drinking_water,
        R.drawable.place_icon_drinking_water,
        markerColor = Color(0xFF1565C0),
        mapPriority = 6,
    )
