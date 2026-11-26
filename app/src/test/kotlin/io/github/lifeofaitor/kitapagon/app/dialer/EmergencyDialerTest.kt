package io.github.lifeofaitor.kitapagon.app.dialer

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class EmergencyDialerTest {
    private lateinit var activity: Activity

    @Before
    fun createActivityThatFailsWhenNoAppCanHandleAnIntent() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        shadowOf(activity.application).checkActivities(true)
    }

    @Test
    fun opensThePhoneAppWithTheEmergencyNumberTyped() {
        installPhoneApp()

        val opened = EmergencyDialer(activity).openDialer()

        assertTrue(opened)
        val startedIntent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_DIAL, startedIntent.action)
        assertEquals("tel:112", startedIntent.dataString)
    }

    @Test
    fun reportsFailureInsteadOfCrashingWhenThereIsNoPhoneApp() {
        val opened = EmergencyDialer(activity).openDialer()

        assertFalse(opened)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    private fun installPhoneApp() {
        val phoneApp = ComponentName("com.example.phone", "com.example.phone.DialActivity")
        val packageManager = shadowOf(activity.packageManager)
        packageManager.addActivityIfNotPresent(phoneApp)
        packageManager.addIntentFilterForActivity(
            phoneApp,
            IntentFilter(Intent.ACTION_DIAL).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("tel")
            },
        )
    }
}
