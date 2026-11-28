package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import java.io.File

/**
 * The commands that help the monthly publication: the official list of municipalities and which
 * releases to delete (decision 0014).
 */
class PublicationCommands(
    private val print: (String) -> Unit,
) {
    fun officialMunicipalities(command: GeneratorCommand.OfficialMunicipalities) {
        val municipalities = OfficialSpreadsheetReader.read(command.spreadsheet)
        command.output.writeText(OfficialMunicipalitiesFile.write(municipalities))
        print("municipalities: ${municipalities.size}")
    }

    /** Throws IllegalStateException, after writing the report, when too many municipalities are missing. */
    fun checkMunicipalities(command: GeneratorCommand.CheckMunicipalities) {
        val provinces = ProvinceSelection.codesOf(command.provinces)
        val municipalities = ProvinceSelection.municipalitiesOf(command.boundaries, command.provinces)
        val official = OfficialMunicipalitiesFile.read(command.official.readText())
        val check = MunicipalityCheck.of(municipalities, official, provinces)
        command.report.writeText(check.report() + "\n")
        print(
            listOf(
                "official: ${check.expectedCount}",
                "missing: ${check.missing.size}",
                "unexpected: ${check.unexpected.size}",
            ).joinToString("\n"),
        )
        check(check.isPublishable) {
            "${check.missing.size} of ${check.expectedCount} municipalities have no boundary: " +
                "the data is probably broken and nothing is published."
        }
    }

    fun missingBoundaries(command: GeneratorCommand.MissingBoundaries) {
        missingCodesOf(command.boundaries, command.official, command.provinces).forEach(print)
    }

    fun completeBoundaries(command: GeneratorCommand.CompleteBoundaries) {
        val missingCodes = missingCodesOf(command.boundaries, command.official, command.provinces)
        val completedLines =
            command.overpass.useLines { lines ->
                BoundaryCompleter.complete(missingCodes.toSet(), lines)
            }
        command.output.writeText(completedLines.joinToString("") { line -> line + "\n" })
        print("completed: ${completedLines.size} of ${missingCodes.size}")
    }

    // Every municipality of the boundaries is compared, so that a province without any boundary at all
    // is also missing instead of stopping the command.
    private fun missingCodesOf(
        boundaries: File,
        official: File,
        provinces: String?,
    ): List<String> {
        val municipalities = boundaries.useLines { lines -> MunicipalityReader.read(lines) }
        val officialMunicipalities = OfficialMunicipalitiesFile.read(official.readText())
        return MunicipalityCheck
            .of(
                municipalities,
                officialMunicipalities,
                ProvinceSelection.codesOf(provinces),
            ).missing
            .map {
                it.code
            }
    }

    fun releaseLeftovers(command: GeneratorCommand.ReleaseLeftovers) {
        require(DataVersion.isValid(command.dataVersion)) { "The data version must be a date such as 2026-12-01." }
        ReleasePlan.leftoversOf(ReleasePlan.parse(command.releases.readText()), command.dataVersion).forEach(print)
    }

    fun obsoleteReleases(command: GeneratorCommand.ObsoleteReleases) {
        ReleasePlan.obsoleteOf(ReleasePlan.parse(command.releases.readText()), command.keep).forEach(print)
    }
}
