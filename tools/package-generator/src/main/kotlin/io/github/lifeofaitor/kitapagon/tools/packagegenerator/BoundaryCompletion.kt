package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A boundary the extract could not provide, completed with the whole relation from Overpass
 * (decision 0016): a contour with pieces missing in the extract, or tags broken in OpenStreetMap.
 */
data class BoundaryCompletion(
    /** The admin_level the relation has in OpenStreetMap, or null when it has none. */
    val originalAdminLevel: String?,
) {
    /** Why it had to be completed, in Spanish for the report of each version. */
    val reason: String
        get() =
            when (originalAdminLevel) {
                MUNICIPALITY_LEVEL -> "contorno incompleto en el extracto"
                null -> "sin admin_level en OpenStreetMap, conviene corregirlo"
                else -> "etiquetado con admin_level $originalAdminLevel en OpenStreetMap, conviene corregirlo"
            }

    companion object {
        const val SOURCE_PROPERTY = "kitapagon:completed_from"
        const val ORIGINAL_ADMIN_LEVEL_PROPERTY = "kitapagon:original_admin_level"
        const val OVERPASS_SOURCE = "overpass"
        const val MUNICIPALITY_LEVEL = "8"

        /** Null for a boundary that came from the extract as it is. */
        fun from(tags: Map<String, String>): BoundaryCompletion? =
            BoundaryCompletion(tags[ORIGINAL_ADMIN_LEVEL_PROPERTY]).takeIf { tags[SOURCE_PROPERTY] == OVERPASS_SOURCE }
    }
}

/**
 * Turns the relations Overpass returned for the missing municipalities into boundary lines the rest of
 * the generator reads like any other: level 8, with marks that keep where they came from.
 */
object BoundaryCompleter {
    private val POLYGON_TYPES = setOf("Polygon", "MultiPolygon")
    private const val INE_CODE_TAG = "ine:municipio"
    private const val ADMIN_LEVEL_TAG = "admin_level"
    private const val BOUNDARY_TAG = "boundary"
    private const val ADMINISTRATIVE = "administrative"

    /**
     * One line per missing municipality whose relation came back. When several relations share a code,
     * only the one of level 8 is used, and none when none is of level 8.
     */
    fun complete(
        missingCodes: Set<String>,
        overpassFeatureLines: Sequence<String>,
    ): List<String> =
        OsmiumFeatures
            .featuresOf(overpassFeatureLines)
            .mapNotNull { feature -> feature as? JsonObject }
            .filter { feature -> codeOf(feature) in missingCodes && isPolygon(feature) }
            .groupBy { feature -> codeOf(feature) }
            .toSortedMap(compareBy { it })
            .mapNotNull { (_, candidates) -> chosenOf(candidates) }
            .map { feature -> markedAsCompleted(feature).toString() }

    private fun chosenOf(candidates: List<JsonObject>): JsonObject? =
        candidates.singleOrNull() ?: candidates.singleOrNull(::isOfMunicipalityLevel)

    private fun isOfMunicipalityLevel(feature: JsonObject): Boolean =
        OsmiumFeatures.tagsOf(feature)[ADMIN_LEVEL_TAG] == BoundaryCompletion.MUNICIPALITY_LEVEL

    private fun markedAsCompleted(feature: JsonObject): JsonObject {
        val properties = feature["properties"] as? JsonObject ?: JsonObject(emptyMap())
        val originalLevel = OsmiumFeatures.tagsOf(feature)[ADMIN_LEVEL_TAG]
        val marks =
            listOfNotNull(
                ADMIN_LEVEL_TAG to BoundaryCompletion.MUNICIPALITY_LEVEL,
                BOUNDARY_TAG to ADMINISTRATIVE,
                BoundaryCompletion.SOURCE_PROPERTY to BoundaryCompletion.OVERPASS_SOURCE,
                originalLevel?.let { BoundaryCompletion.ORIGINAL_ADMIN_LEVEL_PROPERTY to it },
            ).associate { (key, value) -> key to JsonPrimitive(value) }
        return JsonObject(feature + ("properties" to JsonObject(properties + marks)))
    }

    private fun codeOf(feature: JsonObject): String? = OsmiumFeatures.tagsOf(feature)[INE_CODE_TAG]

    private fun isPolygon(feature: JsonObject): Boolean =
        ((feature["geometry"] as? JsonObject)?.get("type") as? JsonPrimitive)?.content in POLYGON_TYPES
}
