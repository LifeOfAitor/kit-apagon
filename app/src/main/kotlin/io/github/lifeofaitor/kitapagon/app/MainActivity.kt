package io.github.lifeofaitor.kitapagon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.lifeofaitor.kitapagon.app.dialer.EmergencyDialer
import io.github.lifeofaitor.kitapagon.app.home.HomeScreen
import io.github.lifeofaitor.kitapagon.core.ui.theme.KitApagonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val emergencyDialer = EmergencyDialer(activityContext = this)
        setContent {
            var isDialerUnavailable by rememberSaveable { mutableStateOf(false) }
            KitApagonTheme {
                HomeScreen(
                    isDialerUnavailable = isDialerUnavailable,
                    onCallEmergencyNumber = { isDialerUnavailable = !emergencyDialer.openDialer() },
                )
            }
        }
    }
}
