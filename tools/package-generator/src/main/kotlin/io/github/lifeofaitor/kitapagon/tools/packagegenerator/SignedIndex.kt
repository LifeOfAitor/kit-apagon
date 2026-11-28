package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.AddressTemplates
import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityEntry
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsParser
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsResult
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceEntry
import io.github.lifeofaitor.kitapagon.core.packageformat.PublishedFile
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/** Signs the index with the private key kept in the secrets of the data repository (decision 0013). */
object IndexSigner {
    /** Throws IllegalArgumentException when the text is not a PKCS#8 P-256 key in Base64. */
    fun privateKeyFrom(base64Pkcs8: String): PrivateKey {
        val keyBytes = Base64.getDecoder().decode(base64Pkcs8.trim())
        return runCatching {
            KeyFactory.getInstance(IndexSignature.KEY_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(keyBytes))
        }.getOrElse { problem ->
            throw IllegalArgumentException("The signing key is not a PKCS#8 EC private key.", problem)
        }
    }

    fun sign(
        indexBytes: ByteArray,
        privateKey: PrivateKey,
    ): ByteArray =
        Signature.getInstance(IndexSignature.ALGORITHM).run {
            initSign(privateKey)
            update(indexBytes)
            sign()
        }
}

/**
 * Creates the key pair of the index, once: the private key goes to the secrets of the data repository
 * and the public key into the app (decision 0013). Both are Base64 text, the form they are stored in.
 */
object SigningKeys {
    const val PRIVATE_KEY_FILE_NAME = "clave-privada-del-indice.txt"
    const val PUBLIC_KEY_FILE_NAME = "clave-publica-del-indice.txt"
    private const val CURVE = "secp256r1"

    data class EncodedKeyPair(
        /** PKCS#8, as [IndexSigner.privateKeyFrom] reads it. */
        val privateKey: String,
        /** X.509 SubjectPublicKeyInfo, as IndexSignature.publicKeyFrom reads it. */
        val publicKey: String,
    )

    fun generate(): EncodedKeyPair {
        val keyPair =
            KeyPairGenerator.getInstance(IndexSignature.KEY_ALGORITHM).run {
                initialize(ECGenParameterSpec(CURVE))
                generateKeyPair()
            }
        val encoder = Base64.getEncoder()
        return EncodedKeyPair(
            encoder.encodeToString(keyPair.private.encoded),
            encoder.encodeToString(keyPair.public.encoded),
        )
    }
}

/** The files of the index ready to publish: the signed list and the details of each province, compressed. */
class PublishedIndex(
    val list: MunicipalityList,
    /** municipios.json.gz, the bytes the signature covers. */
    val compressedList: ByteArray,
    /** provincia-ES-NN.json.gz by file name. */
    val compressedProvinceDetails: Map<String, ByteArray>,
)

/**
 * Joins the parts written province by province into the published index (decision 0017). Any doubt
 * stops the publication, because a wrong index would reach every phone.
 */
object IndexParts {
    /** Throws IllegalArgumentException when the parts cannot form one consistent index. */
    fun merge(
        partTexts: List<String>,
        addresses: AddressTemplates,
    ): PublishedIndex {
        require(partTexts.isNotEmpty()) { "There are no index parts to merge." }
        val parts = partTexts.mapIndexed(::validPartOf).sortedBy { part -> part.provinceCode }
        requireConsistent(parts)
        val compressedDetails =
            parts.associate { part ->
                MunicipalityList.detailsFileNameOf(part.provinceCode) to
                    compressed(ProvinceDetailsWriter.write(part))
            }
        val list =
            MunicipalityList(
                formatVersion = MunicipalityList.CURRENT_FORMAT_VERSION,
                dataVersion = parts.first().dataVersion,
                addresses = addresses,
                provinces = parts.map { part -> provinceEntryOf(part, compressedDetails) },
                municipalities =
                    parts
                        .flatMap { part -> part.packages.map { MunicipalityEntry(it.id, it.names, part.provinceCode) } }
                        .sortedBy { municipality -> municipality.id.value },
            )
        return PublishedIndex(list, compressed(MunicipalityListWriter.write(list)), compressedDetails)
    }

    // One month of data, and each province and each package only once.
    private fun requireConsistent(parts: List<ProvinceDetails>) {
        val dataVersions = parts.map { part -> part.dataVersion }.distinct()
        require(dataVersions.size == 1) { "The index parts belong to different data versions: $dataVersions" }
        val repeatedProvinces = parts.groupBy { part -> part.provinceCode }.filterValues { it.size > 1 }.keys
        require(repeatedProvinces.isEmpty()) { "These provinces appear in more than one part: $repeatedProvinces" }
        val repeatedIds =
            parts
                .flatMap { part -> part.packages }
                .groupBy { it.id }
                .filterValues { it.size > 1 }
                .keys
        require(repeatedIds.isEmpty()) { "These packages appear in more than one part: $repeatedIds" }
    }

    private fun provinceEntryOf(
        part: ProvinceDetails,
        compressedDetails: Map<String, ByteArray>,
    ): ProvinceEntry {
        val fileName = MunicipalityList.detailsFileNameOf(part.provinceCode)
        val bytes = compressedDetails.getValue(fileName)
        return ProvinceEntry(
            part.provinceCode,
            part.provinceName,
            PublishedFile(fileName, bytes.size.toLong(), sha256Of(bytes)),
        )
    }

    private fun compressed(text: String): ByteArray = GzipBytes.compress(text.toByteArray(Charsets.UTF_8))

    private fun sha256Of(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

    private fun validPartOf(
        position: Int,
        partText: String,
    ): ProvinceDetails {
        val result = ProvinceDetailsParser.parse(partText)
        require(result is ProvinceDetailsResult.Valid) { "Index part ${position + 1} is not valid: $result" }
        require(result.skippedPackageCount == 0) {
            "Index part ${position + 1} has ${result.skippedPackageCount} packages the app would skip."
        }
        return result.details
    }
}
