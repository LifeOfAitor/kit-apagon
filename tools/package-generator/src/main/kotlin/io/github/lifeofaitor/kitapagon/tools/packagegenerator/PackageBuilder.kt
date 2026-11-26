package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexedPackage
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
    /** The tars to publish, one or more per province (decision 0014). */
    val bundles: List<File>,
)

/**
 * Writes the map and the places of each municipality, the two files of its package (decision 0006),
 * and puts the packages of each province together in a tar (decision 0014).
 */
class PackageBuilder(
    private val mapExtractor: MapExtractor,
    private val maximumBundleBytes: Long = MAXIMUM_BUNDLE_BYTES,
) {
    fun build(
        municipalities: List<Municipality>,
        places: List<Place>,
        outputDirectory: File,
        dataVersion: String,
        addressTemplate: DownloadAddressTemplate,
    ): BuiltPackages {
        require(DataVersion.isValid(dataVersion)) { "The data version must be a date such as 2026-12-01: $dataVersion" }
        val looseFilesDirectory = File(outputDirectory, LOOSE_FILES_DIRECTORY_NAME)
        check(looseFilesDirectory.isDirectory || looseFilesDirectory.mkdirs()) { "Cannot create $looseFilesDirectory" }
        val request =
            BuildRequest(
                PlaceDistributor.distribute(places, municipalities),
                outputDirectory,
                looseFilesDirectory,
                dataVersion,
                addressTemplate,
            )
        val provinces =
            municipalities
                .groupBy { municipality -> municipality.provinceCode }
                .toSortedMap()
                .map { (provinceCode, provinceMunicipalities) ->
                    ProvinceBuild(provinceCode, provinceMunicipalities, request).run()
                }
        looseFilesDirectory.deleteRecursively()
        val indexedPackages = provinces.flatMap { it.packages }.sortedBy { indexedPackage -> indexedPackage.id.value }
        return BuiltPackages(
            indexPart = PackageIndex(PackageIndex.CURRENT_FORMAT_VERSION, dataVersion, indexedPackages),
            placeCounts = request.placesByMunicipality.mapValues { (_, municipalityPlaces) -> municipalityPlaces.size },
            bundles = provinces.flatMap { it.bundles },
        )
    }

    /** What every province of one build shares. */
    private class BuildRequest(
        val placesByMunicipality: Map<PackageId, List<Place>>,
        val outputDirectory: File,
        val looseFilesDirectory: File,
        val dataVersion: String,
        val addressTemplate: DownloadAddressTemplate,
    )

    private class BuiltProvince(
        val packages: List<IndexedPackage>,
        val bundles: List<File>,
    )

    private inner class ProvinceBuild(
        private val provinceCode: String,
        private val municipalities: List<Municipality>,
        private val request: BuildRequest,
    ) {
        fun run(): BuiltProvince {
            val looseFiles = municipalities.associateWith(::writeLooseFiles)
            val parts =
                BundlePlan.split(
                    municipalities,
                    maximumBundleBytes,
                ) { looseFiles.getValue(it).map(File::length) }
            val bundleNames = BundlePlan.bundleNamesOf(provinceCode, parts.size)
            val packages =
                parts.zip(bundleNames).flatMap { (partMunicipalities, bundleName) ->
                    val entries = writeBundle(File(request.outputDirectory, bundleName), partMunicipalities, looseFiles)
                    val bundleAddress = request.addressTemplate.addressOf(request.dataVersion, provinceCode, bundleName)
                    partMunicipalities.map { municipality ->
                        IndexBuilder.indexedPackageOf(municipality, entries.getValue(municipality), bundleAddress)
                    }
                }
            looseFiles.values.flatten().forEach(File::delete)
            return BuiltProvince(packages, bundleNames.map { File(request.outputDirectory, it) })
        }

        private fun writeLooseFiles(municipality: Municipality): List<File> {
            val mapFile = File(request.looseFilesDirectory, IndexBuilder.mapFileNameOf(municipality))
            mapExtractor.extract(municipality.packageArea, mapFile)
            // Always written, even empty: the app tells a package without places from a damaged one.
            val placesFile = File(request.looseFilesDirectory, IndexBuilder.placesFileNameOf(municipality))
            placesFile.writeText(PlacesFileWriter.write(request.placesByMunicipality.getValue(municipality.id)))
            return listOf(mapFile, placesFile)
        }

        private fun writeBundle(
            bundle: File,
            partMunicipalities: List<Municipality>,
            looseFiles: Map<Municipality, List<File>>,
        ): Map<Municipality, List<TarEntry>> =
            TarWriter(bundle.outputStream().buffered(), DataVersion.epochSecondsOf(request.dataVersion)).use { tar ->
                partMunicipalities.associateWith { municipality -> looseFiles.getValue(municipality).map(tar::add) }
            }
    }

    companion object {
        // Well below the 2 GiB GitHub accepts per file (docs/investigacion/10-publicacion-mensual-de-los-paquetes.md).
        const val MAXIMUM_BUNDLE_BYTES = 1_500_000_000L
        private const val LOOSE_FILES_DIRECTORY_NAME = "loose-files"
    }
}

/** How the packages of a province are split into tars that stay under a size (decision 0014). */
object BundlePlan {
    /**
     * Keeps the order and the files of each package together, and starts a new tar only when the
     * next package does not fit. A package larger than the limit goes alone in its tar.
     */
    fun <T> split(
        packages: List<T>,
        maximumBytes: Long,
        fileSizesOf: (T) -> List<Long>,
    ): List<List<T>> {
        val parts = mutableListOf<MutableList<T>>()
        val partFileSizes = mutableListOf<MutableList<Long>>()
        packages.forEach { item ->
            val sizes = fileSizesOf(item)
            val fitsInLastPart =
                partFileSizes.lastOrNull()?.let { TarWriter.archiveSizeOf(it + sizes) <= maximumBytes } ?: false
            if (!fitsInLastPart) {
                parts += mutableListOf<T>()
                partFileSizes += mutableListOf<Long>()
            }
            parts.last() += item
            partFileSizes.last() += sizes
        }
        return parts
    }

    /** ES-20.tar for a province in one tar; ES-09-1.tar, ES-09-2.tar when it needs several. */
    fun bundleNamesOf(
        provinceCode: String,
        partCount: Int,
    ): List<String> =
        if (partCount == 1) {
            listOf("ES-$provinceCode.tar")
        } else {
            (1..partCount).map { part -> "ES-$provinceCode-$part.tar" }
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
            "bundles: ${built.bundles.joinToString(", ") { bundle -> "${bundle.name} ${megabytes(bundle.length())}" }}",
        ).joinToString("\n")
    }

    private fun megabytes(bytes: Long): String = "%.1f MB".format(Locale.ROOT, bytes / BYTES_PER_MEGABYTE)
}
