package io.github.lifeofaitor.kitapagon.app.development

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImportPushedPackageFilesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun installsThePushedMapAndPlacesAsOnePackage() {
        val pushed = temporaryFolder.newFolder("pushed")
        val packages = temporaryFolder.newFolder("packages")
        File(pushed, "ES-20045.pmtiles").writeText("map")
        File(pushed, "ES-20045.places.json").writeText("places")

        importPushedPackageFiles(pushed, packages)

        assertEquals("map", File(packages, "ES-20045/map.pmtiles").readText())
        assertEquals("places", File(packages, "ES-20045/places.json").readText())
        assertEquals(0, pushed.listFiles()?.size)
    }

    @Test
    fun replacesTheFilesOfAnInstalledPackage() {
        val pushed = temporaryFolder.newFolder("pushed")
        val packages = temporaryFolder.newFolder("packages")
        File(packages, "ES-20045").mkdirs()
        File(packages, "ES-20045/map.pmtiles").writeText("old map")
        File(pushed, "ES-20045.pmtiles").writeText("new map")

        importPushedPackageFiles(pushed, packages)

        assertEquals("new map", File(packages, "ES-20045/map.pmtiles").readText())
    }

    @Test
    fun ignoresDirectoriesAndOtherFiles() {
        val pushed = temporaryFolder.newFolder("pushed")
        val packages = temporaryFolder.newFolder("packages")
        File(pushed, "ES-20045").mkdirs()
        File(pushed, "notes.txt").writeText("not part of a package")

        importPushedPackageFiles(pushed, packages)

        assertEquals(0, packages.listFiles()?.size)
        assertTrue(File(pushed, "notes.txt").exists())
    }
}
