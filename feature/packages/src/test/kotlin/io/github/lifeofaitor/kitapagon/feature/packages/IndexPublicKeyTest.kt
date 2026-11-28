package io.github.lifeofaitor.kitapagon.feature.packages

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class IndexPublicKeyTest {
    // The fingerprint written in tools/package-generator/data/README.md when the key was created.
    @Test
    fun theAppCarriesTheKeyOfTheProject() {
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(IndexPublicKey.load().encoded).toHexString()

        assertEquals("c6ba729eea175554ab2bc361d886df7a43feff206ac9effbcfedaad30f397036", fingerprint)
    }
}
