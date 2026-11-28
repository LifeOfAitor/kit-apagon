package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackages
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageStorageTest {
    @TempDir
    lateinit var rootDirectory: File

    private val storage by lazy { PackageStorage(rootDirectory) }
    private val irun = PackageId.parseOrNull("ES-20045")!!
    private val bilbao = PackageId.parseOrNull("ES-48020")!!

    @Test
    fun putsBackAPackageWhoseReplacementWasCut() {
        val replaced = directory(".replaced/ES-20045").also { File(it, "map.pmtiles").writeText("old map") }

        storage.finishInterruptedOperations()

        assertEquals("old map", File(rootDirectory, "ES-20045/map.pmtiles").readText())
        assertFalse(replaced.exists())
    }

    @Test
    fun deletesThePreviousPackageWhenTheNewOneIsAlreadyInPlace() {
        val replaced = directory(".replaced/ES-20045")
        File(directory("ES-20045"), "map.pmtiles").writeText("new map")

        storage.finishInterruptedOperations()

        assertEquals("new map", File(rootDirectory, "ES-20045/map.pmtiles").readText())
        assertFalse(replaced.exists())
    }

    @Test
    fun finishesADeletionThatWasCut() {
        val deleted = directory(".deleted/ES-20045").also { File(it, "map.pmtiles").writeText("map") }

        storage.finishInterruptedOperations()

        assertFalse(deleted.exists())
    }

    @Test
    fun deletesAPackageAndWhatWasDownloadedOfIt() {
        install("ES-20045")
        install("ES-48020")
        val part = File(directory(".downloads/ES-20045"), "map-a.part").apply { writeText("part") }

        assertTrue(storage.uninstall(irun))

        assertEquals(listOf(bilbao), InstalledPackages(rootDirectory).list().map { it.id })
        assertFalse(part.exists())
        assertEquals(emptyList(), File(rootDirectory, ".deleted").list().orEmpty().toList())
    }

    @Test
    fun deletingAPackageThatIsNotInstalledOnlyDiscardsItsDownload() {
        val part = File(directory(".downloads/ES-20045"), "map-a.part").apply { writeText("part") }

        assertTrue(storage.uninstall(irun))
        assertFalse(part.exists())
    }

    @Test
    fun aPhoneThatCannotMoveThePackageKeepsItAndSaysSo() {
        install("ES-20045")
        File(rootDirectory, ".deleted").writeText("a file where the directory should be")

        assertFalse(storage.uninstall(irun))
        assertEquals(listOf(irun), InstalledPackages(rootDirectory).list().map { it.id })
    }

    @Test
    fun discardsOnlyTheDownloadsThatNobodyWillFinish() {
        val cancelled = directory(".downloads/ES-20045")
        val running = directory(".downloads/ES-48020")
        val unknown = directory(".downloads/not-a-package")

        storage.discardDownloadsExcept(setOf(bilbao))

        assertFalse(cancelled.exists())
        assertTrue(running.exists())
        assertFalse(unknown.exists())
    }

    private fun directory(path: String): File = File(rootDirectory, path).apply { mkdirs() }

    private fun install(id: String) {
        File(directory(id), InstalledPackages.MAP_FILE_NAME).writeText("map")
    }
}
