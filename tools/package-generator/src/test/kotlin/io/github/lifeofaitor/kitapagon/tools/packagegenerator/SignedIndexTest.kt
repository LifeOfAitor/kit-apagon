package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListParser
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListResult
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsParser
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsResult
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.TEST_ADDRESSES
import io.github.lifeofaitor.kitapagon.core.packageformat.indexSigningKeyPair
import io.github.lifeofaitor.kitapagon.core.packageformat.packageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.provinceDetails
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignedIndexTest {
    private val gipuzkoa =
        provinceDetails("20", "Gipuzkoa", packageDetails("ES-20069", "Donostia/San Sebastián"), packageDetails())
    private val bizkaia = provinceDetails("48", "Bizkaia", packageDetails("ES-48020", "Bilbao", emptyList()))

    @Test
    fun theAppAcceptsTheSignatureMadeWithTheStoredKey() {
        val keyPair = indexSigningKeyPair()
        val storedKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        val listBytes = IndexParts.merge(listOf(ProvinceDetailsWriter.write(gipuzkoa)), TEST_ADDRESSES).compressedList

        val signature = IndexSigner.sign(listBytes, IndexSigner.privateKeyFrom("$storedKey\n"))

        assertTrue(IndexSignature.isValid(listBytes, signature, keyPair.public))
        assertFalse(IndexSignature.isValid(listBytes + 0, signature, keyPair.public))
    }

    @Test
    fun refusesAStoredKeyThatIsNotAPrivateKey() {
        val publicKeyText = Base64.getEncoder().encodeToString(indexSigningKeyPair().public.encoded)

        assertFailsWith<IllegalArgumentException> { IndexSigner.privateKeyFrom(publicKeyText) }
        assertFailsWith<IllegalArgumentException> { IndexSigner.privateKeyFrom("not base64!") }
    }

    @Test
    fun theListHasEveryMunicipalityInOrderWithItsProvince() {
        val index = IndexParts.merge(listOf(bizkaia, gipuzkoa).map(ProvinceDetailsWriter::write), TEST_ADDRESSES)

        val list =
            assertIs<MunicipalityListResult.Valid>(
                MunicipalityListParser.parse(decompressed(index.compressedList)),
            ).list
        assertEquals(index.list, list)
        assertEquals(listOf("ES-20045", "ES-20069", "ES-48020"), list.municipalities.map { it.id.value })
        assertEquals(listOf("20", "20", "48"), list.municipalities.map { it.provinceCode })
        assertEquals(listOf("20" to "Gipuzkoa", "48" to "Bizkaia"), list.provinces.map { it.code to it.name })
        assertEquals(TEST_ADDRESSES, list.addresses)
    }

    @Test
    fun theListGivesTheSizeAndHashOfEachCompressedProvince() {
        val index = IndexParts.merge(listOf(gipuzkoa, bizkaia).map(ProvinceDetailsWriter::write), TEST_ADDRESSES)

        index.list.provinces.forEach { province ->
            val bytes = index.compressedProvinceDetails.getValue(province.details.name)
            assertEquals(province.details.sizeBytes, bytes.size.toLong())
            assertEquals(province.details.sha256, MessageDigest.getInstance("SHA-256").digest(bytes).toHexString())
        }
        val details =
            assertIs<ProvinceDetailsResult.Valid>(
                ProvinceDetailsParser.parse(
                    decompressed(index.compressedProvinceDetails.getValue("provincia-ES-20.json.gz")),
                ),
            )
        assertEquals(gipuzkoa, details.details)
    }

    @Test
    fun theSamePartsGiveTheSameBytes() {
        val parts = listOf(gipuzkoa, bizkaia).map(ProvinceDetailsWriter::write)

        val first = IndexParts.merge(parts, TEST_ADDRESSES)
        val second = IndexParts.merge(parts.reversed(), TEST_ADDRESSES)

        assertTrue(first.compressedList.contentEquals(second.compressedList))
    }

    @Test
    fun refusesPartsOfDifferentMonthsOrRepeatedProvincesOrPackages() {
        val november = ProvinceDetailsWriter.write(bizkaia.copy(dataVersion = "2026-11-01"))
        val samePackageElsewhere = ProvinceDetailsWriter.write(provinceDetails("48", "Bizkaia", packageDetails()))
        val written = ProvinceDetailsWriter.write(gipuzkoa)

        assertFailsWith<IllegalArgumentException> { IndexParts.merge(listOf(written, november), TEST_ADDRESSES) }
        assertFailsWith<IllegalArgumentException> { IndexParts.merge(listOf(written, written), TEST_ADDRESSES) }
        assertFailsWith<IllegalArgumentException> {
            IndexParts.merge(
                listOf(written, samePackageElsewhere),
                TEST_ADDRESSES,
            )
        }
    }

    @Test
    fun refusesPartsThatAreBrokenOrThatTheAppWouldPartlySkip() {
        val written = ProvinceDetailsWriter.write(gipuzkoa)
        val withSkippedPackage = written.replace("\"ES-20045\"", "\"../../etc\"")

        assertFailsWith<IllegalArgumentException> {
            IndexParts.merge(listOf(written, "<html>Too Many Requests</html>"), TEST_ADDRESSES)
        }
        assertFailsWith<IllegalArgumentException> { IndexParts.merge(listOf(withSkippedPackage), TEST_ADDRESSES) }
        assertFailsWith<IllegalArgumentException> { IndexParts.merge(emptyList(), TEST_ADDRESSES) }
    }

    private fun decompressed(bytes: ByteArray): String =
        GzipBytes.decompressOrNull(bytes, maximumBytes = 10_000_000)!!.decodeToString()
}
