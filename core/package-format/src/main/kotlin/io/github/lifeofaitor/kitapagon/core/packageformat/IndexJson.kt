package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/** Why a downloaded index file cannot be used at all. */
enum class InvalidIndexReason {
    NOT_JSON,
    NOT_AN_INDEX,

    /** Written by a newer generator: the app must be updated to read it. */
    UNSUPPORTED_FORMAT_VERSION,
}

/** The JSON keys and the checks the two files of the index share (decision 0017). */
internal object IndexJson {
    const val FORMAT_VERSION = "format_version"
    const val DATA_VERSION = "data_version"
    const val ADDRESSES = "addresses"
    const val DETAILS = "details"
    const val BUNDLES = "bundles"
    const val PROVINCES = "provinces"
    const val MUNICIPALITIES = "municipalities"
    const val PACKAGES = "packages"
    const val ID = "id"
    const val CODE = "code"
    const val NAME = "name"
    const val ALTERNATIVE_NAMES = "alternative_names"
    const val PROVINCE_CODE = "province_code"
    const val PROVINCE_NAME = "province_name"
    const val BOUNDS = "bounds"
    const val CENTRE = "centre"
    const val BUNDLE = "bundle"
    const val FILES = "files"
    const val KIND = "kind"
    const val OFFSET = "offset"
    const val SIZE = "size"
    const val SHA256 = "sha256"

    private val SHA256_FORMAT = Regex("^[0-9a-f]{64}$")
    private val PROVINCE_CODE_FORMAT = Regex("^[0-9]{2}$")

    // Names that become part of a download address: letters, digits, dots, dashes and underscores only.
    private val PUBLISHED_NAME_FORMAT = Regex("^[A-Za-z0-9._-]{1,100}$")

    // Only encrypted connections: a file could be tampered with on the way, and the hash only protects
    // it if the index itself was not tampered with too.
    private const val HTTPS_PREFIX = "https://"
    private const val FILE_PLACEHOLDER = "{file}"

    fun isSha256(value: String): Boolean = SHA256_FORMAT.matches(value)

    fun isProvinceCode(value: String): Boolean = PROVINCE_CODE_FORMAT.matches(value)

    fun isPublishedName(value: String): Boolean = PUBLISHED_NAME_FORMAT.matches(value)

    fun addressTemplatesOf(element: JsonElement?): List<String> =
        (element as? JsonArray)
            .orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
            .filter { template -> template.startsWith(HTTPS_PREFIX) && FILE_PLACEHOLDER in template }

    fun namesOf(entry: JsonObject): MunicipalityNames? =
        entry.nonBlankStringOrNull(NAME)?.let { official ->
            MunicipalityNames(official, entry.stringListOrEmpty(ALTERNATIVE_NAMES))
        }

    fun stringArrayOf(values: List<String>): JsonArray = buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }

    fun numberArrayOf(values: List<Double>): JsonArray = buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }
}
