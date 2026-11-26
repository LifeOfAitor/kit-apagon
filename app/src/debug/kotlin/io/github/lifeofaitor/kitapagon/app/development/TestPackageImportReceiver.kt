package io.github.lifeofaitor.kitapagon.app.development

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.lifeofaitor.kitapagon.app.AppContainer
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import java.io.File

/**
 * Debug builds only: imports the test maps that scripts/install-test-package.sh pushes into the
 * app's external files directory. Asking for that directory also creates it owned by the app, so
 * the script calls this receiver once before pushing (files pushed into a directory created by the
 * adb shell cannot be read by the app) and once after.
 */
class TestPackageImportReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pushedMapsDirectory = context.getExternalFilesDir(AppContainer.MUNICIPAL_PACKAGES_DIRECTORY) ?: return
        val privatePackagesDirectory = File(context.filesDir, AppContainer.MUNICIPAL_PACKAGES_DIRECTORY)
        importPushedMaps(pushedMapsDirectory, privatePackagesDirectory)
    }
}

/** Turns every pushed `<package id>.pmtiles` file into the installed package `<package id>/map.pmtiles`. */
internal fun importPushedMaps(
    pushedMapsDirectory: File,
    packagesDirectory: File,
) {
    pushedMapsDirectory
        .listFiles()
        .orEmpty()
        .filter { candidate -> candidate.isFile && candidate.extension == PUSHED_MAP_EXTENSION }
        .forEach { pushedMap ->
            val packageDirectory = File(packagesDirectory, pushedMap.nameWithoutExtension).apply { mkdirs() }
            pushedMap.copyTo(File(packageDirectory, InstalledPackages.MAP_FILE_NAME), overwrite = true)
            pushedMap.delete()
        }
}

private const val PUSHED_MAP_EXTENSION = "pmtiles"
