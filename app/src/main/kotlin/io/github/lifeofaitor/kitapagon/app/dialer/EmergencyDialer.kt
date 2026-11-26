package io.github.lifeofaitor.kitapagon.app.dialer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens the phone app with the emergency number already typed. It never places the call by
 * itself, so no call permission is needed and the person always confirms.
 */
class EmergencyDialer(
    private val activityContext: Context,
) {
    /** Returns false when the device has no app able to dial, such as some tablets. */
    fun openDialer(): Boolean =
        try {
            activityContext.startActivity(createDialIntent())
            true
        } catch (expectedOnDevicesWithoutPhone: ActivityNotFoundException) {
            false
        }

    companion object {
        const val EMERGENCY_NUMBER = "112"

        fun createDialIntent(): Intent = Intent(Intent.ACTION_DIAL, "tel:$EMERGENCY_NUMBER".toUri())
    }
}
