package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.indexSigningKeyPair
import io.github.lifeofaitor.kitapagon.core.packageformat.indexedPackage
import io.github.lifeofaitor.kitapagon.core.packageformat.packageIndex
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignedIndexTest {
    @Test
    fun theAppAcceptsTheSignatureMadeWithTheStoredKey() {
        val keyPair = indexSigningKeyPair()
        val storedKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        val indexBytes = PackageIndexWriter.write(packageIndex()).toByteArray()

        val signature = IndexSigner.sign(indexBytes, IndexSigner.privateKeyFrom("$storedKey\n"))

        assertTrue(IndexSignature.isValid(indexBytes, signature, keyPair.public))
        assertFalse(IndexSignature.isValid(indexBytes + ' '.code.toByte(), signature, keyPair.public))
    }

    @Test
    fun refusesAStoredKeyThatIsNotAPrivateKey() {
        val publicKeyText = Base64.getEncoder().encodeToString(indexSigningKeyPair().public.encoded)

        assertFailsWith<IllegalArgumentException> { IndexSigner.privateKeyFrom(publicKeyText) }
        assertFailsWith<IllegalArgumentException> { IndexSigner.privateKeyFrom("not base64!") }
    }

    @Test
    fun joinsThePartsOfEachProvinceInOrder() {
        val bizkaia = packageIndex(indexedPackage("ES-48020", "Bilbao"))
        val gipuzkoa =
            packageIndex(indexedPackage("ES-20069", "Donostia/San Sebastián"), indexedPackage("ES-20045", "Irun"))

        val index = IndexParts.merge(listOf(bizkaia, gipuzkoa).map(PackageIndexWriter::write))

        assertEquals(listOf("ES-20045", "ES-20069", "ES-48020"), index.packages.map { it.id.value })
        assertEquals(bizkaia.dataVersion, index.dataVersion)
    }

    @Test
    fun refusesPartsOfDifferentMonths() {
        val december = packageIndex(indexedPackage("ES-48020"))
        val november = packageIndex(indexedPackage("ES-20045")).copy(dataVersion = "2026-11-01")

        assertFailsWith<IllegalArgumentException> {
            IndexParts.merge(listOf(december, november).map(PackageIndexWriter::write))
        }
    }

    @Test
    fun refusesAPackageThatAppearsInTwoParts() {
        val part = PackageIndexWriter.write(packageIndex(indexedPackage("ES-20045")))

        assertFailsWith<IllegalArgumentException> { IndexParts.merge(listOf(part, part)) }
    }

    @Test
    fun refusesPartsThatAreBrokenOrThatTheAppWouldPartlySkip() {
        val valid = PackageIndexWriter.write(packageIndex())
        val withSkippedPackage = valid.replace("\"ES-20045\"", "\"../../etc\"")

        assertFailsWith<IllegalArgumentException> { IndexParts.merge(listOf(valid, "<html>Too Many Requests</html>")) }
        assertFailsWith<IllegalArgumentException> { IndexParts.merge(listOf(withSkippedPackage)) }
        assertFailsWith<IllegalArgumentException> { IndexParts.merge(emptyList()) }
    }
}
