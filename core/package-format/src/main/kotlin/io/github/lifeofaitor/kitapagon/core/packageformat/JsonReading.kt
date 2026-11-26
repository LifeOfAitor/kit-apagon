package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

// Tolerant readers for the JSON files of a package: they return null instead of throwing, so a
// malformed value can be skipped without losing the rest of the file.

internal fun parseJsonOrNull(text: String): JsonElement? =
    try {
        Json.parseToJsonElement(text)
    } catch (expectedForMalformedJson: SerializationException) {
        null
    }

internal fun JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content

internal fun JsonObject.nonBlankStringOrNull(key: String): String? = stringOrNull(key)?.takeIf { it.isNotBlank() }

internal fun JsonObject.longOrNull(key: String): Long? =
    (this[key] as? JsonPrimitive)
        ?.takeUnless {
            it.isString
        }?.longOrNull

internal fun JsonObject.stringListOrEmpty(key: String): List<String> =
    (this[key] as? JsonArray).orEmpty().mapNotNull {
        (it as? JsonPrimitive)
            ?.takeIf { primitive ->
                primitive.isString
            }?.content
    }

internal fun JsonElement.numberOrNull(): Double? = (this as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull

/** The numbers of a JSON array, or null if any element is not a number or the size is not [expectedSize]. */
internal fun JsonElement?.numbersOrNull(expectedSize: Int): List<Double>? {
    val numbers = (this as? JsonArray)?.map { element -> element.numberOrNull() }
    return numbers?.takeIf { it.size == expectedSize && it.all { number -> number != null } }?.filterNotNull()
}
