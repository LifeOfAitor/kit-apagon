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

    fun run(command: GeneratorCommand) =
        when (command) {
            is GeneratorCommand.Places -> packageCommands.places(command)
            is GeneratorCommand.RegionArea -> packageCommands.regionArea(command)
            is GeneratorCommand.BuildPackages -> packageCommands.buildPackages(command)
            is GeneratorCommand.SignIndex -> indexCommands.signIndex(command)
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

    /** Throws IllegalArgumentException for a malformed code or a province without municipalities. */
    fun municipalitiesOf(
        boundaries: File,
        provinces: String?,
    ): List<Municipality> {
        val municipalities = boundaries.useLines { lines -> MunicipalityReader.read(lines) }
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
