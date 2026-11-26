package io.github.lifeofaitor.kitapagon.tools.packagegenerator

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

    fun releaseLeftovers(command: GeneratorCommand.ReleaseLeftovers) {
        require(DataVersion.isValid(command.dataVersion)) { "The data version must be a date such as 2026-12-01." }
        ReleasePlan.leftoversOf(ReleasePlan.parse(command.releases.readText()), command.dataVersion).forEach(print)
    }

    fun obsoleteReleases(command: GeneratorCommand.ObsoleteReleases) {
        ReleasePlan.obsoleteOf(ReleasePlan.parse(command.releases.readText()), command.keep).forEach(print)
    }
}
