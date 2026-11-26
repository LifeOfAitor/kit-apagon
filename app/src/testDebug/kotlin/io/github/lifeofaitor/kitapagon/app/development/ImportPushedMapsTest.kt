package io.github.lifeofaitor.kitapagon.app.development

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImportPushedMapsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun installsAPushedMapAsAPackage() {
        val pushed = temporaryFolder.newFolder("pushed")
        val packages = temporaryFolder.newFolder("packages")
        File(pushed, "ES-20045.pmtiles").writeText("new map")

        importPushedMaps(pushed, packages)

        assertEquals("new map", File(packages, "ES-20045/map.pmtiles").readText())
        assertFalse(File(pushed, "ES-20045.pmtiles").exists())
    }

    @Test
    fun replacesTheMapOfAnInstalledPackage() {
        val pushed = temporaryFolder.newFolder("pushed")
        val packages = temporaryFolder.newFolder("packages")
        File(packages, "ES-20045").mkdirs()
        File(packages, "ES-20045/map.pmtiles").writeText("old map")
        File(pushed, "ES-20045.pmtiles").writeText("new map")

        importPushedMaps(pushed, packages)

        assertEquals("new map", File(packages, "ES-20045/map.pmtiles").readText())
    }

    @Test
    fun ignoresDirectoriesAndOtherFiles() {
        val pushed = temporaryFolder.newFolder("pushed")
        val packages = temporaryFolder.newFolder("packages")
        File(pushed, "ES-20045").mkdirs()
        File(pushed, "notes.txt").writeText("not a map")

        importPushedMaps(pushed, packages)

        assertEquals(0, packages.listFiles()?.size)
    }
}
