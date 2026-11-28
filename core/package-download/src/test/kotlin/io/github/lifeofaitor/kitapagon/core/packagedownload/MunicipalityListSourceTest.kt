package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidIndexReason
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityListWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.indexSigningKeyPair
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import java.security.PrivateKey
import java.security.Signature
import kotlin.test.Test
import kotlin.test.assertEquals

class MunicipalityListSourceTest {
    private val server = MockWebServer().apply { start() }
    private val keyPair = indexSigningKeyPair()
    private val list = municipalityList()
    private val compressedList = GzipBytes.compress(MunicipalityListWriter.write(list).toByteArray())
    private val source =
        MunicipalityListSource(
            HttpDownloads(),
            keyPair.public,
            PublishedListAddresses(
                server.url("/municipios.json.gz").toString(),
                server.url("/municipios.json.gz.sig").toString(),
            ),
        )

    @AfterEach
    fun stopServer() = server.close()

    @Test
    fun givesTheListWhenItIsSignedWithTheKeyOfTheApp() {
        publish(compressedList, signatureOf(compressedList))

        assertEquals(MunicipalityListFetch.Valid(list), source.fetch())
        assertEquals("/municipios.json.gz.sig", server.takeRequest().url.encodedPath)
        assertEquals("/municipios.json.gz", server.takeRequest().url.encodedPath)
    }

    @Test
    fun asksAgainOnceWhenTheSignatureIsOfAnotherVersion() {
        publish(compressedList, signatureOf(compressedList + 0))
        publish(compressedList, signatureOf(compressedList))

        assertEquals(MunicipalityListFetch.Valid(list), source.fetch())
        assertEquals(4, server.requestCount)
    }

    @Test
    fun neverUsesAListThatIsNotSigned() {
        val otherKey = indexSigningKeyPair()
        repeat(2) { publish(compressedList, signatureOf(compressedList, otherKey.private)) }

        assertEquals(MunicipalityListFetch.NotSigned, source.fetch())
        assertEquals(4, server.requestCount)
    }

    @Test
    fun withoutTheSignatureTheListIsNotDownloaded() {
        server.enqueue(MockResponse.Builder().code(503).build())

        assertEquals(MunicipalityListFetch.Failed(DownloadFailure.SERVER), source.fetch())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun aSignedFileThatIsNotGzipIsDamaged() {
        val notGzip = "not gzip".toByteArray()
        publish(notGzip, signatureOf(notGzip))

        assertEquals(MunicipalityListFetch.Failed(DownloadFailure.DAMAGED), source.fetch())
    }

    @Test
    fun aSignedFileOfANewerFormatAsksToUpdateTheApp() {
        val newer = GzipBytes.compress("{\"format_version\":3}".toByteArray())
        publish(newer, signatureOf(newer))

        assertEquals(MunicipalityListFetch.Invalid(InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION), source.fetch())
    }

    @Test
    fun theAppAsksForTheLatestVersionOfTheDataRepository() {
        assertEquals(
            PublishedListAddresses(
                "https://github.com/LifeOfAitor/kit-apagon-datos/releases/latest/download/municipios.json.gz",
                "https://github.com/LifeOfAitor/kit-apagon-datos/releases/latest/download/municipios.json.gz.sig",
            ),
            PublishedListAddresses.GITHUB,
        )
    }

    private fun publish(
        listBytes: ByteArray,
        signature: ByteArray,
    ) {
        server.enqueue(okResponse(signature))
        server.enqueue(okResponse(listBytes))
    }

    private fun signatureOf(
        bytes: ByteArray,
        privateKey: PrivateKey = keyPair.private,
    ): ByteArray =
        Signature.getInstance(IndexSignature.ALGORITHM).run {
            initSign(privateKey)
            update(bytes)
            sign()
        }
}
