package io.github.lifeofaitor.kitapagon.core.packageformat

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Checks that the index was signed by the package generator (decision 0013). ECDSA with the P-256
 * curve and SHA-256 is available on every Android version the app supports and in Java, unlike
 * Ed25519, which Android only offers from Android 13.
 */
object IndexSignature {
    const val ALGORITHM = "SHA256withECDSA"
    const val KEY_ALGORITHM = "EC"

    /** Reads a public key encoded as Base64 X.509 SubjectPublicKeyInfo, the form embedded in the app. */
    fun publicKeyFrom(base64X509: String): PublicKey =
        KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(base64X509)))

    /** False for a wrong, damaged or malformed signature; it never throws for bad input. */
    fun isValid(
        signedBytes: ByteArray,
        signature: ByteArray,
        publicKey: PublicKey,
    ): Boolean =
        try {
            Signature.getInstance(ALGORITHM).run {
                initVerify(publicKey)
                update(signedBytes)
                verify(signature)
            }
        } catch (expectedForMalformedSignatures: GeneralSecurityException) {
            false
        }
}
