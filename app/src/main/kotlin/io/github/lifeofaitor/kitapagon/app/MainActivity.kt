package io.github.lifeofaitor.kitapagon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lifeofaitor.kitapagon.app.dialer.EmergencyDialer
import io.github.lifeofaitor.kitapagon.app.home.HomeScreen
import io.github.lifeofaitor.kitapagon.app.navigation.KitApagonNavigation
import io.github.lifeofaitor.kitapagon.core.ui.theme.KitApagonTheme
import io.github.lifeofaitor.kitapagon.feature.map.MapScreen
import io.github.lifeofaitor.kitapagon.feature.map.MapViewModel
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = AppContainer(applicationContext)
        val emergencyDialer = EmergencyDialer(activityContext = this)
        setContent {
            KitApagonTheme {
                KitApagonNavigation(
                    homeScreen = { onOpenMap -> HomeRoute(emergencyDialer, onOpenMap) },
                    mapScreen = {
                        MapScreen(viewModel { MapViewModel(appContainer.installedPackages, Dispatchers.IO) })
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeRoute(
    emergencyDialer: EmergencyDialer,
    onOpenMap: () -> Unit,
) {
    var isDialerUnavailable by rememberSaveable { mutableStateOf(false) }
    HomeScreen(
        isDialerUnavailable = isDialerUnavailable,
        onCallEmergencyNumber = { isDialerUnavailable = !emergencyDialer.openDialer() },
        onOpenMap = onOpenMap,
    )
}
