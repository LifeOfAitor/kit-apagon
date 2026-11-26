package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface PackageIndexResult {
    data class Valid(
        val index: PackageIndex,
        /** Packages that were not valid; the rest of the index is still usable. */
        val skippedPackageCount: Int,
    ) : PackageIndexResult

    data class Invalid(
        val reason: InvalidPackageIndexReason,
    ) : PackageIndexResult
}

enum class InvalidPackageIndexReason {
    NOT_JSON,
    NOT_AN_INDEX,

    /** Written by a newer generator: the app must be updated to read it. */
    UNSUPPORTED_FORMAT_VERSION,
}

/** Reads index.json. Its signature must be checked with IndexSignature before trusting it. */
object PackageIndexParser {
    fun parse(text: String): PackageIndexResult {
        val json = parseJsonOrNull(text)
        val root = json as? JsonObject
        val formatVersion = root?.longOrNull(IndexProperty.FORMAT_VERSION)?.toInt()
        val dataVersion = root?.nonBlankStringOrNull(IndexProperty.DATA_VERSION)
        val packages = root?.get(IndexProperty.PACKAGES) as? JsonArray
        return when {
            json == null -> {
                PackageIndexResult.Invalid(InvalidPackageIndexReason.NOT_JSON)
            }

            formatVersion == null || dataVersion == null || packages == null -> {
                PackageIndexResult.Invalid(InvalidPackageIndexReason.NOT_AN_INDEX)
            }

            formatVersion > PackageIndex.CURRENT_FORMAT_VERSION -> {
                PackageIndexResult.Invalid(InvalidPackageIndexReason.UNSUPPORTED_FORMAT_VERSION)
            }

            else -> {
                validIndexOf(formatVersion, dataVersion, packages)
            }
        }
    }

    private fun validIndexOf(
        formatVersion: Int,
        dataVersion: String,
        packages: JsonArray,
    ): PackageIndexResult.Valid {
        val parsedPackages = packages.map(::toPackageOrNull)
        return PackageIndexResult.Valid(
            PackageIndex(formatVersion, dataVersion, parsedPackages.filterNotNull()),
            skippedPackageCount = parsedPackages.count { it == null },
        )
    }

    private fun toPackageOrNull(element: JsonElement): IndexedPackage? {
        val packageObject = element as? JsonObject ?: return null
        val identity = identityOf(packageObject)
        val area = areaOf(packageObject)
        val files = filesOf(packageObject)
        return if (identity != null && area != null && files != null) {
            IndexedPackage(identity.id, identity.names, identity.provinceCode, area.bounds, area.centre, files)
        } else {
            null
        }
    }

    private data class Identity(
        val id: PackageId,
        val names: MunicipalityNames,
        val provinceCode: String,
    )

    private fun identityOf(packageObject: JsonObject): Identity? {
        val id = packageObject.stringOrNull(IndexProperty.ID)?.let(PackageId::parseOrNull)
        val officialName = packageObject.nonBlankStringOrNull(IndexProperty.NAME)
        val provinceCode = packageObject.nonBlankStringOrNull(IndexProperty.PROVINCE_CODE)
        val alternatives = packageObject.stringListOrEmpty(IndexProperty.ALTERNATIVE_NAMES)
        return if (id != null && officialName != null && provinceCode != null) {
            Identity(id, MunicipalityNames(officialName, alternatives), provinceCode)
        } else {
            null
        }
    }

    private data class Area(
        val bounds: GeographicBounds,
        val centre: GeographicPoint,
    )

    private fun areaOf(packageObject: JsonObject): Area? {
        val bounds =
            packageObject[IndexProperty.BOUNDS]
                .numbersOrNull(BOUNDS_SIZE)
                ?.let(::boundsOf)
                ?.takeIf { it.isValid }
        val centre =
            packageObject[IndexProperty.CENTRE]
                .numbersOrNull(POINT_SIZE)
                ?.let { (longitude, latitude) -> GeographicPoint(latitude, longitude) }
                ?.takeIf { it.isValid }
        return if (bounds != null && centre != null) Area(bounds, centre) else null
    }

    private fun boundsOf(numbers: List<Double>): GeographicBounds =
        GeographicBounds(
            west = numbers[WEST_POSITION],
            south = numbers[SOUTH_POSITION],
            east = numbers[EAST_POSITION],
            north = numbers[NORTH_POSITION],
        )

    /** Null unless every file of the package is valid: a package with a broken file cannot be installed. */
    private fun filesOf(packageObject: JsonObject): List<PackageFile>? {
        val files = (packageObject[IndexProperty.FILES] as? JsonArray)?.map(::toFileOrNull)
        return files?.takeIf { it.isNotEmpty() && it.all { file -> file != null } }?.filterNotNull()
    }

    private fun toFileOrNull(element: JsonElement): PackageFile? {
        val fileObject = element as? JsonObject
        val name = fileObject?.nonBlankStringOrNull(IndexProperty.FILE_NAME)
        val size = fileObject?.longOrNull(IndexProperty.FILE_SIZE)?.takeIf { it > 0 }
        val sha256 = fileObject?.stringOrNull(IndexProperty.FILE_SHA256)?.takeIf { SHA256_FORMAT.matches(it) }
        val urls =
            fileObject
                ?.stringListOrEmpty(IndexProperty.FILE_URLS)
                ?.filter { it.startsWith(HTTPS_PREFIX) }
                ?.takeIf { it.isNotEmpty() }
        return if (name != null && size != null &&
            sha256 != null
        ) {
            urls?.let { PackageFile(name, size, sha256, it) }
        } else {
            null
        }
    }

    private const val BOUNDS_SIZE = 4

    // Bounds are written as [west, south, east, north], like the bbox of GeoJSON.
    private const val WEST_POSITION = 0
    private const val SOUTH_POSITION = 1
    private const val EAST_POSITION = 2
    private const val NORTH_POSITION = 3
    private const val POINT_SIZE = 2
    private val SHA256_FORMAT = Regex("^[0-9a-f]{64}$")

    // Only encrypted connections: a file could be tampered with on the way, and the checksum only
    // protects it if the index itself was not tampered with too.
    private const val HTTPS_PREFIX = "https://"
}

/** Writes index.json. Used by the package generator; the app only reads. */
object PackageIndexWriter {
    fun write(index: PackageIndex): String =
        buildJsonObject {
            put(IndexProperty.FORMAT_VERSION, index.formatVersion)
            put(IndexProperty.DATA_VERSION, index.dataVersion)
            put(IndexProperty.PACKAGES, buildJsonArray { index.packages.forEach { add(packageOf(it)) } })
        }.toString()

    private fun packageOf(indexedPackage: IndexedPackage): JsonObject =
        buildJsonObject {
            put(IndexProperty.ID, indexedPackage.id.value)
            put(IndexProperty.NAME, indexedPackage.names.official)
            put(IndexProperty.ALTERNATIVE_NAMES, stringArrayOf(indexedPackage.names.alternatives))
            put(IndexProperty.PROVINCE_CODE, indexedPackage.provinceCode)
            put(IndexProperty.BOUNDS, numberArrayOf(indexedPackage.bounds.run { listOf(west, south, east, north) }))
            put(IndexProperty.CENTRE, numberArrayOf(indexedPackage.centre.run { listOf(longitude, latitude) }))
            put(IndexProperty.FILES, buildJsonArray { indexedPackage.files.forEach { add(fileOf(it)) } })
        }

    private fun fileOf(file: PackageFile): JsonObject =
        buildJsonObject {
            put(IndexProperty.FILE_NAME, file.name)
            put(IndexProperty.FILE_SIZE, file.sizeBytes)
            put(IndexProperty.FILE_SHA256, file.sha256)
            put(IndexProperty.FILE_URLS, stringArrayOf(file.urls))
        }

    private fun stringArrayOf(values: List<String>): JsonArray =
        buildJsonArray {
            values.forEach { add(JsonPrimitive(it)) }
        }

    private fun numberArrayOf(values: List<Double>): JsonArray =
        buildJsonArray {
            values.forEach { add(JsonPrimitive(it)) }
        }
}

private object IndexProperty {
    const val FORMAT_VERSION = "format_version"
    const val DATA_VERSION = "data_version"
    const val PACKAGES = "packages"
    const val ID = "id"
    const val NAME = "name"
    const val ALTERNATIVE_NAMES = "alternative_names"
    const val PROVINCE_CODE = "province_code"
    const val BOUNDS = "bounds"
    const val CENTRE = "centre"
    const val FILES = "files"
    const val FILE_NAME = "name"
    const val FILE_SIZE = "size"
    const val FILE_SHA256 = "sha256"
    const val FILE_URLS = "urls"
}
