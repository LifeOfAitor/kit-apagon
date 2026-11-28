package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface ProvinceDetailsResult {
    data class Valid(
        val details: ProvinceDetails,
        /** Packages that were not valid; the rest of the province is still usable. */
        val skippedPackageCount: Int,
    ) : ProvinceDetailsResult

    data class Invalid(
        val reason: InvalidIndexReason,
    ) : ProvinceDetailsResult
}

/** Reads provincia-ES-NN.json, already decompressed and checked with the hash the signed list gives. */
object ProvinceDetailsParser {
    fun parse(text: String): ProvinceDetailsResult {
        val json = parseJsonOrNull(text)
        val root = json as? JsonObject
        val formatVersion = root?.longOrNull(IndexJson.FORMAT_VERSION)?.toInt()
        return when {
            json == null -> {
                ProvinceDetailsResult.Invalid(InvalidIndexReason.NOT_JSON)
            }

            root == null || formatVersion == null -> {
                ProvinceDetailsResult.Invalid(InvalidIndexReason.NOT_AN_INDEX)
            }

            formatVersion > MunicipalityList.CURRENT_FORMAT_VERSION -> {
                ProvinceDetailsResult.Invalid(
                    InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION,
                )
            }

            else -> {
                detailsOf(root, formatVersion)
            }
        }
    }

    private fun detailsOf(
        root: JsonObject,
        formatVersion: Int,
    ): ProvinceDetailsResult {
        val dataVersion = root.nonBlankStringOrNull(IndexJson.DATA_VERSION)
        val province = provinceOf(root)
        val packages = root[IndexJson.PACKAGES] as? JsonArray
        if (dataVersion == null || province == null ||
            packages == null
        ) {
            return ProvinceDetailsResult.Invalid(InvalidIndexReason.NOT_AN_INDEX)
        }
        val parsedPackages = packages.map(::toPackageOrNull)
        return ProvinceDetailsResult.Valid(
            ProvinceDetails(
                formatVersion,
                dataVersion,
                province.first,
                province.second,
                parsedPackages.filterNotNull(),
            ),
            skippedPackageCount = parsedPackages.count { it == null },
        )
    }

    // The code and the name of the province.
    private fun provinceOf(root: JsonObject): Pair<String, String>? {
        val code = root.stringOrNull(IndexJson.PROVINCE_CODE)?.takeIf(IndexJson::isProvinceCode)
        val name = root.nonBlankStringOrNull(IndexJson.PROVINCE_NAME)
        return if (code != null && name != null) code to name else null
    }

    private fun toPackageOrNull(element: JsonElement): PackageDetails? {
        val packageObject = element as? JsonObject
        val id = packageObject?.stringOrNull(IndexJson.ID)?.let(PackageId::parseOrNull)
        val names = packageObject?.let(IndexJson::namesOf)
        val content = packageObject?.let(::contentOf)
        return if (id != null && names != null && content != null) {
            PackageDetails(id, names, content.bounds, content.centre, content.bundle, content.files)
        } else {
            null
        }
    }

    private class PackageContent(
        val bounds: GeographicBounds,
        val centre: GeographicPoint,
        val bundle: String,
        val files: List<BundledFile>,
    )

    private fun contentOf(packageObject: JsonObject): PackageContent? {
        val area = areaOf(packageObject)
        val bundle = packageObject.stringOrNull(IndexJson.BUNDLE)?.takeIf(IndexJson::isPublishedName)
        val files = filesOf(packageObject)
        return if (area != null && bundle != null &&
            files != null
        ) {
            PackageContent(area.first, area.second, bundle, files)
        } else {
            null
        }
    }

    private fun areaOf(packageObject: JsonObject): Pair<GeographicBounds, GeographicPoint>? {
        val bounds = GeographicJson.boundsOf(packageObject[IndexJson.BOUNDS])
        val centre = GeographicJson.pointOf(packageObject[IndexJson.CENTRE])
        return if (bounds != null && centre != null) bounds to centre else null
    }

    /** Null unless every file is valid and there is a map: a package without its map cannot be shown. */
    private fun filesOf(packageObject: JsonObject): List<BundledFile>? {
        val files = (packageObject[IndexJson.FILES] as? JsonArray)?.map(::toFileOrNull)
        return files
            ?.takeIf { it.all { file -> file != null } }
            ?.filterNotNull()
            ?.takeIf { valid ->
                valid.count { it.kind == PackageFileKind.MAP } == 1 &&
                    valid.distinctBy { it.kind }.size == valid.size
            }
    }

    private fun toFileOrNull(element: JsonElement): BundledFile? {
        val file = element as? JsonObject
        val kind = file?.stringOrNull(IndexJson.KIND)?.let(PackageFileKind::fromWireName)
        val name = file?.stringOrNull(IndexJson.NAME)?.takeIf(IndexJson::isPublishedName)
        val location = file?.let(::locationOf)
        return if (kind != null && name != null && location != null) {
            BundledFile(kind, name, location.offset, location.sizeBytes, location.sha256)
        } else {
            null
        }
    }

    /** Where the bytes of a file are inside the tar, and the hash they must have. */
    private class FileLocation(
        val offset: Long,
        val sizeBytes: Long,
        val sha256: String,
    )

    private fun locationOf(file: JsonObject): FileLocation? {
        val offset = file.longOrNull(IndexJson.OFFSET)?.takeIf { it >= 0 }
        val size = file.longOrNull(IndexJson.SIZE)?.takeIf { it > 0 }
        val sha256 = file.stringOrNull(IndexJson.SHA256)?.takeIf(IndexJson::isSha256)
        return if (offset != null && size != null && sha256 != null) FileLocation(offset, size, sha256) else null
    }
}

/** Writes provincia-ES-NN.json. Used by the package generator; the app only reads. */
object ProvinceDetailsWriter {
    fun write(details: ProvinceDetails): String =
        buildJsonObject {
            put(IndexJson.FORMAT_VERSION, details.formatVersion)
            put(IndexJson.DATA_VERSION, details.dataVersion)
            put(IndexJson.PROVINCE_CODE, details.provinceCode)
            put(IndexJson.PROVINCE_NAME, details.provinceName)
            put(IndexJson.PACKAGES, buildJsonArray { details.packages.forEach { add(packageOf(it)) } })
        }.toString()

    private fun packageOf(packageDetails: PackageDetails): JsonObject =
        buildJsonObject {
            put(IndexJson.ID, packageDetails.id.value)
            put(IndexJson.NAME, packageDetails.names.official)
            put(IndexJson.ALTERNATIVE_NAMES, IndexJson.stringArrayOf(packageDetails.names.alternatives))
            put(IndexJson.BOUNDS, GeographicJson.arrayOf(packageDetails.bounds))
            put(IndexJson.CENTRE, GeographicJson.arrayOf(packageDetails.centre))
            put(IndexJson.BUNDLE, packageDetails.bundle)
            put(IndexJson.FILES, buildJsonArray { packageDetails.files.forEach { add(fileOf(it)) } })
        }

    private fun fileOf(file: BundledFile): JsonObject =
        buildJsonObject {
            put(IndexJson.KIND, file.kind.wireName)
            put(IndexJson.NAME, file.name)
            put(IndexJson.OFFSET, file.offset)
            put(IndexJson.SIZE, file.sizeBytes)
            put(IndexJson.SHA256, file.sha256)
        }
}
