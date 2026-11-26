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

    /** Joins the index parts and signs the result with the key in [SignIndex.SIGNING_KEY_VARIABLE]. */
    data class SignIndex(
        val parts: File,
        val output: File,
    ) : GeneratorCommand {
        companion object {
            const val SIGNING_KEY_VARIABLE = "KIT_APAGON_INDEX_SIGNING_KEY"
        }
    }

    companion object {
        val USAGE =
            """
            Usage:
              places OSMIUM_GEOJSON_INPUT PLACES_JSON_OUTPUT
              region-area --boundaries FILE [--provinces 01,20,48]
              build-packages --boundaries FILE --places FILE --map FILE --data-version YYYY-MM-DD
                             --address-template URL --output DIRECTORY [--provinces 01,20,48]
              sign-index --parts DIRECTORY --output DIRECTORY   (key in ${SignIndex.SIGNING_KEY_VARIABLE})
            Boundaries and places are osmium exports in the geojsonseq format.
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
                .parseOrNull(arguments, required = setOf("parts", "output"), optional = emptySet())
                ?.let { options -> SignIndex(options.file("parts"), options.file("output")) }
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
