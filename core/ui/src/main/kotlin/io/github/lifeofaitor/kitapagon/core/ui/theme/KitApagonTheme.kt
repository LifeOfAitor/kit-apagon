package io.github.lifeofaitor.kitapagon.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmergencyRed = Color(0xFFB3261E)
private val EmergencyRedLight = Color(0xFFFFB4AB)
private val DeepBlue = Color(0xFF1F4E79)
private val DeepBlueLight = Color(0xFFA8C8FF)

// Pure black backgrounds turn pixels off on OLED screens, which saves battery in dark mode.
private val DarkColors: ColorScheme =
    darkColorScheme(
        primary = DeepBlueLight,
        error = EmergencyRedLight,
        background = Color.Black,
        surface = Color.Black,
    )

private val LightColors: ColorScheme =
    lightColorScheme(
        primary = DeepBlue,
        error = EmergencyRed,
    )

/**
 * Theme of the whole app. Dynamic colours are deliberately not used, so every phone shows the
 * same tested, high-contrast palette.
 */
@Composable
fun KitApagonTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content,
    )
}
