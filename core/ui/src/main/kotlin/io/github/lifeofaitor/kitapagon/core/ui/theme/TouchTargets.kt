package io.github.lifeofaitor.kitapagon.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum sizes of touchable elements, so they are easy to hit under stress. */
object TouchTargets {
    /** Accessibility guidelines ask for at least 48 dp; Android 8 does not enforce it by itself. */
    val minimum: Dp = 48.dp

    /** Emergency actions get extra room because they are pressed in a hurry. */
    val emergencyAction: Dp = 56.dp
}
