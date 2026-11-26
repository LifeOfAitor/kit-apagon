package io.github.lifeofaitor.kitapagon.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the final, merged manifest: libraries must not add permissions the app does not use. */
@RunWith(AndroidJUnit4::class)
class DeclaredPermissionsTest {
    @Test
    fun theAppDoesNotDeclareLocationPermissions() {
        val declaredPermissions = declaredPermissions()

        assertFalse(declaredPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertFalse(declaredPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun declaredPermissions(): Set<String> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        return packageInfo.requestedPermissions.orEmpty().toSet()
    }
}
