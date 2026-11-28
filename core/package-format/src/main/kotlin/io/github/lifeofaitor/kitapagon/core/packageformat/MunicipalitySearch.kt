package io.github.lifeofaitor.kitapagon.core.packageformat

import java.text.Normalizer
import java.util.Locale

/** A municipality found by a search, with the name of its province to tell apart repeated names. */
data class MunicipalityMatch(
    val municipality: MunicipalityEntry,
    val provinceName: String,
)

/**
 * Finds municipalities by any of their names, ignoring accents, capitals and punctuation (decision
 * 0017): "san sebastian" finds Donostia/San Sebastián. The names are prepared once, so each search
 * only compares short texts, fast enough for every key the person presses on a modest phone.
 */
class MunicipalitySearch(
    list: MunicipalityList,
) {
    private val provinceNames = list.provinces.associate { it.code to it.name }
    private val candidates =
        list.municipalities.map { municipality ->
            Candidate(
                municipality,
                (listOf(municipality.names.official) + municipality.names.alternatives).map(::comparable),
            )
        }

    /** The best matches first: whole name, then start of the name, then start of a word, then any part. */
    fun search(
        query: String,
        maximumResults: Int = DEFAULT_MAXIMUM_RESULTS,
    ): List<MunicipalityMatch> {
        val comparableQuery = comparable(query)
        if (comparableQuery.isEmpty()) return emptyList()
        return candidates
            .mapNotNull { candidate -> candidate.rankFor(comparableQuery)?.let { rank -> rank to candidate } }
            .sortedWith(compareBy({ it.first }, { it.second.names.first() }, { it.second.municipality.id.value }))
            .take(maximumResults)
            .map { (_, candidate) ->
                MunicipalityMatch(candidate.municipality, provinceNames[candidate.municipality.provinceCode].orEmpty())
            }
    }

    private class Candidate(
        val municipality: MunicipalityEntry,
        val names: List<String>,
    ) {
        fun rankFor(query: String): Int? = names.mapNotNull { name -> rankOf(name, query) }.minOrNull()

        private fun rankOf(
            name: String,
            query: String,
        ): Int? =
            when {
                name == query -> WHOLE_NAME
                name.startsWith(query) -> START_OF_NAME
                name.contains(" $query") -> START_OF_WORD
                name.contains(query) -> ANY_PART
                else -> null
            }
    }

    companion object {
        const val DEFAULT_MAXIMUM_RESULTS = 50

        private const val WHOLE_NAME = 0
        private const val START_OF_NAME = 1
        private const val START_OF_WORD = 2
        private const val ANY_PART = 3

        private val COMBINING_MARKS = Regex("\\p{M}+")
        private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

        /** Without accents or capitals, and with any run of punctuation or spaces as one space. */
        fun comparable(text: String): String =
            Normalizer
                .normalize(text, Normalizer.Form.NFD)
                .replace(COMBINING_MARKS, "")
                .lowercase(Locale.ROOT)
                .replace(SEPARATORS, " ")
                .trim()
    }
}
