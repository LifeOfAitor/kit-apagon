package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceCategory
import io.github.lifeofaitor.kitapagon.core.packageformat.PlaceSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PackageGeneratorTest {
    @Test
    fun readsThePlacesCommand() {
        assertEquals(
            GeneratorCommand.Places(File("in.geojson"), File("places.json")),
            GeneratorCommand.parseOrNull(listOf("places", "in.geojson", "places.json")),
        )
    }

    @Test
    fun readsTheRegionAreaCommandWithOrWithoutProvinces() {
        assertEquals(
            GeneratorCommand.RegionArea(File("boundaries.geojsonseq"), "01,20,48"),
            GeneratorCommand.parseOrNull(
                listOf("region-area", "--provinces", "01,20,48", "--boundaries", "boundaries.geojsonseq"),
            ),
        )
        assertEquals(
            GeneratorCommand.RegionArea(File("boundaries.geojsonseq"), null),
            GeneratorCommand.parseOrNull(listOf("region-area", "--boundaries", "boundaries.geojsonseq")),
        )
    }

    @Test
    fun readsTheBuildPackagesCommand() {
        val arguments =
            (
                "build-packages --boundaries b.geojsonseq --places p.geojsonseq --map spain.pmtiles " +
                    "--data-version 2026-12-01 --address-template https://example.org/{file} " +
                    "--output out --provinces 20"
            ).split(" ")

        assertEquals(
            GeneratorCommand.BuildPackages(
                boundaries = File("b.geojsonseq"),
                places = File("p.geojsonseq"),
                sourceMap = File("spain.pmtiles"),
                provinces = "20",
                dataVersion = "2026-12-01",
                addressTemplate = "https://example.org/{file}",
                output = File("out"),
            ),
            GeneratorCommand.parseOrNull(arguments),
        )
    }

    @Test
    fun readsTheSignIndexCommand() {
        assertEquals(
            GeneratorCommand.SignIndex(File("parts"), File("key.txt"), File("published")),
            GeneratorCommand.parseOrNull("sign-index --parts parts --public-key key.txt --output published".split(" ")),
        )
    }

    @Test
    fun readsTheCommandsOfTheMonthlyPublication() {
        assertEquals(GeneratorCommand.Provinces(File("b")), parse("provinces --boundaries b"))
        assertEquals(GeneratorCommand.NewSigningKey(File("keys")), parse("new-signing-key --output keys"))
        assertEquals(
            GeneratorCommand.OfficialMunicipalities(File("26codmun.xlsx"), File("municipios.csv")),
            parse("official-municipalities --spreadsheet 26codmun.xlsx --output municipios.csv"),
        )
        assertEquals(
            GeneratorCommand.CheckMunicipalities(File("b"), File("o.csv"), "20", File("informe.txt")),
            parse("check-municipalities --boundaries b --official o.csv --report informe.txt --provinces 20"),
        )
        assertEquals(
            GeneratorCommand.ReleaseLeftovers(File("r.json"), "2026-12-02"),
            parse("release-leftovers --releases r.json --data-version 2026-12-02"),
        )
        assertEquals(
            GeneratorCommand.ObsoleteReleases(File("r.json"), 2),
            parse("obsolete-releases --releases r.json --keep 2"),
        )
        assertNull(parse("obsolete-releases --releases r.json --keep two"))
    }

    @Test
    fun rejectsAnyOtherCommandLine() {
        assertNull(GeneratorCommand.parseOrNull(listOf()))
        assertNull(GeneratorCommand.parseOrNull(listOf("places", "only-input.geojson")))
        assertNull(GeneratorCommand.parseOrNull(listOf("maps", "in.geojson", "out.json")))
        assertNull(GeneratorCommand.parseOrNull(listOf("region-area")))
        assertNull(GeneratorCommand.parseOrNull(listOf("region-area", "--boundaries")))
        assertNull(GeneratorCommand.parseOrNull(listOf("region-area", "--boundaries", "--provinces")))
        assertNull(GeneratorCommand.parseOrNull(listOf("region-area", "boundaries", "b.geojsonseq")))
        assertNull(GeneratorCommand.parseOrNull(listOf("region-area", "--boundaries", "a", "--boundaries", "b")))
        assertNull(GeneratorCommand.parseOrNull(listOf("region-area", "--boundaries", "a", "--colour", "red")))
        assertNull(GeneratorCommand.parseOrNull(listOf("sign-index", "--parts", "parts")))
        assertNull(GeneratorCommand.parseOrNull(listOf("build-packages", "--boundaries", "b")))
    }

    @Test
    fun summarisesHowManyPlacesOfEachCategoryThereAre() {
        val places =
            listOf(
                place(PlaceCategory.DRINKING_WATER, "node/1"),
                place(PlaceCategory.PHARMACY, "node/2"),
                place(PlaceCategory.DRINKING_WATER, "node/3"),
            )

        assertEquals("pharmacy: 1\ndrinking_water: 2\ntotal: 3", PlacesSummary.describe(places))
    }

    private fun parse(commandLine: String) = GeneratorCommand.parseOrNull(commandLine.split(" "))

    private fun place(
        category: PlaceCategory,
        sourceId: String,
    ) = Place(category, GeographicPoint(43.3, -1.8), PlaceSource.OPENSTREETMAP, sourceId)
}
