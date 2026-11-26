package io.github.lifeofaitor.kitapagon.app

import android.content.Context
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import java.io.File

/** Creates the objects the screens share. Dependencies are wired by hand (decision 0008). */
class AppContainer(
    applicationContext: Context,
) {
    val installedPackages = InstalledPackages(File(applicationContext.filesDir, MUNICIPAL_PACKAGES_DIRECTORY))

    companion object {
        // Also used by scripts/install-test-package.sh.
        const val MUNICIPAL_PACKAGES_DIRECTORY = "municipal-packages"
    }
}
