package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** A release of the data repository, as `gh release list --json tagName,isDraft,isPrerelease` lists it. */
data class DataRelease(
    val tag: String,
    val isDraft: Boolean,
    val isPrerelease: Boolean,
) {
    /** The data version the release belongs to, or null for a release this generator did not create. */
    val dataVersion: String? get() =
        INDEX_TAG.matchEntire(tag)?.value
            ?: PROVINCE_TAG.matchEntire(tag)?.groupValues?.get(1)

    /** The release of a month that holds index.json, once it is no longer a draft. */
    val isPublishedIndex: Boolean get() = INDEX_TAG.matches(tag) && !isDraft && !isPrerelease

    private companion object {
        val INDEX_TAG = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
        val PROVINCE_TAG = Regex("^([0-9]{4}-[0-9]{2}-[0-9]{2})-ES-[0-9]{2}$")
    }
}

/**
 * Which releases to delete so that a month starts clean and only the last versions remain
 * (decision 0014). Releases with other names are never touched.
 */
object ReleasePlan {
    /** Throws IllegalArgumentException when the text is not the list gh writes. */
    fun parse(releasesJson: String): List<DataRelease> {
        val releases = Json.parseToJsonElement(releasesJson) as? JsonArray
        requireNotNull(releases) { "The list of releases is not a JSON array." }
        return releases.map { element ->
            val release = requireNotNull(element as? JsonObject) { "A release is not a JSON object: $element" }
            DataRelease(
                tag =
                    requireNotNull(
                        (release["tagName"] as? JsonPrimitive)?.content,
                    ) { "A release has no tagName: $release" },
                isDraft = (release["isDraft"] as? JsonPrimitive)?.booleanOrNull ?: false,
                isPrerelease = (release["isPrerelease"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }
    }

    /**
     * What an earlier failed run of the same day left: provinces and a draft index that the app never
     * saw. Throws IllegalStateException when that day is already published, so it is never overwritten.
     */
    fun leftoversOf(
        releases: List<DataRelease>,
        dataVersion: String,
    ): List<String> {
        check(releases.none { it.isPublishedIndex && it.dataVersion == dataVersion }) {
            "The data of $dataVersion is already published; it is never replaced."
        }
        return releases.filter { it.dataVersion == dataVersion }.map { it.tag }.sorted()
    }

    /**
     * Every release outside the [keep] newest published versions. The previous version stays so that a
     * phone that read the old index can still download its files.
     */
    fun obsoleteOf(
        releases: List<DataRelease>,
        keep: Int,
    ): List<String> {
        require(keep >= 1) { "At least the newest version must be kept." }
        val keptVersions =
            releases
                .filter { it.isPublishedIndex }
                .mapNotNull { it.dataVersion }
                .distinct()
                .sortedDescending()
                .take(keep)
                .toSet()
        if (keptVersions.isEmpty()) return emptyList()
        return releases
            .filter { release ->
                release.dataVersion?.let { it !in keptVersions } ?: false
            }.map { it.tag }
            .sorted()
    }
}
