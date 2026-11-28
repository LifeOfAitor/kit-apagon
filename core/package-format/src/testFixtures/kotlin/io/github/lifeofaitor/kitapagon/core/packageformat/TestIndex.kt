package io.github.lifeofaitor.kitapagon.core.packageformat

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

const val TEST_DATA_VERSION = "2026-12-01"

val TEST_ADDRESSES =
    AddressTemplates(
        details = listOf("https://example.org/kit-apagon-datos/{data_version}/{file}"),
        bundles = listOf("https://example.org/kit-apagon-datos/{data_version}-ES-{province}/{file}"),
    )

private const val SHA256_HEX_LENGTH = 64
private const val TEST_DETAILS_SIZE_BYTES = 20_480L

/** The details of a valid package for tests, Irun by default, with its map and its places. */
fun packageDetails(
    id: String = "ES-20045",
    officialName: String = "Irun",
    alternativeNames: List<String> = listOf("Irún"),
): PackageDetails =
    PackageDetails(
        id = PackageId.parseOrNull(id) ?: error("Invalid test package id $id"),
        names = MunicipalityNames(officialName, alternativeNames),
        bounds = GeographicBounds(west = -1.8893, south = 43.25947, east = -1.69223, north = 43.38024),
        centre = GeographicPoint(latitude = 43.3198, longitude = -1.7907),
        bundle = "ES-${id.removePrefix("ES-").take(2)}.tar",
        files =
            listOf(
                BundledFile(
                    PackageFileKind.MAP,
                    "$id.pmtiles",
                    offset = 512,
                    sizeBytes = 3_501_019,
                    sha256 = "a".repeat(SHA256_HEX_LENGTH),
                ),
                BundledFile(
                    PackageFileKind.PLACES,
                    "$id.places.json",
                    offset = 3_502_080,
                    sizeBytes = 54_792,
                    sha256 = "b".repeat(SHA256_HEX_LENGTH),
                ),
            ),
    )

fun provinceDetails(
    provinceCode: String = "20",
    provinceName: String = "Gipuzkoa",
    vararg packages: PackageDetails,
): ProvinceDetails =
    ProvinceDetails(
        MunicipalityList.CURRENT_FORMAT_VERSION,
        TEST_DATA_VERSION,
        provinceCode,
        provinceName,
        packages.toList().ifEmpty { listOf(packageDetails()) },
    )

/** A valid list with the given provinces and, by default, Irun. */
fun municipalityList(
    provinces: List<ProvinceEntry> = listOf(provinceEntry()),
    municipalities: List<MunicipalityEntry> = listOf(municipalityEntry()),
): MunicipalityList =
    MunicipalityList(
        MunicipalityList.CURRENT_FORMAT_VERSION,
        TEST_DATA_VERSION,
        TEST_ADDRESSES,
        provinces,
        municipalities,
    )

fun provinceEntry(
    code: String = "20",
    name: String = "Gipuzkoa",
): ProvinceEntry =
    ProvinceEntry(
        code,
        name,
        PublishedFile(MunicipalityList.detailsFileNameOf(code), TEST_DETAILS_SIZE_BYTES, "c".repeat(SHA256_HEX_LENGTH)),
    )

fun municipalityEntry(
    id: String = "ES-20045",
    officialName: String = "Irun",
    alternativeNames: List<String> = listOf("Irún"),
): MunicipalityEntry =
    MunicipalityEntry(
        id = PackageId.parseOrNull(id) ?: error("Invalid test package id $id"),
        names = MunicipalityNames(officialName, alternativeNames),
        provinceCode = id.removePrefix("ES-").take(2),
    )

/** A fresh P-256 key pair, the kind the package generator signs the index with. */
fun indexSigningKeyPair(): KeyPair =
    KeyPairGenerator.getInstance(IndexSignature.KEY_ALGORITHM).run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }

/** What the app keeps about Irun once it is installed. */
fun installedPackageInfo(dataVersion: String = TEST_DATA_VERSION): InstalledPackageInfo {
    val details = packageDetails()
    return InstalledPackageInfo(
        id = details.id,
        names = details.names,
        provinceCode = "20",
        provinceName = "Gipuzkoa",
        dataVersion = dataVersion,
        bounds = details.bounds,
        centre = details.centre,
    )
}
