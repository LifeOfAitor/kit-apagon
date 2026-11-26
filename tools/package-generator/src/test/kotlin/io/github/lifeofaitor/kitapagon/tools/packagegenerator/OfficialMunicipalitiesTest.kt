package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfficialMunicipalitiesTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun readsTheMunicipalitiesOfEverySheetAndSkipsTitlesAndHeaders() {
        val municipalities = OfficialSpreadsheetReader.read(gipuzkoaAndBizkaia())

        assertEquals(
            listOf(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("48916", "Usansolo")),
            municipalities,
        )
    }

    // The layout of 26codmun.xlsx: a title, the province, a header, then one row per municipality.
    private fun gipuzkoaAndBizkaia(): File {
        val shared = listOf("Relación de municipios", "Gipuzkoa", "CPRO", "CMUN", "DC", "NOMBRE", "20", "045", "Irun")
        val gipuzkoa =
            sheetOf(
                row(1, "A" to shared(0)),
                row(2, "A" to shared(1)),
                row(3, "A" to shared(2), "B" to shared(3), "C" to shared(4), "D" to shared(5)),
                row(4, "A" to shared(6), "B" to shared(7), "C" to inline("8"), "D" to shared(8)),
            )
        val bizkaia =
            sheetOf(
                row(
                    4,
                    "A" to inline("48"),
                    "B" to inline("916"),
                    "C" to inline("2"),
                    "D" to inline("Usansolo"),
                ),
            )
        return spreadsheetOf(shared, gipuzkoa, bizkaia)
    }

    @Test
    fun refusesAFileWithoutMunicipalities() {
        val empty = spreadsheetOf(emptyList(), sheetOf(row(1, "A" to inline("Otra cosa"))))

        assertFailsWith<IllegalArgumentException> { OfficialSpreadsheetReader.read(empty) }
    }

    @Test
    fun theFileInTheRepositoryKeepsNamesWithCommasAndQuotes() {
        val municipalities =
            listOf(
                OfficialMunicipality("44129", "Iglesuela del Cid, La"),
                OfficialMunicipality("20045", "Irun \"Irún\""),
            )

        val text = OfficialMunicipalitiesFile.write(municipalities)

        assertEquals("codigo_ine,nombre\n44129,\"Iglesuela del Cid, La\"\n20045,\"Irun \"\"Irún\"\"\"\n", text)
        assertEquals(municipalities, OfficialMunicipalitiesFile.read(text))
        assertFailsWith<IllegalArgumentException> {
            OfficialMunicipalitiesFile.read("codigo_ine,nombre\n2004,\"Short code\"\n")
        }
    }

    @Test
    fun listsTheMunicipalitiesWithoutBoundaryAndTheUnexpectedOnes() {
        val official = listOf(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("20036", "Hondarribia"))
        val inOpenStreetMap = listOf(municipality(), municipality("ES-20999", "Nuevo"))

        val check = MunicipalityCheck.of(inOpenStreetMap, official, provinces = null)

        assertEquals(listOf(OfficialMunicipality("20036", "Hondarribia")), check.missing)
        assertEquals(listOf("ES-20999"), check.unexpected.map { it.id.value })
        assertEquals(
            """
            Municipios de la relación oficial del INE: 2
            Sin límite en OpenStreetMap, y por tanto sin paquete: 1
            - 20036 Hondarribia
            Con un código que no está en la relación oficial: 1
            - 20999 Nuevo
            Relación oficial: elaboración propia con datos extraídos del sitio web del INE: www.ine.es
            """.trimIndent(),
            check.report(),
        )
    }

    @Test
    fun aTrialRunComparesOnlyItsProvinces() {
        val official = listOf(OfficialMunicipality("20045", "Irun"), OfficialMunicipality("48020", "Bilbao"))

        val check = MunicipalityCheck.of(listOf(municipality()), official, provinces = setOf("20"))

        assertEquals(emptyList(), check.missing)
        assertEquals(1, check.expectedCount)
    }

    @Test
    fun doesNotPublishWhenMoreThanOnePercentIsMissing() {
        val official = (1..200).map { OfficialMunicipality("20${it.toString().padStart(3, '0')}", "Municipio $it") }

        fun withBoundaries(count: Int) = (1..count).map { municipality("ES-20${it.toString().padStart(3, '0')}") }

        assertTrue(MunicipalityCheck.of(withBoundaries(198), official, null).isPublishable)
        assertFalse(MunicipalityCheck.of(withBoundaries(197), official, null).isPublishable)
    }

    private fun shared(index: Int) = Cell.Shared(index)

    private fun inline(text: String) = Cell.Inline(text)

    private sealed interface Cell {
        data class Shared(
            val index: Int,
        ) : Cell

        data class Inline(
            val text: String,
        ) : Cell
    }

    private fun row(
        number: Int,
        vararg cells: Pair<String, Cell>,
    ): String =
        "<row r=\"$number\">" +
            cells.joinToString("") { (column, cell) ->
                when (cell) {
                    is Cell.Shared -> "<c r=\"$column$number\" t=\"s\"><v>${cell.index}</v></c>"
                    is Cell.Inline -> "<c r=\"$column$number\" t=\"inlineStr\"><is><t>${cell.text}</t></is></c>"
                }
            } + "</row>"

    private fun sheetOf(vararg rows: String): String =
        "<worksheet xmlns=\"$SPREADSHEET\"><sheetData>${rows.joinToString("")}</sheetData></worksheet>"

    private fun spreadsheetOf(
        sharedStrings: List<String>,
        vararg sheets: String,
    ): File {
        val file = File(temporaryDirectory, "codmun.xlsx")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(
                (
                    "<sst xmlns=\"$SPREADSHEET\">" + sharedStrings.joinToString("") { "<si><t>$it</t></si>" } +
                        "</sst>"
                ).toByteArray(),
            )
            sheets.forEachIndexed { position, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${position + 1}.xml"))
                zip.write(sheet.toByteArray())
            }
        }
        return file
    }

    private companion object {
        const val SPREADSHEET = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    }
}
