package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import java.io.File

/** What the package generator is asked to do from the command line. */
sealed interface GeneratorCommand {
    /** Development only: converts osmium output into the places of one package (scripts/install-test-package.sh). */
    data class Places(
        val input: File,
        val output: File,
    ) : GeneratorCommand

    /** Prints the rectangle the source map has to cover so that every package has its margin. */
    data class RegionArea(
        val boundaries: File,
        val provinces: String?,
    ) : GeneratorCommand

    /** Writes the packages of a set of provinces and the index part that describes them. */
    data class BuildPackages(
        val boundaries: File,
        val places: File,
        val sourceMap: File,
        val provinces: String?,
        val dataVersion: String,
        val addressTemplate: String,
        val output: File,
    ) : GeneratorCommand

    /**
     * Joins the index parts, signs the result with the key in [SignIndex.SIGNING_KEY_VARIABLE] and
     * checks the signature with [publicKey], the key of the app, before writing anything.
     */
    data class SignIndex(
        val parts: File,
        val publicKey: File,
        val output: File,
    ) : GeneratorCommand {
        companion object {
            const val SIGNING_KEY_VARIABLE = "KIT_APAGON_INDEX_SIGNING_KEY"
        }
    }

    /** Creates the key pair of the index, once, in two files of [output]. */
    data class NewSigningKey(
        val output: File,
    ) : GeneratorCommand

    /** Once a year: converts the list of municipalities of the INE into the file kept in the repository. */
    data class OfficialMunicipalities(
        val spreadsheet: File,
        val output: File,
    ) : GeneratorCommand

    /** Compares the municipalities of OpenStreetMap with the official list and writes the report. */
    data class CheckMunicipalities(
        val boundaries: File,
        val official: File,
        val provinces: String?,
        val report: File,
    ) : GeneratorCommand

    /** Prints the releases left by a failed run of [dataVersion], to delete before starting again. */
    data class ReleaseLeftovers(
        val releases: File,
        val dataVersion: String,
    ) : GeneratorCommand

    /** Prints the releases older than the last [keep] published versions. */
    data class ObsoleteReleases(
        val releases: File,
        val keep: Int,
    ) : GeneratorCommand

    companion object {
        val USAGE =
            """
            Usage:
              places OSMIUM_GEOJSON_INPUT PLACES_JSON_OUTPUT
              region-area --boundaries FILE [--provinces 01,20,48]
              build-packages --boundaries FILE --places FILE --map FILE --data-version YYYY-MM-DD
                             --address-template URL --output DIRECTORY [--provinces 01,20,48]
              sign-index --parts DIRECTORY --public-key FILE --output DIRECTORY
                         (private key in the variable ${SignIndex.SIGNING_KEY_VARIABLE})
              new-signing-key --output DIRECTORY
              official-municipalities --spreadsheet 26codmun.xlsx --output FILE
              check-municipalities --boundaries FILE --official FILE --report FILE [--provinces 01,20,48]
              release-leftovers --releases FILE --data-version YYYY-MM-DD
              obsolete-releases --releases FILE --keep 2
            Boundaries and places are osmium exports in the geojsonseq format. Releases are the output of
            gh release list --json tagName,isDraft,isPrerelease.
            """.trimIndent()

        /** Null when the arguments do not match any command of [USAGE]. */
        fun parseOrNull(arguments: List<String>): GeneratorCommand? {
            val name = arguments.firstOrNull()
            val rest = arguments.drop(1)
            return when (name) {
                "places" -> placesOrNull(rest)
                "region-area" -> regionAreaOrNull(rest)
                "build-packages" -> buildPackagesOrNull(rest)
                "sign-index" -> signIndexOrNull(rest)
                "new-signing-key" -> newSigningKeyOrNull(rest)
                "official-municipalities" -> officialMunicipalitiesOrNull(rest)
                "check-municipalities" -> checkMunicipalitiesOrNull(rest)
                "release-leftovers" -> releaseLeftoversOrNull(rest)
                "obsolete-releases" -> obsoleteReleasesOrNull(rest)
                else -> null
            }
        }

        private fun placesOrNull(arguments: List<String>): Places? =
            if (arguments.size == 2) Places(File(arguments[0]), File(arguments[1])) else null

        private fun regionAreaOrNull(arguments: List<String>): RegionArea? =
            CommandOptions
                .parseOrNull(arguments, required = setOf("boundaries"), optional = setOf("provinces"))
                ?.let { options -> RegionArea(options.file("boundaries"), options.optional("provinces")) }

        private fun buildPackagesOrNull(arguments: List<String>): BuildPackages? =
            CommandOptions
                .parseOrNull(
                    arguments,
                    required = setOf("boundaries", "places", "map", "data-version", "address-template", "output"),
                    optional = setOf("provinces"),
                )?.let { options ->
                    BuildPackages(
                        boundaries = options.file("boundaries"),
                        places = options.file("places"),
                        sourceMap = options.file("map"),
                        provinces = options.optional("provinces"),
                        dataVersion = options.required("data-version"),
                        addressTemplate = options.required("address-template"),
                        output = options.file("output"),
                    )
                }

        private fun signIndexOrNull(arguments: List<String>): SignIndex? =
            CommandOptions
                .parseOrNull(arguments, required = setOf("parts", "public-key", "output"), optional = emptySet())
                ?.let { options ->
                    SignIndex(options.file("parts"), options.file("public-key"), options.file("output"))
                }

        private fun newSigningKeyOrNull(arguments: List<String>): NewSigningKey? =
            CommandOptions
                .parseOrNull(arguments, required = setOf("output"), optional = emptySet())
                ?.let { options -> NewSigningKey(options.file("output")) }

        private fun officialMunicipalitiesOrNull(arguments: List<String>): OfficialMunicipalities? =
            CommandOptions
                .parseOrNull(arguments, required = setOf("spreadsheet", "output"), optional = emptySet())
                ?.let { options -> OfficialMunicipalities(options.file("spreadsheet"), options.file("output")) }

        private fun checkMunicipalitiesOrNull(arguments: List<String>): CheckMunicipalities? =
            CommandOptions
                .parseOrNull(
                    arguments,
                    required = setOf("boundaries", "official", "report"),
                    optional = setOf("provinces"),
                )?.let { options ->
                    CheckMunicipalities(
                        boundaries = options.file("boundaries"),
                        official = options.file("official"),
                        provinces = options.optional("provinces"),
                        report = options.file("report"),
                    )
                }

        private fun releaseLeftoversOrNull(arguments: List<String>): ReleaseLeftovers? =
            CommandOptions
                .parseOrNull(arguments, required = setOf("releases", "data-version"), optional = emptySet())
                ?.let { options -> ReleaseLeftovers(options.file("releases"), options.required("data-version")) }

        private fun obsoleteReleasesOrNull(arguments: List<String>): ObsoleteReleases? =
            CommandOptions
                .parseOrNull(arguments, required = setOf("releases", "keep"), optional = emptySet())
                ?.let { options ->
                    options.required("keep").toIntOrNull()?.let { keep ->
                        ObsoleteReleases(options.file("releases"), keep)
                    }
                }
    }
}

/** Options written as `--name value`, in any order and each one once. */
class CommandOptions private constructor(
    private val values: Map<String, String>,
) {
    fun required(name: String): String = values.getValue(name)

    fun optional(name: String): String? = values[name]

    fun file(name: String): File = File(required(name))

    companion object {
        private const val PREFIX = "--"

        /** Null when an option is unknown, repeated or without value, or a required one is missing. */
        fun parseOrNull(
            arguments: List<String>,
            required: Set<String>,
            optional: Set<String>,
        ): CommandOptions? {
            if (arguments.size % 2 != 0) return null
            val pairs = arguments.chunked(2) { (name, value) -> name.removePrefix(PREFIX) to value }
            val names = pairs.map { (name, _) -> name }
            val isWellFormed =
                arguments.chunked(2).all { (name, value) -> name.startsWith(PREFIX) && !value.startsWith(PREFIX) } &&
                    names.all { name -> name in required || name in optional } &&
                    names.distinct().size == names.size &&
                    names.containsAll(required)
            return if (isWellFormed) CommandOptions(pairs.toMap()) else null
        }
    }
}
