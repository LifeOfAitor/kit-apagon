package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import java.io.File
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The public key in the repository is the one every phone will trust. A change by mistake would make
 * the monthly job refuse to publish, or worse, make the app reject every index.
 */
class PublicKeyOfTheAppTest {
    // Tests run from the folder of the module.
    private val keyText = File("data/clave-publica-del-indice.txt").readText().trim()

    @Test
    fun isAnEcdsaKeyOnTheCurveP256() {
        val key = IndexSignature.publicKeyFrom(keyText) as ECPublicKey

        assertEquals(CURVE_FIELD_SIZE_BITS, key.params.curve.field.fieldSize)
    }

    @Test
    fun isTheKeyCreatedByTheOwnerOnTheDayItWasCreated() {
        val fingerprint =
            MessageDigest
                .getInstance(
                    "SHA-256",
                ).digest(IndexSignature.publicKeyFrom(keyText).encoded)
                .toHexString()

        assertEquals(DOCUMENTED_FINGERPRINT, fingerprint)
    }

    private companion object {
        const val CURVE_FIELD_SIZE_BITS = 256

        // The same fingerprint as in data/README.md.
        const val DOCUMENTED_FINGERPRINT = "c6ba729eea175554ab2bc361d886df7a43feff206ac9effbcfedaad30f397036"
    }
}
