package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.AddressTemplates
import io.github.lifeofaitor.kitapagon.core.packageformat.GzipBytes
import io.github.lifeofaitor.kitapagon.core.packageformat.InvalidIndexReason
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceDetailsWriter
import io.github.lifeofaitor.kitapagon.core.packageformat.ProvinceEntry
import io.github.lifeofaitor.kitapagon.core.packageformat.PublishedFile
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.provinceDetails
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals

class ProvinceDetailsSourceTest {
    private val server = MockWebServer().apply { start() }
    private val mirror = MockWebServer().apply { start() }
    private val details = provinceDetails()
    private val compressed = compressed(details)
    private val province = provinceEntryFor(compressed)
    private val source = ProvinceDetailsSource(HttpDownloads())

    @AfterEach
    fun stopServers() {
        server.close()
        mirror.close()
    }

    @Test
    fun givesTheDetailsThatMatchTheSignedList() {
        server.enqueue(okResponse(compressed))

        assertEquals(ProvinceDetailsFetch.Valid(details), source.fetch(listServedBy(server), province))
        assertEquals("/2026-12-01/provincia-ES-20.json.gz", server.takeRequest().url.encodedPath)
    }

    @Test
    fun triesTheNextMirrorWhenAFileIsDamagedOrMissing() {
        server.enqueue(okResponse(compressed.copyOf().also { it[it.size - 1] = 0 }))
        mirror.enqueue(okResponse(compressed))

        assertEquals(ProvinceDetailsFetch.Valid(details), source.fetch(listServedBy(server, mirror), province))
    }

    @Test
    fun failsWhenNoMirrorHasThePublishedFile() {
        server.enqueue(MockResponse.Builder().code(404).build())
        mirror.enqueue(okResponse(compressed + 0))

        assertEquals(
            ProvinceDetailsFetch.Failed(DownloadFailure.SERVER),
            source.fetch(listServedBy(server, mirror), province),
        )
        assertEquals(1, mirror.requestCount)
    }

    @Test
    fun publishedBytesThatAreNotGzipAreDamaged() {
        val notGzip = "not gzip".toByteArray()
        server.enqueue(okResponse(notGzip))

        assertEquals(
            ProvinceDetailsFetch.Failed(DownloadFailure.DAMAGED),
            source.fetch(listServedBy(server), provinceEntryFor(notGzip)),
        )
    }

    @Test
    fun refusesTheDetailsOfAnotherVersionOrProvince() {
        val ofNovember = compressed(details.copy(dataVersion = "2026-11-01"))
        val ofBizkaia = compressed(provinceDetails("48", "Bizkaia"))
        server.enqueue(okResponse(ofNovember))
        server.enqueue(okResponse(ofBizkaia))
        val notAnIndex = ProvinceDetailsFetch.Invalid(InvalidIndexReason.NOT_AN_INDEX)

        assertEquals(notAnIndex, source.fetch(listServedBy(server), provinceEntryFor(ofNovember)))
        assertEquals(notAnIndex, source.fetch(listServedBy(server), provinceEntryFor(ofBizkaia)))
    }

    private fun compressed(details: ProvinceDetails): ByteArray =
        GzipBytes.compress(ProvinceDetailsWriter.write(details).toByteArray())

    private fun provinceEntryFor(bytes: ByteArray): ProvinceEntry =
        ProvinceEntry("20", "Gipuzkoa", PublishedFile("provincia-ES-20.json.gz", bytes.size.toLong(), sha256Of(bytes)))

    // The parser only accepts https addresses, so the tests build the list directly.
    private fun listServedBy(vararg servers: MockWebServer): MunicipalityList =
        municipalityList().copy(
            addresses =
                AddressTemplates(
                    details = servers.map { "${it.url("/")}{data_version}/{file}" },
                    bundles = emptyList(),
                ),
        )
}
