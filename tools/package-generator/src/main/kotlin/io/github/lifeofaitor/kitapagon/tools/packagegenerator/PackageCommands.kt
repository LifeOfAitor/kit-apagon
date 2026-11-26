package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderReader
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/** The commands that turn OpenStreetMap data and a map into packages. */
class PackageCommands(
    private val print: (String) -> Unit,
    private val mapExtractorFor: (sourceMap: File) -> MapExtractor,
) {
    fun places(command: GeneratorCommand.Places) {
        val places = OsmiumPlacesConverter.convert(command.input.readText())
        command.output.writeText(PlacesFileWriter.write(places))
        print(PlacesSummary.describe(places))
    }

    fun provinces(command: GeneratorCommand.Provinces) {
        ProvinceSelection
            .municipalitiesOf(command.boundaries, provinces = null)
            .map { municipality -> municipality.provinceCode }
            .distinct()
            .sorted()
            .forEach(print)
    }

    fun regionArea(command: GeneratorCommand.RegionArea) {
        val municipalities = ProvinceSelection.municipalitiesOf(command.boundaries, command.provinces)
        print(outwardRounded(regionAreaOf(municipalities)))
    }

    fun buildPackages(command: GeneratorCommand.BuildPackages) {
        require(DataVersion.isValid(command.dataVersion)) { "The data version must be a date such as 2026-12-01." }
        val addressTemplate = DownloadAddressTemplate(command.addressTemplate)
        val municipalities = ProvinceSelection.municipalitiesOf(command.boundaries, command.provinces)
        SourceMapCoverage.check(PmtilesHeaderReader.read(command.sourceMap), regionAreaOf(municipalities))
        val places = command.places.useLines { lines -> OsmiumPlacesConverter.convertLines(lines) }
        val built =
            PackageBuilder(mapExtractorFor(command.sourceMap))
                .build(municipalities, places, command.output, command.dataVersion, addressTemplate)
        File(command.output, INDEX_PART_FILE_NAME).writeText(PackageIndexWriter.write(built.indexPart))
        print(BuiltPackagesSummary.describe(built))
    }

    private fun regionAreaOf(municipalities: List<Municipality>): GeographicBounds =
        checkNotNull(municipalities.map { municipality -> municipality.packageArea }.unionOrNull())

    // Rounded outwards so that the printed rectangle never cuts a margin short.
    private fun outwardRounded(area: GeographicBounds): String =
        listOf(
            floor(area.west * DECIMAL_SCALE),
            floor(area.south * DECIMAL_SCALE),
            ceil(area.east * DECIMAL_SCALE),
            ceil(area.north * DECIMAL_SCALE),
        ).joinToString(",") { scaled -> "%.5f".format(Locale.ROOT, scaled / DECIMAL_SCALE) }

    companion object {
        /** Written next to the tars of a build; [IndexParts] joins them into the signed index. */
        const val INDEX_PART_FILE_NAME = "index-part.json"
        private const val DECIMAL_SCALE = 100_000.0
    }
}
