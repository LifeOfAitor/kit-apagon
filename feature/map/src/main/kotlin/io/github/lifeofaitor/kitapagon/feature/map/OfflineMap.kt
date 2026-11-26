package io.github.lifeofaitor.kitapagon.feature.map

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Draws an installed package with MapLibre. Everything it loads comes from the device. */
@Composable
internal fun OfflineMap(
    state: MapScreenState.Ready,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val theme = MapTheme.forDarkMode(isSystemInDarkTheme())
    val styleJson =
        remember(state.mapFile, theme) {
            MapStyleTemplate.bindToMapFile(context.assets.readMapStyle(theme), state.mapFile)
        }
    val mapView = rememberMapViewFollowingLifecycle()
    ReleaseMapMemoryWhenTheSystemIsLow(mapView)
    LaunchedEffect(mapView, styleJson) {
        mapView.getMapAsync { map -> map.showPackage(styleJson, state.bounds) }
    }
    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}

@Composable
private fun rememberMapViewFollowingLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { createOfflineMapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event -> mapView.follow(event) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.releaseFrom(lifecycle.currentState)
        }
    }
    return mapView
}

@Composable
private fun ReleaseMapMemoryWhenTheSystemIsLow(mapView: MapView) {
    val context = LocalContext.current
    DisposableEffect(context, mapView) {
        val memoryCallbacks = MapMemoryReleaser(mapView)
        context.registerComponentCallbacks(memoryCallbacks)
        onDispose { context.unregisterComponentCallbacks(memoryCallbacks) }
    }
}

private class MapMemoryReleaser(
    private val mapView: MapView,
) : ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        if (shouldReleaseMapMemory(level)) mapView.onLowMemory()
    }

    // Deprecated since Android 14 in favour of onTrimMemory, but still the only signal some older
    // devices send, and the interface requires it.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onLowMemory() {
        mapView.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit
}

/** MapLibre drops its caches when memory runs low or when the map is no longer visible. */
@Suppress("DEPRECATION") // The running-low level is deprecated on Android 14+, yet still sent by Android 8 to 13.
internal fun shouldReleaseMapMemory(trimLevel: Int): Boolean = trimLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW

private fun createOfflineMapView(context: Context): MapView {
    MapLibre.getInstance(context)
    // Every resource of the map is local, so MapLibre must never try to reach the network.
    MapLibre.setConnected(false)
    return MapView(context).apply { onCreate(null) }
}

private fun MapView.follow(event: Lifecycle.Event) {
    when (event) {
        Lifecycle.Event.ON_START -> onStart()
        Lifecycle.Event.ON_RESUME -> onResume()
        Lifecycle.Event.ON_PAUSE -> onPause()
        Lifecycle.Event.ON_STOP -> onStop()
        else -> Unit
    }
}

// Leaving the screen while it is visible must still walk MapLibre through pause and stop before destroy.
private fun MapView.releaseFrom(currentState: Lifecycle.State) {
    if (currentState.isAtLeast(Lifecycle.State.RESUMED)) onPause()
    if (currentState.isAtLeast(Lifecycle.State.STARTED)) onStop()
    onDestroy()
}

private fun MapLibreMap.showPackage(
    styleJson: String,
    bounds: GeographicBounds,
) {
    uiSettings.isLogoEnabled = false
    uiSettings.isAttributionEnabled = false
    val packageArea = LatLngBounds.from(bounds.north, bounds.east, bounds.south, bounds.west)
    setLatLngBoundsForCameraTarget(packageArea)
    setMaxZoomPreference(MAXIMUM_ZOOM)
    setStyle(Style.Builder().fromJson(styleJson)) {
        moveCamera(CameraUpdateFactory.newLatLngBounds(packageArea, 0))
    }
}

// Packages contain tiles up to zoom 14 (decision 0006); MapLibre enlarges them up to this level.
private const val MAXIMUM_ZOOM = 18.0
