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
import io.github.lifeofaitor.kitapagon.app.dialer.EMERGENCY_NUMBER
import io.github.lifeofaitor.kitapagon.app.dialer.PhoneDialer
import io.github.lifeofaitor.kitapagon.app.home.HomeScreen
import io.github.lifeofaitor.kitapagon.app.navigation.KitApagonNavigation
import io.github.lifeofaitor.kitapagon.app.navigation.MapNavigation
import io.github.lifeofaitor.kitapagon.app.navigation.MyMunicipalitiesNavigation
import io.github.lifeofaitor.kitapagon.core.ui.theme.KitApagonTheme
import io.github.lifeofaitor.kitapagon.feature.map.MapScreen
import io.github.lifeofaitor.kitapagon.feature.map.MapViewModel
import io.github.lifeofaitor.kitapagon.feature.packages.ChooseMunicipalityScreen
import io.github.lifeofaitor.kitapagon.feature.packages.ChooseMunicipalityViewModel
import io.github.lifeofaitor.kitapagon.feature.packages.MyMunicipalitiesScreen
import io.github.lifeofaitor.kitapagon.feature.packages.MyMunicipalitiesViewModel
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as KitApagonApplication).container
        val phoneDialer = PhoneDialer(activityContext = this)
        setContent {
            KitApagonTheme {
                KitApagonNavigation(
                    homeScreen = { onOpenMap -> HomeRoute(phoneDialer, onOpenMap) },
                    mapScreen = { navigation -> MapRoute(appContainer, phoneDialer, navigation) },
                    chooseMunicipalityScreen = { onInstalled -> ChooseMunicipalityRoute(appContainer, onInstalled) },
                    myMunicipalitiesScreen = { navigation -> MyMunicipalitiesRoute(appContainer, navigation) },
                )
            }
        }
    }
}

@Composable
private fun MapRoute(
    appContainer: AppContainer,
    phoneDialer: PhoneDialer,
    navigation: MapNavigation,
) {
    MapScreen(
        viewModel =
            viewModel {
                MapViewModel(
                    appContainer.installedPackages,
                    Dispatchers.IO,
                    appContainer.installedPackagesChanges,
                    appContainer.shownMunicipality.id,
                )
            },
        onCallPhone = phoneDialer::openDialer,
        onChooseMunicipality = navigation.onChooseMunicipality,
        onOpenMunicipalities = navigation.onOpenMunicipalities,
    )
}

// The municipality just installed is the one the person wants to see.
@Composable
private fun ChooseMunicipalityRoute(
    appContainer: AppContainer,
    onInstalled: () -> Unit,
) {
    ChooseMunicipalityScreen(
        viewModel =
            viewModel {
                ChooseMunicipalityViewModel(
                    appContainer.municipalityListLoader,
                    appContainer.provinceDetailsLoader,
                    appContainer.packageDownloads,
                    Dispatchers.IO,
                )
            },
        onInstalled = { id ->
            appContainer.shownMunicipality.show(id)
            onInstalled()
        },
    )
}

@Composable
private fun MyMunicipalitiesRoute(
    appContainer: AppContainer,
    navigation: MyMunicipalitiesNavigation,
) {
    MyMunicipalitiesScreen(
        viewModel = viewModel { MyMunicipalitiesViewModel(appContainer.myMunicipalitiesSources, Dispatchers.IO) },
        onShown = navigation.onShown,
        onAddMunicipality = navigation.onAddMunicipality,
    )
}

@Composable
private fun HomeRoute(
    phoneDialer: PhoneDialer,
    onOpenMap: () -> Unit,
) {
    var isDialerUnavailable by rememberSaveable { mutableStateOf(false) }
    HomeScreen(
        isDialerUnavailable = isDialerUnavailable,
        onCallEmergencyNumber = { isDialerUnavailable = !phoneDialer.openDialer(EMERGENCY_NUMBER) },
        onOpenMap = onOpenMap,
    )
}
