package io.github.lifeofaitor.kitapagon.app.dialer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

const val EMERGENCY_NUMBER = "112"

/**
 * Opens the phone app with a number already typed. It never places the call by itself, so no call
 * permission is needed and the person always confirms.
 */
class PhoneDialer(
    private val activityContext: Context,
) {
    /** Returns false when the device has no app able to dial, such as some tablets. */
    fun openDialer(phoneNumber: String): Boolean =
        try {
            activityContext.startActivity(createDialIntent(phoneNumber))
            true
        } catch (expectedOnDevicesWithoutPhone: ActivityNotFoundException) {
            false
        }

    companion object {
        // Uri.fromParts encodes spaces and the plus sign that phone numbers in OpenStreetMap contain.
        fun createDialIntent(phoneNumber: String): Intent =
            Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null))
    }
}
