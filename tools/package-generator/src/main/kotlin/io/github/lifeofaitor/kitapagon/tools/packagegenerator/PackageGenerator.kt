package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileWriter
import java.io.File
import kotlin.system.exitProcess

/**
 * Command line entry point of the package generator (decisions 0006 and 0011).
 *
 * Usage: places OSMIUM_GEOJSON_INPUT PLACES_JSON_OUTPUT
 */
fun main(arguments: Array<String>) {
    val command = PlacesCommand.parse(arguments)
    if (command == null) {
        System.err.println(PlacesCommand.USAGE)
        exitProcess(EXIT_CODE_WRONG_USAGE)
    }
    val places = OsmiumPlacesConverter.convert(command.input.readText())
    command.output.writeText(PlacesFileWriter.write(places))
    println(PlacesSummary.describe(places))
}

data class PlacesCommand(
    val input: File,
    val output: File,
) {
    companion object {
        const val USAGE = "Usage: places OSMIUM_GEOJSON_INPUT PLACES_JSON_OUTPUT"
        private const val COMMAND_NAME = "places"
        private const val ARGUMENT_COUNT = 3

        fun parse(arguments: Array<String>): PlacesCommand? =
            if (arguments.size == ARGUMENT_COUNT && arguments[0] == COMMAND_NAME) {
                PlacesCommand(input = File(arguments[1]), output = File(arguments[2]))
            } else {
                null
            }
    }
}

private const val EXIT_CODE_WRONG_USAGE = 2
