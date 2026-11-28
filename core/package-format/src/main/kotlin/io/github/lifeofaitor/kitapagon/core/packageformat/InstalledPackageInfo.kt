package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * What the app keeps about each installed package, written when it is installed (decision 0017): the
 * date of its data is shown on the map, and the rest lets the app list the package without the index.
 */
data class InstalledPackageInfo(
    val id: PackageId,
    val names: MunicipalityNames,
    val provinceCode: String,
    val provinceName: String,
    /** Date of the data, such as 2026-12-01. */
    val dataVersion: String,
    val bounds: GeographicBounds,
    val centre: GeographicPoint,
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

/** Reads package.json. Null for a damaged file or one written by a newer version of the app. */
object InstalledPackageInfoParser {
    fun parseOrNull(text: String): InstalledPackageInfo? {
        val root = parseJsonOrNull(text) as? JsonObject
        val formatVersion = root?.longOrNull(IndexJson.FORMAT_VERSION)
        return root?.takeIf { formatVersion == InstalledPackageInfo.FORMAT_VERSION.toLong() }?.let(::infoOrNull)
    }

    private fun infoOrNull(root: JsonObject): InstalledPackageInfo? {
        val municipality = municipalityOf(root)
        val province = provinceOf(root)
        val area = areaOf(root)
        return if (municipality != null && province != null && area != null) {
            InstalledPackageInfo(
                municipality.first,
                municipality.second,
                province.code,
                province.name,
                area.dataVersion,
                area.bounds,
                area.centre,
            )
        } else {
            null
        }
    }

    // The identifier and the names of the municipality.
    private fun municipalityOf(root: JsonObject): Pair<PackageId, MunicipalityNames>? {
        val id = root.stringOrNull(IndexJson.ID)?.let(PackageId::parseOrNull)
        val names = IndexJson.namesOf(root)
        return if (id != null && names != null) id to names else null
    }

    private class Province(
        val code: String,
        val name: String,
    )

    private fun provinceOf(root: JsonObject): Province? {
        val code = root.stringOrNull(IndexJson.PROVINCE_CODE)?.takeIf(IndexJson::isProvinceCode)
        val name = root.nonBlankStringOrNull(IndexJson.PROVINCE_NAME)
        return if (code != null && name != null) Province(code, name) else null
    }

    // The area of the package and the date of its data, which together say what the package covers.
    private class Area(
        val dataVersion: String,
        val bounds: GeographicBounds,
        val centre: GeographicPoint,
    )

    private fun areaOf(root: JsonObject): Area? {
        val dataVersion = root.nonBlankStringOrNull(IndexJson.DATA_VERSION)
        val bounds = GeographicJson.boundsOf(root[IndexJson.BOUNDS])
        val centre = GeographicJson.pointOf(root[IndexJson.CENTRE])
        return if (dataVersion != null && bounds != null && centre != null) Area(dataVersion, bounds, centre) else null
    }
}

/** Writes package.json, with the same keys as the index. */
object InstalledPackageInfoWriter {
    fun write(info: InstalledPackageInfo): String =
        buildJsonObject {
            put(IndexJson.FORMAT_VERSION, InstalledPackageInfo.FORMAT_VERSION)
            put(IndexJson.ID, info.id.value)
            put(IndexJson.NAME, info.names.official)
            put(IndexJson.ALTERNATIVE_NAMES, IndexJson.stringArrayOf(info.names.alternatives))
            put(IndexJson.PROVINCE_CODE, info.provinceCode)
            put(IndexJson.PROVINCE_NAME, info.provinceName)
            put(IndexJson.DATA_VERSION, info.dataVersion)
            put(IndexJson.BOUNDS, GeographicJson.arrayOf(info.bounds))
            put(IndexJson.CENTRE, GeographicJson.arrayOf(info.centre))
        }.toString()
}
