package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfoParser
import io.github.lifeofaitor.kitapagon.core.packageformat.InstalledPackageInfoWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFile
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageFileKind
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * A package as the background download keeps it until it finishes, even if the app is closed: the data
 * it will save and its files, already checked with the signed list.
 */
object DownloadablePackageJson {
    fun write(target: DownloadablePackage): String =
        buildJsonObject {
            put(INFO, InstalledPackageInfoWriter.write(target.info))
            put(FILES, buildJsonArray { target.files.forEach { add(fileOf(it)) } })
        }.toString()

    /** Null for anything this object did not write. */
    fun parseOrNull(text: String): DownloadablePackage? =
        try {
            parse(text)
        } catch (expectedForTextThisObjectDidNotWrite: InvalidPackageJson) {
            null
        }

    private fun parse(text: String): DownloadablePackage {
        val root = rootOf(text)
        return DownloadablePackage(infoOf(root), filesOf(root))
    }

    private fun infoOf(root: JsonObject) =
        InstalledPackageInfoParser.parseOrNull(root.requiredString(INFO)) ?: throw InvalidPackageJson()

    private fun filesOf(root: JsonObject): List<PackageFile> =
        root
            .requiredArray(FILES)
            .map { element -> fileOf(element as? JsonObject ?: throw InvalidPackageJson()) }
            .ifEmpty { throw InvalidPackageJson() }

    private fun fileOf(file: PackageFile): JsonObject =
        buildJsonObject {
            put(KIND, file.kind.wireName)
            put(NAME, file.name)
            put(OFFSET, file.offset)
            put(SIZE, file.sizeBytes)
            put(SHA256, file.sha256)
            put(URLS, buildJsonArray { file.urls.forEach { add(JsonPrimitive(it)) } })
        }

    private fun fileOf(file: JsonObject): PackageFile =
        PackageFile(
            kind = PackageFileKind.fromWireName(file.requiredString(KIND)) ?: throw InvalidPackageJson(),
            name = file.requiredString(NAME),
            offset = file.requiredLong(OFFSET).takeIf { it >= 0 } ?: throw InvalidPackageJson(),
            sizeBytes = file.requiredLong(SIZE).takeIf { it > 0 } ?: throw InvalidPackageJson(),
            sha256 = file.requiredString(SHA256),
            urls = file.requiredArray(URLS).map { (it as? JsonPrimitive)?.contentOrNull ?: throw InvalidPackageJson() },
        )

    private fun rootOf(text: String): JsonObject =
        try {
            Json.parseToJsonElement(text) as? JsonObject ?: throw InvalidPackageJson()
        } catch (notJson: SerializationException) {
            throw InvalidPackageJson(notJson)
        }

    private const val INFO = "info"
    private const val FILES = "files"
    private const val KIND = "kind"
    private const val NAME = "name"
    private const val OFFSET = "offset"
    private const val SIZE = "size"
    private const val SHA256 = "sha256"
    private const val URLS = "urls"
}

// Each value the reader needs either exists with its type or makes the whole text unusable.
private fun JsonObject.requiredString(key: String): String = string(key) ?: throw InvalidPackageJson()

private fun JsonObject.requiredLong(key: String): Long =
    (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull ?: throw InvalidPackageJson()

private fun JsonObject.requiredArray(key: String): JsonArray = get(key) as? JsonArray ?: throw InvalidPackageJson()

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

private class InvalidPackageJson(
    cause: Throwable? = null,
) : Exception(cause)
