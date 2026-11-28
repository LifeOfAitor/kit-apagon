package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstalledPackageInfoFileTest {
    @Test
    fun readsWhatItWrites() {
        val info = installedPackageInfo()

        assertEquals(info, InstalledPackageInfoParser.parseOrNull(InstalledPackageInfoWriter.write(info)))
    }

    @Test
    fun writesTheSameKeysAsTheIndex() {
        val written = InstalledPackageInfoWriter.write(installedPackageInfo())

        assertEquals(
            "{\"format_version\":1,\"id\":\"ES-20045\",\"name\":\"Irun\",\"alternative_names\":[\"Irún\"]," +
                "\"province_code\":\"20\",\"province_name\":\"Gipuzkoa\",\"data_version\":\"2026-12-01\"," +
                "\"bounds\":[-1.8893,43.25947,-1.69223,43.38024],\"centre\":[-1.7907,43.3198]}",
            written,
        )
    }

    @Test
    fun refusesDamagedFilesAndFilesOfANewerApp() {
        val written = InstalledPackageInfoWriter.write(installedPackageInfo())

        assertNull(InstalledPackageInfoParser.parseOrNull(written.dropLast(1)))
        assertNull(InstalledPackageInfoParser.parseOrNull("[]"))
        assertNull(
            InstalledPackageInfoParser.parseOrNull(written.replace("\"format_version\":1", "\"format_version\":2")),
        )
        assertNull(InstalledPackageInfoParser.parseOrNull(written.replace("\"ES-20045\"", "\"../etc\"")))
        assertNull(InstalledPackageInfoParser.parseOrNull(written.replace("\"20\"", "\"Gipuzkoa\"")))
        assertNull(InstalledPackageInfoParser.parseOrNull(written.replace("\"2026-12-01\"", "\"\"")))
        assertNull(InstalledPackageInfoParser.parseOrNull(written.replace("-1.8893", "-200")))
    }
}
