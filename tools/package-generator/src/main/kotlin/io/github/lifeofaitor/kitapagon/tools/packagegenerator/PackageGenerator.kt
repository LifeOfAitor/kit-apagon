package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import java.io.File
import kotlin.system.exitProcess

/**
 * Command line entry point of the package generator (decisions 0006, 0011, 0013 and 0014). Any failure
 * ends the program with an exception and a non-zero exit code, so that nothing incomplete is published.
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
    environment: Map<String, String>,
    print: (String) -> Unit,
    mapExtractorFor: (sourceMap: File) -> MapExtractor = ::PmtilesMapExtractor,
) {
    private val packageCommands = PackageCommands(print, mapExtractorFor)
    private val indexCommands = IndexCommands(environment, print)
    private val publicationCommands = PublicationCommands(print)

    fun run(command: GeneratorCommand) =
        when (command) {
            is GeneratorCommand.Places -> packageCommands.places(command)
            is GeneratorCommand.Provinces -> packageCommands.provinces(command)
            is GeneratorCommand.RegionArea -> packageCommands.regionArea(command)
            is GeneratorCommand.BuildPackages -> packageCommands.buildPackages(command)
            is GeneratorCommand.SignIndex -> indexCommands.signIndex(command)
            is GeneratorCommand.NewSigningKey -> indexCommands.newSigningKey(command)
            is GeneratorCommand.OfficialMunicipalities -> publicationCommands.officialMunicipalities(command)
            is GeneratorCommand.CheckMunicipalities -> publicationCommands.checkMunicipalities(command)
            is GeneratorCommand.ReleaseLeftovers -> publicationCommands.releaseLeftovers(command)
            is GeneratorCommand.ObsoleteReleases -> publicationCommands.obsoleteReleases(command)
        }
}

/** The municipalities of the provinces asked for, such as "01,20,48", or of every province. */
object ProvinceSelection {
    private val PROVINCE_CODE = Regex("^[0-9]{2}$")

    /** Null for every province. Throws IllegalArgumentException for a malformed code. */
    fun codesOf(provinces: String?): Set<String>? {
        val codes = provinces?.split(",")?.map { code -> code.trim() }?.toSet() ?: return null
        require(
            codes.all(PROVINCE_CODE::matches),
        ) { "Provinces are two digit INE codes separated by commas: $provinces" }
        return codes
    }

    /**
     * Only the municipalities of the official list of the INE get a package. OpenStreetMap also has
     * lands shared by two municipalities at the same level, with codes the INE does not have, such as
     * the Comunidad de Soto de la Vega y Villazala with 53053 (docs/investigacion/10).
     */
    fun officialMunicipalitiesOf(
        boundaries: File,
        official: File,
        provinces: String?,
    ): List<Municipality> {
        val officialCodes = OfficialMunicipalitiesFile.read(official.readText()).map { it.code }.toSet()
        return selectionOf(
            readMunicipalities(boundaries).filter {
                it.id.value.removePrefix("ES-") in officialCodes
            },
            boundaries,
            provinces,
        )
    }

    /** Every municipality in the boundaries, also the ones the official list does not have. */
    fun municipalitiesOf(
        boundaries: File,
        provinces: String?,
    ): List<Municipality> = selectionOf(readMunicipalities(boundaries), boundaries, provinces)

    private fun readMunicipalities(boundaries: File): List<Municipality> =
        boundaries.useLines { lines -> MunicipalityReader.read(lines) }

    /** Throws IllegalArgumentException for a malformed code or a province without municipalities. */
    private fun selectionOf(
        municipalities: List<Municipality>,
        boundaries: File,
        provinces: String?,
    ): List<Municipality> {
        val wanted = codesOf(provinces)
        if (wanted == null) {
            require(municipalities.isNotEmpty()) { "There are no municipalities in $boundaries." }
            return municipalities
        }
        val selected = municipalities.filter { municipality -> municipality.provinceCode in wanted }
        val missing = wanted - selected.map { municipality -> municipality.provinceCode }.toSet()
        require(missing.isEmpty()) { "There are no municipalities of the provinces $missing in $boundaries." }
        return selected
    }
}

private const val EXIT_CODE_WRONG_USAGE = 2
