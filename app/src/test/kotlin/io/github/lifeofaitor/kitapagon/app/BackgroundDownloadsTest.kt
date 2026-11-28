package io.github.lifeofaitor.kitapagon.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.startup.InitializationProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lifeofaitor.kitapagon.feature.packages.PackageDownloadWorkerFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the final, merged manifest for what the background download needs (decision 0017). */
@RunWith(AndroidJUnit4::class)
class BackgroundDownloadsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun workManagerCreatesTheDownloadJobWithTheFactoryOfTheApp() {
        val application = context as KitApagonApplication

        assertTrue(application.workManagerConfiguration.workerFactory is PackageDownloadWorkerFactory)
    }

    // The automatic start would use a factory that cannot create the download job.
    @Test
    fun workManagerDoesNotStartWithItsDefaultConfiguration() {
        val provider =
            context.packageManager.getProviderInfo(
                ComponentName(context, InitializationProvider::class.java),
                PackageManager.GET_META_DATA,
            )

        assertFalse(provider.metaData?.containsKey("androidx.work.WorkManagerInitializer") ?: false)
    }

    @Test
    fun theDownloadServiceSaysItSynchronisesData() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val service =
            context.packageManager.getServiceInfo(
                ComponentName(context, "androidx.work.impl.foreground.SystemForegroundService"),
                0,
            )

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, service.foregroundServiceType)
        assertTrue(declaredPermissions().contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
    }

    // The progress is shown in the app, so it never asks to show notifications (decision 0017).
    @Test
    fun theAppDoesNotAskToShowNotifications() {
        assertFalse(declaredPermissions().contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    private fun declaredPermissions(): Set<String> =
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()
}
