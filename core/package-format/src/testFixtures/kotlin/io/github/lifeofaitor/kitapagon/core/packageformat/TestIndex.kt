package io.github.lifeofaitor.kitapagon.core.packageformat

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

/** A valid package entry for tests, Irun by default. */
fun indexedPackage(
    id: String = "ES-20045",
    officialName: String = "Irun",
    alternativeNames: List<String> = listOf("Irún"),
): IndexedPackage =
    IndexedPackage(
        id = PackageId.parseOrNull(id) ?: error("Invalid test package id $id"),
        names = MunicipalityNames(officialName, alternativeNames),
        provinceCode = id.removePrefix("ES-").take(2),
        bounds = GeographicBounds(west = -1.8893, south = 43.25947, east = -1.69223, north = 43.38024),
        centre = GeographicPoint(latitude = 43.3198, longitude = -1.7907),
        files =
            listOf(
                PackageFile(
                    name = "$id.pmtiles",
                    sizeBytes = 3_501_019,
                    sha256 = "a".repeat(SHA256_HEX_LENGTH),
                    urls = listOf("$TEST_RELEASE_ADDRESS/$id.pmtiles"),
                ),
            ),
    )

private const val SHA256_HEX_LENGTH = 64
private const val TEST_RELEASE_ADDRESS = "https://example.org/kit-apagon-datos/2026-12-01-ES-20"

fun packageIndex(vararg packages: IndexedPackage): PackageIndex =
    PackageIndex(
        PackageIndex.CURRENT_FORMAT_VERSION,
        "2026-12-01",
        packages.toList().ifEmpty { listOf(indexedPackage()) },
    )

/** A fresh P-256 key pair, the kind the package generator signs the index with. */
fun indexSigningKeyPair(): KeyPair =
    KeyPairGenerator.getInstance(IndexSignature.KEY_ALGORITHM).run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
