package io.github.lifeofaitor.kitapagon.feature.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the offline principle: the bundled styles must never reference the network, and every
 * font and sprite they reference must be bundled in the app (decision 0010).
 */
class BundledMapAssetsTest {
    private val mapAssetsDirectory = File("src/main/assets/map")

    @Test
    fun everyStyleHasThePlaceholderForThePackageMap() {
        MapTheme.entries.forEach { theme ->
            assertTrue(theme.name, readStyle(theme).contains(MapStyleTemplate.PACKAGE_MAP_PLACEHOLDER))
        }
    }

    @Test
    fun noStyleReferencesTheNetwork() {
        MapTheme.entries.forEach { theme ->
            val style = readStyle(theme)
            assertFalse(theme.name, style.contains("http://") || style.contains("https://"))
        }
    }

    @Test
    fun everyFontUsedByTheStylesIsBundledWithEveryGlyphRange() {
        val referencedFonts = MapTheme.entries.flatMap { theme -> QUOTED_FONT_NAME.findAll(readStyle(theme)).toList() }
        val fontNames = referencedFonts.map { match -> match.groupValues[1] }.toSet() - INTENTIONALLY_EXCLUDED_FONTS

        assertTrue("The styles reference no fonts", fontNames.isNotEmpty())
        fontNames.forEach { fontName ->
            GLYPH_RANGES.forEach { range ->
                val glyphFile = File(mapAssetsDirectory, "fonts/$fontName/$range.pbf")
                assertTrue("Missing ${glyphFile.path}", glyphFile.isFile)
            }
        }
    }

    @Test
    fun everyStyleHasItsSpritesAtBothPixelDensities() {
        MapTheme.entries.forEach { theme ->
            val spriteName = theme.name.lowercase()
            assertTrue(readStyle(theme).contains("\"sprite\":\"asset://map/sprites/$spriteName\""))
            SPRITE_SUFFIXES.forEach { suffix ->
                val spriteFile = File(mapAssetsDirectory, "sprites/$spriteName$suffix")
                assertTrue("Missing ${spriteFile.path}", spriteFile.isFile)
            }
        }
    }

    @Test
    fun namesWrittenInTheLatinAlphabetShowTheOfficialLocalNameBeforeTheTranslation() {
        MapTheme.entries.forEach { theme ->
            val style = readStyle(theme)
            assertFalse(theme.name, style.contains(TRANSLATION_BEFORE_LOCAL_NAME))
            assertTrue(theme.name, style.contains(LOCAL_NAME_BEFORE_TRANSLATION))
        }
    }

    @Test
    fun theBasemapDoesNotDrawTheDrinkingWaterThatTheAppDrawsItself() {
        MapTheme.entries.forEach { theme ->
            val poisLayer =
                Json
                    .parseToJsonElement(readStyle(theme))
                    .jsonObject
                    .getValue("layers")
                    .jsonArray
                    .map { layer -> layer.jsonObject }
                    .single { layer -> layer["source-layer"]?.jsonPrimitive?.content == "pois" }
            assertFalse(theme.name, poisLayer.getValue("filter").toString().contains("drinking_water"))
        }
    }

    @Test
    fun glyphsAreLoadedFromTheApp() {
        MapTheme.entries.forEach { theme ->
            assertTrue(readStyle(theme).contains("\"glyphs\":\"asset://map/fonts/{fontstack}/{range}.pbf\""))
        }
    }

    @Test
    fun themeAssetPathsPointAtTheBundledStyles() {
        val assetPaths = MapTheme.entries.map { theme -> theme.assetPath }.toSet()

        assertEquals(setOf("map/style/light.json", "map/style/dark.json"), assetPaths)
    }

    private fun readStyle(theme: MapTheme): String = File("src/main/assets", theme.assetPath).readText()

    private companion object {
        val QUOTED_FONT_NAME = Regex("\"(Noto Sans [^\"]+)\"")

        // Only used for place names written in Devanagari, which do not exist in Spain (decision 0010).
        val INTENTIONALLY_EXCLUDED_FONTS = setOf("Noto Sans Devanagari Regular v1")
        val GLYPH_RANGES = listOf("0-255", "256-511", "8192-8447")
        const val TRANSLATION_BEFORE_LOCAL_NAME = """["coalesce",["get","name:es"],["get","pgf:name"]"""
        const val LOCAL_NAME_BEFORE_TRANSLATION = """["coalesce",["get","pgf:name"],["get","name"],["get","name:es"]]"""
        val SPRITE_SUFFIXES = listOf(".json", ".png", "@2x.json", "@2x.png")
    }
}
