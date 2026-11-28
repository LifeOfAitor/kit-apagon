package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface MunicipalityListResult {
    data class Valid(
        val list: MunicipalityList,
        /** Provinces and municipalities that were not valid; the rest of the list is still usable. */
        val skippedEntryCount: Int,
    ) : MunicipalityListResult

    data class Invalid(
        val reason: InvalidIndexReason,
    ) : MunicipalityListResult
}

/** Reads municipios.json, already decompressed. Its signature must be checked with IndexSignature first. */
object MunicipalityListParser {
    fun parse(text: String): MunicipalityListResult {
        val json = parseJsonOrNull(text)
        val root = json as? JsonObject
        val formatVersion = root?.longOrNull(IndexJson.FORMAT_VERSION)?.toInt()
        return when {
            json == null -> {
                MunicipalityListResult.Invalid(InvalidIndexReason.NOT_JSON)
            }

            root == null || formatVersion == null -> {
                MunicipalityListResult.Invalid(InvalidIndexReason.NOT_AN_INDEX)
            }

            formatVersion > MunicipalityList.CURRENT_FORMAT_VERSION -> {
                MunicipalityListResult.Invalid(
                    InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION,
                )
            }

            else -> {
                municipalityListOf(root, formatVersion)
            }
        }
    }

    private fun municipalityListOf(
        root: JsonObject,
        formatVersion: Int,
    ): MunicipalityListResult {
        val dataVersion = root.nonBlankStringOrNull(IndexJson.DATA_VERSION)
        val addresses = (root[IndexJson.ADDRESSES] as? JsonObject)?.let(::addressesOf)
        val entries = entriesOf(root)
        return if (dataVersion != null && addresses != null && entries != null) {
            validListOf(MunicipalityList(formatVersion, dataVersion, addresses, emptyList(), emptyList()), entries)
        } else {
            MunicipalityListResult.Invalid(InvalidIndexReason.NOT_AN_INDEX)
        }
    }

    private class Entries(
        val provinces: JsonArray,
        val municipalities: JsonArray,
    )

    private fun entriesOf(root: JsonObject): Entries? {
        val provinces = root[IndexJson.PROVINCES] as? JsonArray
        val municipalities = root[IndexJson.MUNICIPALITIES] as? JsonArray
        return if (provinces != null && municipalities != null) Entries(provinces, municipalities) else null
    }

    private fun validListOf(
        header: MunicipalityList,
        entries: Entries,
    ): MunicipalityListResult.Valid {
        val provinces = entries.provinces
        val municipalities = entries.municipalities
        val parsedProvinces = provinces.map(::toProvinceOrNull)
        val parsedMunicipalities = municipalities.map(::toMunicipalityOrNull)
        return MunicipalityListResult.Valid(
            header.copy(
                provinces = parsedProvinces.filterNotNull(),
                municipalities = parsedMunicipalities.filterNotNull(),
            ),
            skippedEntryCount = parsedProvinces.count { it == null } + parsedMunicipalities.count { it == null },
        )
    }

    // A list nobody can download from is no list at all.
    private fun addressesOf(addresses: JsonObject): AddressTemplates? {
        val details = IndexJson.addressTemplatesOf(addresses[IndexJson.DETAILS])
        val bundles = IndexJson.addressTemplatesOf(addresses[IndexJson.BUNDLES])
        return if (details.isNotEmpty() && bundles.isNotEmpty()) AddressTemplates(details, bundles) else null
    }

    private fun toProvinceOrNull(element: JsonElement): ProvinceEntry? {
        val province = element as? JsonObject
        val code = province?.stringOrNull(IndexJson.CODE)?.takeIf(IndexJson::isProvinceCode)
        val name = province?.nonBlankStringOrNull(IndexJson.NAME)
        val details = (province?.get(IndexJson.DETAILS) as? JsonObject)?.let(::publishedFileOf)
        return if (code != null && name != null && details != null) ProvinceEntry(code, name, details) else null
    }

    private fun publishedFileOf(file: JsonObject): PublishedFile? {
        val name = file.stringOrNull(IndexJson.NAME)?.takeIf(IndexJson::isPublishedName)
        val size = file.longOrNull(IndexJson.SIZE)?.takeIf { it > 0 }
        val sha256 = file.stringOrNull(IndexJson.SHA256)?.takeIf(IndexJson::isSha256)
        return if (name != null && size != null && sha256 != null) PublishedFile(name, size, sha256) else null
    }

    private fun toMunicipalityOrNull(element: JsonElement): MunicipalityEntry? {
        val municipality = element as? JsonObject
        val id = municipality?.stringOrNull(IndexJson.ID)?.let(PackageId::parseOrNull)
        val names = municipality?.let(IndexJson::namesOf)
        val provinceCode = municipality?.stringOrNull(IndexJson.PROVINCE_CODE)?.takeIf(IndexJson::isProvinceCode)
        return if (id != null && names != null &&
            provinceCode != null
        ) {
            MunicipalityEntry(id, names, provinceCode)
        } else {
            null
        }
    }
}

/** Writes municipios.json. Used by the package generator; the app only reads. */
object MunicipalityListWriter {
    fun write(list: MunicipalityList): String =
        buildJsonObject {
            put(IndexJson.FORMAT_VERSION, list.formatVersion)
            put(IndexJson.DATA_VERSION, list.dataVersion)
            put(
                IndexJson.ADDRESSES,
                buildJsonObject {
                    put(IndexJson.DETAILS, IndexJson.stringArrayOf(list.addresses.details))
                    put(IndexJson.BUNDLES, IndexJson.stringArrayOf(list.addresses.bundles))
                },
            )
            put(IndexJson.PROVINCES, buildJsonArray { list.provinces.forEach { add(provinceOf(it)) } })
            put(IndexJson.MUNICIPALITIES, buildJsonArray { list.municipalities.forEach { add(municipalityOf(it)) } })
        }.toString()

    private fun provinceOf(province: ProvinceEntry): JsonObject =
        buildJsonObject {
            put(IndexJson.CODE, province.code)
            put(IndexJson.NAME, province.name)
            put(
                IndexJson.DETAILS,
                buildJsonObject {
                    put(IndexJson.NAME, province.details.name)
                    put(IndexJson.SIZE, province.details.sizeBytes)
                    put(IndexJson.SHA256, province.details.sha256)
                },
            )
        }

    private fun municipalityOf(municipality: MunicipalityEntry): JsonObject =
        buildJsonObject {
            put(IndexJson.ID, municipality.id.value)
            put(IndexJson.NAME, municipality.names.official)
            put(IndexJson.ALTERNATIVE_NAMES, IndexJson.stringArrayOf(municipality.names.alternatives))
            put(IndexJson.PROVINCE_CODE, municipality.provinceCode)
        }
}
