package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.Place

/** One line per category, so whoever runs the generator sees at a glance what a package contains. */
object PlacesSummary {
    fun describe(places: List<Place>): String {
        val countsByCategory =
            places
                .groupingBy { place -> place.category }
                .eachCount()
                .toSortedMap(compareBy { category -> category.ordinal })
        val lines = countsByCategory.map { (category, count) -> "${category.wireName}: $count" }
        return (lines + "total: ${places.size}").joinToString("\n")
    }
}
