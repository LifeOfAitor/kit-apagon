package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertEquals

class ProvinceDetailsFileTest {
    @Test
    fun theDetailsReadBackExactlyAsTheyWereWritten() {
        val details =
            provinceDetails(
                "20",
                "Gipuzkoa",
                packageDetails(),
                packageDetails("ES-20036", "Hondarribia", listOf("Fuenterrabía")),
            )

        assertEquals(
            ProvinceDetailsResult.Valid(details, skippedPackageCount = 0),
            ProvinceDetailsParser.parse(ProvinceDetailsWriter.write(details)),
        )
    }

    @Test
    fun rejectsTextThatIsNotJsonNotDetailsOrFromANewerGenerator() {
        assertEquals(ProvinceDetailsResult.Invalid(InvalidIndexReason.NOT_JSON), ProvinceDetailsParser.parse("{"))
        assertEquals(
            ProvinceDetailsResult.Invalid(InvalidIndexReason.NOT_AN_INDEX),
            ProvinceDetailsParser.parse("""{"format_version":2}"""),
        )
        val newer =
            ProvinceDetailsWriter
                .write(
                    provinceDetails(),
                ).replace("\"format_version\":2", "\"format_version\":3")
        assertEquals(
            ProvinceDetailsResult.Invalid(InvalidIndexReason.UNSUPPORTED_FORMAT_VERSION),
            ProvinceDetailsParser.parse(newer),
        )
    }

    @Test
    fun skipsAPackageWithABrokenFileAndKeepsTheOthers() {
        val broken =
            packageDetails("ES-20036").let {
                it.copy(
                    files =
                        it.files.map { file ->
                            file.copy(sizeBytes = 0)
                        },
                )
            }

        val valid =
            ProvinceDetailsParser.parse(
                ProvinceDetailsWriter.write(provinceDetails("20", "Gipuzkoa", packageDetails(), broken)),
            ) as ProvinceDetailsResult.Valid

        assertEquals(listOf("ES-20045"), valid.details.packages.map { it.id.value })
        assertEquals(1, valid.skippedPackageCount)
    }

    @Test
    fun aPackageNeedsExactlyOneMapAndNoRepeatedKind() {
        val withoutMap =
            packageDetails().let {
                it.copy(
                    files =
                        it.files.filter { file ->
                            file.kind ==
                                PackageFileKind.PLACES
                        },
                )
            }
        val twoMaps = packageDetails("ES-20036").let { it.copy(files = it.files + it.files.first()) }
        val onlyMap =
            packageDetails("ES-20016").let {
                it.copy(
                    files =
                        it.files.filter { file ->
                            file.kind ==
                                PackageFileKind.MAP
                        },
                )
            }

        val valid =
            ProvinceDetailsParser.parse(
                ProvinceDetailsWriter.write(provinceDetails("20", "Gipuzkoa", withoutMap, twoMaps, onlyMap)),
            ) as ProvinceDetailsResult.Valid

        assertEquals(listOf("ES-20016"), valid.details.packages.map { it.id.value })
        assertEquals(2, valid.skippedPackageCount)
    }

    @Test
    fun skipsAPackageWithAnUnknownKindOfFileOrAnUnsafeTarName() {
        val written =
            ProvinceDetailsWriter.write(
                provinceDetails("20", "Gipuzkoa", packageDetails(), packageDetails("ES-20036")),
            )
        val unknownKind = written.replaceFirst("\"kind\":\"places\"", "\"kind\":\"videos\"")
        val unsafeBundle = written.replace("\"bundle\":\"ES-20.tar\"", "\"bundle\":\"../ES-20.tar\"")

        assertEquals(1, (ProvinceDetailsParser.parse(unknownKind) as ProvinceDetailsResult.Valid).skippedPackageCount)
        assertEquals(2, (ProvinceDetailsParser.parse(unsafeBundle) as ProvinceDetailsResult.Valid).skippedPackageCount)
    }

    @Test
    fun skipsAPackageWithAnAreaOutsideTheWorld() {
        val outside = packageDetails().copy(bounds = GeographicBounds(0.0, 0.0, 200.0, 1.0))

        assertEquals(
            1,
            (
                ProvinceDetailsParser.parse(
                    ProvinceDetailsWriter.write(provinceDetails("20", "Gipuzkoa", outside)),
                ) as ProvinceDetailsResult.Valid
            ).skippedPackageCount,
        )
    }
}
