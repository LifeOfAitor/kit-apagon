package io.github.lifeofaitor.kitapagon.app

import android.app.Application
import androidx.work.Configuration

/**
 * Holds the objects of the app for the whole process, and gives WorkManager the factory of the
 * download job, which needs them (decision 0008). WorkManager starts the first time it is used.
 */
class KitApagonApplication :
    Application(),
    Configuration.Provider {
    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(container.packageDownloadWorkerFactory).build()
}
