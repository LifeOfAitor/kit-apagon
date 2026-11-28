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
    fun pointsEachPackageToItsPlacesFileEvenWhenItDoesNotExistYet() {
        installPackage("ES-20045")

        val installedPackage = InstalledPackages(packagesDirectory).list().single()

        assertEquals(File(packagesDirectory, "ES-20045/places.json"), installedPackage.placesFile)
    }

    @Test
    fun ignoresPackagesWithoutAMapFile() {
        File(packagesDirectory, "ES-20045").mkdirs()

        assertEquals(emptyList(), InstalledPackages(packagesDirectory).list())
    }

    @Test
    fun ignoresDirectoriesAndFilesThatAreNotPackages() {
        installPackage("temporary-download")
        installPackage(".downloads")
        File(packagesDirectory, "ES-20045").writeText("a file, not a package directory")

        assertEquals(emptyList(), InstalledPackages(packagesDirectory).list())
    }

    @Test
    fun readsTheDataOfEachPackageTheAppInstalled() {
        val info = installedPackageInfo()
        installPackage(
            "ES-20045",
        ).resolve(InstalledPackages.INFO_FILE_NAME).writeText(InstalledPackageInfoWriter.write(info))

        assertEquals(info, InstalledPackages(packagesDirectory).list().single().info)
    }

    @Test
    fun aPackageWithoutDataOrWithDamagedDataIsStillListed() {
        installPackage("ES-20045")
        installPackage("ES-48020").resolve(InstalledPackages.INFO_FILE_NAME).writeText("{\"format_version\":1")

        assertEquals(listOf(null, null), InstalledPackages(packagesDirectory).list().map { it.info })
    }

    @Test
    fun theMapShowsTheChosenPackageOrTheFirstOneIfItIsNotInstalled() {
        installPackage("ES-20045")
        installPackage("ES-48020")
        val installed = InstalledPackages(packagesDirectory).list()

        assertEquals("ES-48020", installed.shownPackage(PackageId.parseOrNull("ES-48020"))?.id?.value)
        assertEquals("ES-20045", installed.shownPackage(PackageId.parseOrNull("ES-01059"))?.id?.value)
        assertEquals("ES-20045", installed.shownPackage(null)?.id?.value)
        assertEquals(null, emptyList<InstalledPackage>().shownPackage(null))
    }

    @Test
    fun namesEachKindOfFileAsTheDeviceStoresIt() {
        assertEquals("map.pmtiles", InstalledPackages.fileNameOf(PackageFileKind.MAP))
        assertEquals("places.json", InstalledPackages.fileNameOf(PackageFileKind.PLACES))
    }

    private fun installPackage(directoryName: String): File {
        val packageDirectory = File(packagesDirectory, directoryName).apply { mkdirs() }
        File(packageDirectory, InstalledPackages.MAP_FILE_NAME).writeBytes(validHeader())
        return packageDirectory
    }
}
