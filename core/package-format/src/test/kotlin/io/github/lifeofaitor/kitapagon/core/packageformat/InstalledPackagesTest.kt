package io.github.lifeofaitor.kitapagon.core.packageformat

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class InstalledPackagesTest {
    @TempDir
    lateinit var packagesDirectory: File

    @Test
    fun listsNothingWhenTheDirectoryDoesNotExistYet() {
        val neverCreated = File(packagesDirectory, "not-created")

        assertEquals(emptyList(), InstalledPackages(neverCreated).list())
    }

    @Test
    fun listsCompletePackagesSortedByIdentifier() {
        installPackage("ES-48020")
        installPackage("ES-20045")

        val listedIds = InstalledPackages(packagesDirectory).list().map { it.id.value }

        assertEquals(listOf("ES-20045", "ES-48020"), listedIds)
    }

    @Test
    fun pointsEachPackageToItsMapFile() {
        installPackage("ES-20045")

        val installedPackage = InstalledPackages(packagesDirectory).list().single()

        assertEquals(File(packagesDirectory, "ES-20045/map.pmtiles"), installedPackage.mapFile)
    }

    @Test
    fun ignoresPackagesWithoutAMapFile() {
        File(packagesDirectory, "ES-20045").mkdirs()

        assertEquals(emptyList(), InstalledPackages(packagesDirectory).list())
    }

    @Test
    fun ignoresDirectoriesAndFilesThatAreNotPackages() {
        installPackage("temporary-download")
        File(packagesDirectory, "ES-20045").writeText("a file, not a package directory")

        assertEquals(emptyList(), InstalledPackages(packagesDirectory).list())
    }

    private fun installPackage(directoryName: String) {
        val packageDirectory = File(packagesDirectory, directoryName).apply { mkdirs() }
        File(packageDirectory, InstalledPackages.MAP_FILE_NAME).writeBytes(validHeader())
    }
}
