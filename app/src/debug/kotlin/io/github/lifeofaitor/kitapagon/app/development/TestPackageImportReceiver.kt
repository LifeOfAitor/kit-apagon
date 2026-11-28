package io.github.lifeofaitor.kitapagon.app.development

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.lifeofaitor.kitapagon.app.AppContainer
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import java.io.File

/**
 * Debug builds only: imports the test package files that scripts/install-test-package.sh pushes into
 * the app's external files directory. Asking for that directory also creates it owned by the app, so
 * the script calls this receiver once before pushing (files pushed into a directory created by the
 * adb shell cannot be read by the app) and once after.
 */
class TestPackageImportReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pushedFilesDirectory = context.getExternalFilesDir(AppContainer.MUNICIPAL_PACKAGES_DIRECTORY) ?: return
        val privatePackagesDirectory = File(context.filesDir, AppContainer.MUNICIPAL_PACKAGES_DIRECTORY)
        importPushedPackageFiles(pushedFilesDirectory, privatePackagesDirectory)
    }
}

// The script pushes flat files named after the package; each one goes to its place inside the package.
private val PUSHED_FILE_SUFFIXES =
    mapOf(
        ".pmtiles" to InstalledPackages.MAP_FILE_NAME,
        ".places.json" to InstalledPackages.PLACES_FILE_NAME,
        ".package.json" to InstalledPackages.INFO_FILE_NAME,
    )

/**
 * Turns `<package id>.pmtiles`, `<package id>.places.json` and, to test the data of a downloaded
 * package, `<package id>.package.json` into the files of that installed package.
 */
internal fun importPushedPackageFiles(
    pushedFilesDirectory: File,
    packagesDirectory: File,
) {
    pushedFilesDirectory.listFiles().orEmpty().filter { candidate -> candidate.isFile }.forEach { pushedFile ->
        val (suffix, installedFileName) =
            PUSHED_FILE_SUFFIXES.entries.firstOrNull { (suffix, _) -> pushedFile.name.endsWith(suffix) }
                ?: return@forEach
        val packageDirectory = File(packagesDirectory, pushedFile.name.removeSuffix(suffix)).apply { mkdirs() }
        pushedFile.copyTo(File(packageDirectory, installedFileName), overwrite = true)
        pushedFile.delete()
    }
}
