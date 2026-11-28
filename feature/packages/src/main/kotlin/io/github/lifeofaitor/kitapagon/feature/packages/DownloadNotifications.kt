package io.github.lifeofaitor.kitapagon.feature.packages

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import java.util.UUID

/**
 * The notification of a download while it runs as a foreground service, up to Android 11
 * (decision 0017). It shows the progress and lets the person cancel it.
 */
internal class DownloadNotifications(
    private val context: Context,
) {
    fun foregroundInfo(
        municipalityName: String,
        workId: UUID,
        percent: Int?,
    ): ForegroundInfo {
        createChannel()
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_package_download)
                .setContentTitle(context.getString(R.string.download_notification_title, municipalityName))
                .setProgress(FULL_PERCENT, percent ?: 0, percent == null)
                .setOngoing(true)
                .setSilent(true)
                .addAction(
                    0,
                    context.getString(R.string.download_notification_cancel),
                    WorkManager.getInstance(context).createCancelPendingIntent(workId),
                ).build()
        // The type of the service exists from Android 10; before it, the service has no type.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.download_notification_channel))
                .build(),
        )
    }

    private companion object {
        const val CHANNEL_ID = "package-downloads"
        const val NOTIFICATION_ID = 17
        const val FULL_PERCENT = 100
    }
}
