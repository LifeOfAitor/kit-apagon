package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndex
import io.github.lifeofaitor.kitapagon.core.packageformat.Place
import io.github.lifeofaitor.kitapagon.core.packageformat.PlacesFileWriter
import java.io.File
import java.util.Locale

data class BuiltPackages(
    /** The index part that describes the packages; joined with the other parts by [IndexParts]. */
    val indexPart: PackageIndex,
    val placeCounts: Map<PackageId, Int>,
)

/** Writes the map and the places of each municipality, the two files of its package (decision 0006). */
class PackageBuilder(
    private val mapExtractor: MapExtractor,
) {
    fun build(
        municipalities: List<Municipality>,
        places: List<Place>,
        outputDirectory: File,
        dataVersion: String,
        addressTemplate: DownloadAddressTemplate,
    ): BuiltPackages {
        val placesByMunicipality = PlaceDistributor.distribute(places, municipalities)
        check(outputDirectory.isDirectory || outputDirectory.mkdirs()) { "Cannot create $outputDirectory" }
        municipalities.forEach { municipality ->
            mapExtractor.extract(
                municipality.packageArea,
                File(outputDirectory, IndexBuilder.mapFileNameOf(municipality)),
            )
            // Always written, even empty: the app tells a package without places from a damaged one.
            val placesText = PlacesFileWriter.write(placesByMunicipality.getValue(municipality.id))
            File(outputDirectory, IndexBuilder.placesFileNameOf(municipality)).writeText(placesText)
        }
        return BuiltPackages(
            indexPart = IndexBuilder.build(municipalities, outputDirectory, dataVersion, addressTemplate),
            placeCounts = placesByMunicipality.mapValues { (_, municipalityPlaces) -> municipalityPlaces.size },
        )
    }
}

/** What the generator prints after building, to spot a broken month before it is published. */
object BuiltPackagesSummary {
    private const val BYTES_PER_MEGABYTE = 1_000_000.0

    fun describe(built: BuiltPackages): String {
        val sizes =
            built.indexPart.packages.associateWith { indexedPackage ->
                indexedPackage.files.sumOf { it.sizeBytes }
            }
        val largest = sizes.maxByOrNull { (_, size) -> size }
        val withoutPlaces =
            built.placeCounts
                .filterValues { count -> count == 0 }
                .keys
                .map { id -> id.value }
                .sorted()
        return listOfNotNull(
            "municipalities: ${sizes.size}",
            "places in packages: ${built.placeCounts.values.sum()}",
            "size: ${megabytes(sizes.values.sum())}",
            largest?.let { (indexedPackage, size) ->
                "largest: ${indexedPackage.id} ${indexedPackage.names.official} ${megabytes(size)}"
            },
            "without places: ${withoutPlaces.size}${if (withoutPlaces.isEmpty()) "" else " $withoutPlaces"}",
        ).joinToString("\n")
    }

    private fun megabytes(bytes: Long): String = "%.1f MB".format(Locale.ROOT, bytes / BYTES_PER_MEGABYTE)
}
