package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndex
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.PmtilesHeaderReader
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.system.exitProcess

/**
 * Command line entry point of the package generator (decisions 0006, 0011 and 0013). Any failure ends
 * the program with an exception and a non-zero exit code, so that nothing incomplete is published.
 */
fun main(arguments: Array<String>) {
    val command = GeneratorCommand.parseOrNull(arguments.toList())
    if (command == null) {
        System.err.println(GeneratorCommand.USAGE)
        exitProcess(EXIT_CODE_WRONG_USAGE)
    }
    GeneratorCommandRunner(environment = System.getenv(), print = ::println).run(command)
}

class GeneratorCommandRunner(
    private val environment: Map<String, String>,
    private val print: (String) -> Unit,
    private val mapExtractorFor: (sourceMap: File) -> MapExtractor = ::PmtilesMapExtractor,
) {
    fun run(command: GeneratorCommand) =
        when (command) {
            is GeneratorCommand.Places -> runPlaces(command)
            is GeneratorCommand.RegionArea -> runRegionArea(command)
            is GeneratorCommand.BuildPackages -> runBuildPackages(command)
            is GeneratorCommand.SignIndex -> runSignIndex(command)
        }

    private fun runPlaces(command: GeneratorCommand.Places) {
        val places = OsmiumPlacesConverter.convert(command.input.readText())
        command.output.writeText(PlacesFileWriter.write(places))
        print(PlacesSummary.describe(places))
    }

    private fun runRegionArea(command: GeneratorCommand.RegionArea) {
        val municipalities = ProvinceSelection.municipalitiesOf(command.boundaries, command.provinces)
        print(outwardRounded(regionAreaOf(municipalities)))
    }

    private fun runBuildPackages(command: GeneratorCommand.BuildPackages) {
        require(
            IndexBuilder.isValidDataVersion(command.dataVersion),
        ) { "The data version must be a date such as 2026-12-01." }
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

    private fun runSignIndex(command: GeneratorCommand.SignIndex) {
        val signingKey = environment[GeneratorCommand.SignIndex.SIGNING_KEY_VARIABLE]
        require(!signingKey.isNullOrBlank()) {
            "The signing key must be in ${GeneratorCommand.SignIndex.SIGNING_KEY_VARIABLE}."
        }
        val privateKey = IndexSigner.privateKeyFrom(signingKey)
        val partFiles =
            command.parts
                .listFiles { file ->
                    file.isFile && file.extension == "json"
                }.orEmpty()
                .sortedBy { it.name }
        val index = IndexParts.merge(partFiles.map { partFile -> partFile.readText() })
        val indexBytes = PackageIndexWriter.write(index).toByteArray(Charsets.UTF_8)
        check(command.output.isDirectory || command.output.mkdirs()) { "Cannot create ${command.output}" }
        File(command.output, PackageIndex.INDEX_FILE_NAME).writeBytes(indexBytes)
        File(command.output, PackageIndex.SIGNATURE_FILE_NAME).writeBytes(IndexSigner.sign(indexBytes, privateKey))
        print("packages: ${index.packages.size}\ndata version: ${index.dataVersion}")
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
        /** Written next to the packages of a build; [IndexParts] joins them into the signed index. */
        const val INDEX_PART_FILE_NAME = "index-part.json"
        private const val DECIMAL_SCALE = 100_000.0
    }
}

/** The municipalities of the provinces asked for, such as "01,20,48", or of every province. */
object ProvinceSelection {
    private val PROVINCE_CODE = Regex("^[0-9]{2}$")

    /** Throws IllegalArgumentException for a malformed code or a province without municipalities. */
    fun municipalitiesOf(
        boundaries: File,
        provinces: String?,
    ): List<Municipality> {
        val municipalities = boundaries.useLines { lines -> MunicipalityReader.read(lines) }
        val wanted = provinces?.split(",")?.map { code -> code.trim() }?.toSet()
        if (wanted == null) {
            require(municipalities.isNotEmpty()) { "There are no municipalities in $boundaries." }
            return municipalities
        }
        require(
            wanted.all(PROVINCE_CODE::matches),
        ) { "Provinces are two digit INE codes separated by commas: $provinces" }
        val selected = municipalities.filter { municipality -> municipality.provinceCode in wanted }
        val missing = wanted - selected.map { municipality -> municipality.provinceCode }.toSet()
        require(missing.isEmpty()) { "There are no municipalities of the provinces $missing in $boundaries." }
        return selected
    }
}

private const val EXIT_CODE_WRONG_USAGE = 2
