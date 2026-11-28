package io.github.lifeofaitor.kitapagon.core.packageformat

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Both files of the index are downloaded from the internet, so they are fed thousands of generated inputs. */
class PublishedIndexPropertyTest {
    @Test
    fun neverThrowsWhateverTheText() =
        runTest {
            checkAll(ITERATIONS, Arb.string(maxSize = 300)) { text ->
                MunicipalityListParser.parse(text)
                ProvinceDetailsParser.parse(text)
            }
        }

    @Test
    fun neverThrowsForFilesWithArbitraryEntries() =
        runTest {
            checkAll(ITERATIONS, Arb.string(maxSize = 200)) { entry ->
                MunicipalityListParser.parse(
                    MunicipalityListWriter
                        .write(
                            municipalityList(),
                        ).replace("\"municipalities\":[", "\"municipalities\":[$entry"),
                )
                ProvinceDetailsParser.parse(
                    ProvinceDetailsWriter.write(provinceDetails()).replace("\"packages\":[", "\"packages\":[$entry"),
                )
            }
        }

    @Test
    fun anyValidListSurvivesBeingWrittenAndReadBack() =
        runTest {
            checkAll(ITERATIONS, Arb.list(validMunicipality(), 0..10)) { municipalities ->
                val list = municipalityList(municipalities = municipalities)

                assertEquals(
                    MunicipalityListResult.Valid(list, skippedEntryCount = 0),
                    MunicipalityListParser.parse(MunicipalityListWriter.write(list)),
                )
            }
        }

    @Test
    fun anyValidProvinceSurvivesBeingWrittenAndReadBack() =
        runTest {
            checkAll(ITERATIONS, Arb.list(validPackage(), 1..10)) { packages ->
                val details = provinceDetails("20", "Gipuzkoa", *packages.toTypedArray())

                assertEquals(
                    ProvinceDetailsResult.Valid(details, skippedPackageCount = 0),
                    ProvinceDetailsParser.parse(ProvinceDetailsWriter.write(details)),
                )
            }
        }

    private fun codes() = Arb.int(1_000..99_999).map { it.toString().padStart(5, '0') }

    private fun names() = Arb.string(1..40).map { it.ifBlank { "Irun" } }

    private fun validMunicipality() =
        arbitrary {
            municipalityEntry("ES-${codes().bind()}", names().bind(), Arb.list(Arb.string(0..20), 0..3).bind())
        }

    private fun validPackage() =
        arbitrary {
            packageDetails("ES-${codes().bind()}", names().bind(), Arb.list(Arb.string(0..20), 0..3).bind())
        }

    private companion object {
        const val ITERATIONS = 1_000
    }
}
