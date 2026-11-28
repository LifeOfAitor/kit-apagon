package io.github.lifeofaitor.kitapagon.core.packageformat

import java.security.PrivateKey
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexSignatureTest {
    private val keyPair = indexSigningKeyPair()
    private val indexBytes = GzipBytes.compress(MunicipalityListWriter.write(municipalityList()).toByteArray())

    @Test
    fun acceptsTheIndexSignedWithThePrivateKey() {
        assertTrue(IndexSignature.isValid(indexBytes, sign(indexBytes, keyPair.private), keyPair.public))
    }

    @Test
    fun rejectsAnIndexChangedAfterSigning() {
        val signature = sign(indexBytes, keyPair.private)
        val tampered =
            indexBytes.copyOf().also { bytes ->
                bytes[bytes.size / 2] = (bytes[bytes.size / 2] + 1).toByte()
            }

        assertFalse(IndexSignature.isValid(tampered, signature, keyPair.public))
    }

    @Test
    fun rejectsASignatureMadeWithAnotherKey() {
        val otherKeyPair = indexSigningKeyPair()

        assertFalse(IndexSignature.isValid(indexBytes, sign(indexBytes, otherKeyPair.private), keyPair.public))
    }

    @Test
    fun rejectsAMalformedSignatureWithoutThrowing() {
        assertFalse(IndexSignature.isValid(indexBytes, "not a signature".toByteArray(), keyPair.public))
        assertFalse(IndexSignature.isValid(indexBytes, ByteArray(0), keyPair.public))
    }

    @Test
    fun readsAPublicKeyEncodedTheWayTheAppEmbedsIt() {
        val encoded = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val publicKey = IndexSignature.publicKeyFrom(encoded)

        assertTrue(IndexSignature.isValid(indexBytes, sign(indexBytes, keyPair.private), publicKey))
    }

    private fun sign(
        bytes: ByteArray,
        privateKey: PrivateKey,
    ): ByteArray =
        Signature.getInstance(IndexSignature.ALGORITHM).run {
            initSign(privateKey)
            update(bytes)
            sign()
        }
}
