package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Reading of the features written by `osmium export`. */
internal object OsmiumFeatures {
    // osmium starts each line of a GeoJSON text sequence with the record separator of RFC 8142.
    private const val RECORD_SEPARATOR = '\u001E'

    /**
     * Reads a GeoJSON text sequence (`osmium export -f geojsonseq`) one line at a time, so that the
     * places of all of Spain never have to be in memory as a single document.
     * Throws IllegalArgumentException when a line is not JSON.
     */
    fun featuresOf(lines: Sequence<String>): Sequence<JsonElement> =
        lines
            .map { line -> line.trimStart(RECORD_SEPARATOR).trim() }
            .filter { line -> line.isNotEmpty() }
            .map { line -> Json.parseToJsonElement(line) }

    /** The OpenStreetMap tags of a feature; osmium writes them as string properties. */
    fun tagsOf(feature: JsonObject): Map<String, String> =
        (feature["properties"] as? JsonObject)
            .orEmpty()
            .mapNotNull { (key, value) -> (value as? JsonPrimitive)?.takeIf { it.isString }?.let { key to it.content } }
            .toMap()
}
