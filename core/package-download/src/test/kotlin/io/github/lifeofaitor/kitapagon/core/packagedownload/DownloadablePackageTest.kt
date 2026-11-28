package io.github.lifeofaitor.kitapagon.core.packagedownload

import io.github.lifeofaitor.kitapagon.core.packageformat.installedPackageInfo
import io.github.lifeofaitor.kitapagon.core.packageformat.municipalityList
import io.github.lifeofaitor.kitapagon.core.packageformat.packageDetails
import io.github.lifeofaitor.kitapagon.core.packageformat.provinceEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadablePackageTest {
    @Test
    fun keepsTheDataOfTheListAndPointsEachFileToTheTarOfItsProvince() {
        val downloadable = DownloadablePackage.of(municipalityList(), provinceEntry(), packageDetails())

        assertEquals(installedPackageInfo(), downloadable.info)
        assertEquals(
            listOf("https://example.org/kit-apagon-datos/2026-12-01-ES-20/ES-20.tar"),
            downloadable.files.flatMap { it.urls }.distinct(),
        )
        assertEquals(3_501_019L + 54_792L, downloadable.totalBytes)
    }
}
