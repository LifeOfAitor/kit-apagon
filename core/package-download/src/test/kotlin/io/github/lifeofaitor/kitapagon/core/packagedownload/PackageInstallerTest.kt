package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfoParser
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfoWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.installedPackageInfo
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageInstallerTest {
    @TempDir
    lateinit var rootDirectory: File

    private val server = MockWebServer().apply { start() }
    private val bundle = TestBundle()
    private val target = DownloadablePackage(installedPackageInfo(), bundle.files(server.url("/ES-20.tar").toString()))
    private val installedDirectory by lazy { File(rootDirectory, "ES-20045") }

    @AfterEach
    fun stopServer() = server.close()

    @Test
    fun installsThePackageWithItsFilesAndItsData() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        val observer = RecordingObserver()

        assertEquals(PackageInstallResult.Installed(target.info), installer().install(target, observer))

        assertContentEquals(bundle.map, File(installedDirectory, "map.pmtiles").readBytes())
        assertContentEquals(bundle.places, File(installedDirectory, "places.json").readBytes())
        assertEquals(target.info, InstalledPackages(rootDirectory).list().single().info)
        assertEquals(DownloadProgress(target.totalBytes, target.totalBytes), observer.progress.last())
        assertEquals(DownloadProgress(0, target.totalBytes), observer.progress.first())
        assertFalse(File(rootDirectory, ".downloads/ES-20045").exists())
    }

    @Test
    fun replacesTheInstalledVersionAtOnce() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        installOldVersion()

        installer().install(target, RecordingObserver())

        assertContentEquals(bundle.map, File(installedDirectory, "map.pmtiles").readBytes())
        assertEquals("2026-12-01", infoOf(installedDirectory)?.dataVersion)
        assertEquals(emptyList(), File(rootDirectory, ".replaced").list().orEmpty().toList())
    }

    @Test
    fun aFailedDownloadLeavesTheInstalledVersionAsItWas() {
        server.enqueue(MockResponse.Builder().code(500).build())
        installOldVersion()

        assertEquals(
            PackageInstallResult.Failed(DownloadFailure.SERVER),
            installer().install(target, RecordingObserver()),
        )
        assertEquals("old map", File(installedDirectory, "map.pmtiles").readText())
        assertEquals("2026-11-01", infoOf(installedDirectory)?.dataVersion)
    }

    @Test
    fun aStoppedInstallationResumesWhereItWas() {
        server.dispatcher = BundleDispatcher(bundle.bytes)

        assertEquals(PackageInstallResult.Stopped, installer().install(target, RecordingObserver(stopAfterBytes = 1)))
        assertFalse(installedDirectory.exists())
        val resumed = RecordingObserver()
        assertEquals(PackageInstallResult.Installed(target.info), installer().install(target, resumed))

        assertTrue(resumed.progress.first().downloadedBytes > 0)
        assertContentEquals(bundle.map, File(installedDirectory, "map.pmtiles").readBytes())
    }

    @Test
    fun forgetsTheFilesOfAnotherVersionBeingDownloaded() {
        server.dispatcher = BundleDispatcher(bundle.bytes)
        val stale = File(rootDirectory, ".downloads/ES-20045/map-${"c".repeat(64)}.part")
        stale.parentFile.mkdirs()
        stale.writeText("part of an older version")

        installer().install(target, RecordingObserver())

        assertFalse(stale.exists())
    }

    @Test
    fun checksTheSpaceBeforeDownloadingAnything() {
        val result = installer(usableBytes = 1_000).install(target, RecordingObserver())

        assertEquals(
            PackageInstallResult.NotEnoughSpace(target.totalBytes + PackageInstaller.SPACE_MARGIN_BYTES, 1_000),
            result,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun aPhoneThatCannotWriteFailsOnItsStorage() {
        val notADirectory = File(rootDirectory, "packages").apply { writeText("a file") }
        val installer = PackageInstaller(notADirectory, PackageFileDownloader(HttpDownloads()))

        assertEquals(
            PackageInstallResult.Failed(DownloadFailure.STORAGE),
            installer.install(target, RecordingObserver()),
        )
    }

    @Test
    fun putsBackAPackageWhoseReplacementWasCut() {
        val replaced = File(rootDirectory, ".replaced/ES-20045").apply { mkdirs() }
        File(replaced, "map.pmtiles").writeText("old map")

        installer().recoverInterruptedReplacements()

        assertEquals("old map", File(installedDirectory, "map.pmtiles").readText())
        assertFalse(replaced.exists())
    }

    @Test
    fun deletesThePreviousPackageWhenTheNewOneIsAlreadyInPlace() {
        val replaced = File(rootDirectory, ".replaced/ES-20045").apply { mkdirs() }
        installedDirectory.mkdirs()
        File(installedDirectory, "map.pmtiles").writeText("new map")

        installer().recoverInterruptedReplacements()

        assertEquals("new map", File(installedDirectory, "map.pmtiles").readText())
        assertFalse(replaced.exists())
    }

    private fun installer(usableBytes: Long = Long.MAX_VALUE): PackageInstaller =
        PackageInstaller(rootDirectory, PackageFileDownloader(HttpDownloads())) { usableBytes }

    private fun installOldVersion() {
        installedDirectory.mkdirs()
        File(installedDirectory, "map.pmtiles").writeText("old map")
        File(installedDirectory, "package.json")
            .writeText(InstalledPackageInfoWriter.write(installedPackageInfo(dataVersion = "2026-11-01")))
    }

    private fun infoOf(directory: File) =
        InstalledPackageInfoParser.parseOrNull(File(directory, "package.json").readText())

    private class RecordingObserver(
        private val stopAfterBytes: Long = Long.MAX_VALUE,
    ) : InstallObserver {
        val progress = mutableListOf<DownloadProgress>()

        override fun shouldStop(): Boolean = (progress.lastOrNull()?.downloadedBytes ?: 0) >= stopAfterBytes

        override fun onProgress(progress: DownloadProgress) {
            this.progress += progress
        }
    }
}
