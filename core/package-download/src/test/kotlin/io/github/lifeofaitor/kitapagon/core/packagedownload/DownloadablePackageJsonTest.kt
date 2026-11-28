package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.packageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.provinceEntry
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadablePackageJsonTest {
    private val target = DownloadablePackage.of(municipalityList(), provinceEntry(), packageDetails())
    private val written = DownloadablePackageJson.write(target)

    @Test
    fun readsWhatItWrites() {
        assertEquals(target, DownloadablePackageJson.parseOrNull(written))
    }

    @Test
    fun fitsWellWithinWhatWorkManagerKeepsForAJob() {
        // WorkManager keeps at most 10,240 bytes of input for each job.
        assertTrue(written.toByteArray().size < 4_096)
    }

    @Test
    fun refusesAnythingItDidNotWrite() {
        assertNull(DownloadablePackageJson.parseOrNull("not json"))
        assertNull(DownloadablePackageJson.parseOrNull("[]"))
        assertNull(DownloadablePackageJson.parseOrNull(written.replace("\"files\":[", "\"files\":[1,")))
        assertNull(DownloadablePackageJson.parseOrNull(written.replace("\"kind\":\"map\"", "\"kind\":\"video\"")))
        assertNull(DownloadablePackageJson.parseOrNull(written.replace("\"offset\":512", "\"offset\":-1")))
        assertNull(DownloadablePackageJson.parseOrNull(written.replace("\"size\":54792", "\"size\":\"54792\"")))
        assertNull(DownloadablePackageJson.parseOrNull(written.replace("\"urls\":[", "\"urls\":[{},")))
        assertNull(DownloadablePackageJson.parseOrNull(written.replace(Regex("\"files\":\\[.*]"), "\"files\":[]")))
        assertNull(
            DownloadablePackageJson.parseOrNull(
                written.replace("\\\"format_version\\\":1", "\\\"format_version\\\":9"),
            ),
        )
    }

    @Test
    fun neverThrowsWhateverTheText() =
        runTest {
            checkAll(2_000, Arb.string(maxSize = 200)) { text ->
                DownloadablePackageJson.parseOrNull(text)
                DownloadablePackageJson.parseOrNull(written.replace("\"files\":[", "\"files\":[$text"))
            }
        }
}
