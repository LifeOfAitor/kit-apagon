package io.github.lifeofaitor.kitapagon.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class MapStyleTemplateTest {
    @Test
    fun pointsTheStyleAtTheMapFileOfThePackage() {
        val template = """{"sources":{"protomaps":{"url":"pmtiles://PACKAGE_MAP_URL"}}}"""
        val mapFile = File("/data/user/0/app/files/municipal-packages/ES-20045/map.pmtiles")

        val style = MapStyleTemplate.bindToMapFile(template, mapFile)

        val expectedUrl = "pmtiles://file:///data/user/0/app/files/municipal-packages/ES-20045/map.pmtiles"
        assertEquals("""{"sources":{"protomaps":{"url":"$expectedUrl"}}}""", style)
    }

    @Test
    fun refusesATemplateWithoutThePlaceholder() {
        assertThrows(IllegalArgumentException::class.java) {
            MapStyleTemplate.bindToMapFile("""{"sources":{}}""", File("/map.pmtiles"))
        }
    }
}
