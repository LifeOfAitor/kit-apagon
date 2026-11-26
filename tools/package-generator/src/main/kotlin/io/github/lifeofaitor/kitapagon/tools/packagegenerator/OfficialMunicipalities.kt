package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/** A municipality of the official list of the INE (decision 0014). */
data class OfficialMunicipality(
    /** The five digit INE code: two of the province and three of the municipality. */
    val code: String,
    val name: String,
)

/**
 * Reads the yearly list of the INE, such as 26codmun.xlsx ("Municipios por provincia con sus
 * códigos"): one sheet per province with the columns CPRO, CMUN, DC and NOMBRE. Only the rows whose
 * first two cells are a province and a municipality code are kept, which skips titles and headers.
 */
object OfficialSpreadsheetReader {
    private val PROVINCE_CODE = Regex("^[0-9]{2}$")
    private val MUNICIPALITY_CODE = Regex("^[0-9]{3}$")
    private const val SHARED_STRINGS = "xl/sharedStrings.xml"
    private val WORKSHEET = Regex("^xl/worksheets/sheet[0-9]+\\.xml$")

    /** Throws IllegalArgumentException when the file has no municipality at all. */
    fun read(spreadsheet: File): List<OfficialMunicipality> =
        ZipFile(spreadsheet).use { zip ->
            val sharedStrings =
                zip
                    .getEntry(SHARED_STRINGS)
                    ?.let { entry ->
                        zip.getInputStream(entry).use(::sharedStringsOf)
                    }.orEmpty()
            val municipalities =
                zip
                    .entries()
                    .asSequence()
                    .filter { entry -> WORKSHEET.matches(entry.name) }
                    .flatMap { entry -> zip.getInputStream(entry).use { rowsOf(it, sharedStrings) } }
                    .mapNotNull(::municipalityOrNull)
                    .sortedBy { municipality -> municipality.code }
                    .toList()
            require(
                municipalities.isNotEmpty(),
            ) { "$spreadsheet does not look like the list of municipalities of the INE." }
            municipalities
        }

    private fun municipalityOrNull(row: Map<String, String>): OfficialMunicipality? {
        val province = row["A"]?.trim()?.takeIf(PROVINCE_CODE::matches)
        val municipality = row["B"]?.trim()?.takeIf(MUNICIPALITY_CODE::matches)
        val name = row["D"]?.trim()?.takeIf { it.isNotEmpty() }
        return if (province != null && municipality != null &&
            name != null
        ) {
            OfficialMunicipality(province + municipality, name)
        } else {
            null
        }
    }

    private fun sharedStringsOf(input: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val reader = xmlReaderOf(input)
        val current = StringBuilder()
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> if (reader.localName == "si") current.clear()
                XMLStreamConstants.CHARACTERS -> current.append(reader.text)
                XMLStreamConstants.END_ELEMENT -> if (reader.localName == "si") strings += current.toString()
            }
        }
        return strings
    }

    // Each row becomes a map from column letter to text, whatever the way the cell stores it.
    private fun rowsOf(
        input: InputStream,
        sharedStrings: List<String>,
    ): List<Map<String, String>> {
        val reader = xmlReaderOf(input)
        val collector = RowCollector(sharedStrings)
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> collector.start(reader)
                XMLStreamConstants.CHARACTERS -> collector.characters(reader.text)
                XMLStreamConstants.END_ELEMENT -> collector.end(reader.localName)
            }
        }
        return collector.rows
    }

    private class RowCollector(
        private val sharedStrings: List<String>,
    ) {
        val rows = mutableListOf<Map<String, String>>()
        private var row = mutableMapOf<String, String>()
        private var cell: SpreadsheetCell? = null
        private val text = StringBuilder()

        fun start(reader: XMLStreamReader) {
            when (reader.localName) {
                "row" -> row = mutableMapOf()
                "c" -> cell = SpreadsheetCell.from(reader)
                "v", "t" -> text.clear()
            }
        }

        fun characters(characters: String) {
            text.append(characters)
        }

        fun end(elementName: String) {
            when (elementName) {
                "v", "t" -> cell?.let { row[it.column] = it.valueOf(text.toString(), sharedStrings) }
                "row" -> rows += row
            }
        }
    }

    private class SpreadsheetCell(
        val column: String,
        private val type: String?,
    ) {
        fun valueOf(
            text: String,
            sharedStrings: List<String>,
        ): String =
            if (type == "s") {
                text
                    .trim()
                    .toIntOrNull()
                    ?.let(sharedStrings::getOrNull)
                    .orEmpty()
            } else {
                text
            }

        companion object {
            fun from(reader: XMLStreamReader): SpreadsheetCell =
                SpreadsheetCell(
                    column = reader.getAttributeValue(null, "r").orEmpty().takeWhile(Char::isLetter),
                    type = reader.getAttributeValue(null, "t"),
                )
        }
    }

    private fun xmlReaderOf(input: InputStream): XMLStreamReader =
        XMLInputFactory
            .newInstance()
            .apply {
                // The spreadsheet is downloaded from the internet: no external entities or DTDs.
                setProperty(XMLInputFactory.SUPPORT_DTD, false)
                setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
                setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)
            }.createXMLStreamReader(input)
}

/** The official list kept in the repository as `code,"name"` lines, one municipality per line. */
object OfficialMunicipalitiesFile {
    private const val HEADER = "codigo_ine,nombre"
    private val LINE = Regex("^([0-9]{5}),\"(.*)\"$")

    fun write(municipalities: List<OfficialMunicipality>): String =
        (
            listOf(HEADER) +
                municipalities.map {
                    "${it.code},\"${it.name.replace("\"", "\"\"")}\""
                }
        ).joinToString("\n", postfix = "\n")

    /** Throws IllegalArgumentException for any line that is not a municipality. */
    fun read(text: String): List<OfficialMunicipality> =
        text
            .lineSequence()
            .drop(1)
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val match = requireNotNull(LINE.matchEntire(line)) { "Not a line of the official list: $line" }
                OfficialMunicipality(match.groupValues[1], match.groupValues[2].replace("\"\"", "\""))
            }.toList()
}

/** What OpenStreetMap has compared with the official list (decision 0014). */
data class MunicipalityCheck(
    /** In the official list but without a boundary in OpenStreetMap: these places get no package. */
    val missing: List<OfficialMunicipality>,
    /** In OpenStreetMap with a code the official list does not have, such as a newly created municipality. */
    val unexpected: List<Municipality>,
    val expectedCount: Int,
) {
    /** False when so many are missing that the extract or the boundaries are probably broken. */
    val isPublishable: Boolean get() = missing.size <= expectedCount * MAXIMUM_MISSING_FRACTION

    /** The report published with each version, in Spanish like the rest of what people read. */
    fun report(): String =
        buildList {
            add("Municipios de la relación oficial del INE: $expectedCount")
            add("Sin límite en OpenStreetMap, y por tanto sin paquete: ${missing.size}")
            missing.forEach { add("- ${it.code} ${it.name}") }
            add("Con un código que no está en la relación oficial: ${unexpected.size}")
            unexpected.forEach { add("- ${it.id.value.removePrefix("ES-")} ${it.names.official}") }
            add("Relación oficial: elaboración propia con datos extraídos del sitio web del INE: www.ine.es")
        }.joinToString("\n")

    companion object {
        private const val MAXIMUM_MISSING_FRACTION = 0.01

        /** Compares only the provinces of [municipalities] when [provinces] is given, as in a trial run. */
        fun of(
            municipalities: List<Municipality>,
            official: List<OfficialMunicipality>,
            provinces: Set<String>?,
        ): MunicipalityCheck {
            val expected = official.filter { provinces == null || it.code.take(2) in provinces }
            val osmCodes = municipalities.map { it.id.value.removePrefix("ES-") }.toSet()
            val officialCodes = expected.map { it.code }.toSet()
            return MunicipalityCheck(
                missing = expected.filter { it.code !in osmCodes },
                unexpected = municipalities.filter { it.id.value.removePrefix("ES-") !in officialCodes },
                expectedCount = expected.size,
            )
        }
    }
}
