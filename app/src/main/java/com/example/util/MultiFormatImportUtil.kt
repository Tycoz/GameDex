package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import com.example.data.model.GameItem
import com.example.data.network.NetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

/**
 * Universal multi-format collection importer.
 * Supports:
 * - Google Sheets: Direct import via shared URL (CSV export endpoint) or downloaded files.
 * - OpenDocument: .ODS (Spreadsheets) and .ODT (Document tables).
 * - Microsoft Excel: .XLSX (Modern OpenXML) and .XLS (XML Spreadsheet 2003 or TSV/CSV).
 * - VCS / ICS / VCF: vCalendar 1.0/2.0, iCalendar events, and vCards.
 * - CSV / TSV / Plain text.
 */
object MultiFormatImportUtil {

    private const val TAG = "MultiFormatImport"

    private val httpClient = NetworkClient.baseClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val SHEET_ID_PATTERN = Pattern.compile("docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9_-]+)")
    private val GID_PATTERN = Pattern.compile("[#&?]gid=([0-9]+)")
    private val VCS_PUBLISHER_REGEX = "(?:Editeur|Publisher)\\s*[:=]\\s*([^;,\n]+)".toRegex(RegexOption.IGNORE_CASE)
    private val VCS_GENRE_REGEX = "Genre\\s*[:=]\\s*([^;,\n]+)".toRegex(RegexOption.IGNORE_CASE)
    private val VCS_PRICE_REGEX = "(?:Prix|Price)\\s*[:=]\\s*([0-9.,]+)".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Parses an imported collection file from a content URI, automatically detecting format.
     */
    fun parseFromUri(context: Context, uri: Uri): CsvParseResult {
        val fileName = getFileName(context, uri) ?: "import_collection"
        val lowerName = fileName.lowercase()

        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Impossible de lire le fichier."))

            when {
                lowerName.endsWith(".ods") || lowerName.endsWith(".odt") -> {
                    parseOpenDocument(ByteArrayInputStream(bytes), fileName)
                }
                lowerName.endsWith(".xlsx") -> {
                    parseExcelXlsx(ByteArrayInputStream(bytes), fileName)
                }
                lowerName.endsWith(".xls") -> {
                    parseExcelXls(bytes, fileName)
                }
                lowerName.endsWith(".vcs") || lowerName.endsWith(".ics") || lowerName.endsWith(".vcf") -> {
                    val text = String(bytes, Charsets.UTF_8)
                    parseVcs(text, fileName)
                }
                else -> {
                    // Check if bytes start with PK (Zip archive - ODS/ODT/XLSX)
                    if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                        try {
                            parseExcelXlsx(ByteArrayInputStream(bytes), fileName)
                        } catch (_: Exception) {
                            try {
                                parseOpenDocument(ByteArrayInputStream(bytes), fileName)
                            } catch (e: Exception) {
                                CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Format d'archive non reconnu : ${e.message}"))
                            }
                        }
                    } else {
                        val text = String(bytes, Charsets.UTF_8)
                        if (text.contains("BEGIN:VEVENT", ignoreCase = true) || text.contains("BEGIN:VCARD", ignoreCase = true)) {
                            parseVcs(text, fileName)
                        } else if (text.contains("<Workbook", ignoreCase = true)) {
                            parseXmlSpreadsheet2003(text, fileName)
                        } else {
                            CsvImportUtil.parseCsvString(text, fileName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing file $fileName", e)
            CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf(e.localizedMessage ?: "Erreur d'importation"))
        }
    }

    /**
     * Creates an XmlPullParser supporting both Android runtime and JVM test environments.
     */
    fun createXmlPullParser(): XmlPullParser {
        return try {
            org.xmlpull.v1.XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }.newPullParser()
        } catch (_: Throwable) {
            Xml.newPullParser()
        }
    }

    /**
     * Extracts the direct CSV export URL from a public/shared Google Sheet link.
     */
    fun extractGoogleSheetExportUrl(sheetUrl: String): Result<String> {
        val trimmedUrl = sheetUrl.trim()
        val matcher = SHEET_ID_PATTERN.matcher(trimmedUrl)

        if (!matcher.find()) {
            return Result.failure(
                IllegalArgumentException("Lien Google Sheet invalide. Le lien doit être de la forme https://docs.google.com/spreadsheets/d/{ID}...")
            )
        }

        val docId = matcher.group(1) ?: return Result.failure(
            IllegalArgumentException("Identifiant de feuille Google Sheet introuvable.")
        )

        // Extract optional gid
        val gidMatcher = GID_PATTERN.matcher(trimmedUrl)
        val gidParam = if (gidMatcher.find()) "&gid=${gidMatcher.group(1)}" else ""

        val exportUrl = "https://docs.google.com/spreadsheets/d/$docId/export?format=csv$gidParam"
        return Result.success(exportUrl)
    }

    /**
     * Downloads and parses a shared Google Sheet via its public or shared URL.
     */
    suspend fun fetchAndParseGoogleSheet(sheetUrl: String): Result<CsvParseResult> = withContext(Dispatchers.IO) {
        try {
            val exportUrl = extractGoogleSheetExportUrl(sheetUrl).getOrElse { error ->
                return@withContext Result.failure(error)
            }

            val request = Request.Builder()
                .url(exportUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            IllegalStateException("Accès refusé au document Google Sheet. Assurez-vous que le document est partagé en 'Tous les utilisateurs disposant du lien : Lecteur'.")
                        )
                    }
                    return@withContext Result.failure(
                        IllegalStateException("Impossible de récupérer la feuille Google Sheet (Erreur HTTP ${response.code}).")
                    )
                }

                val contentType = response.header("Content-Type") ?: ""
                val csvContent = response.body?.string() ?: ""

                // If Google redirected to an HTML login page
                if (contentType.contains("text/html", ignoreCase = true) && csvContent.contains("<html", ignoreCase = true)) {
                    return@withContext Result.failure(
                        IllegalStateException("La feuille Google Sheet nécessite une connexion. Rendez-la accessible avec le lien (Fichier > Partager > Accès général : Tout utilisateur disposant du lien).")
                    )
                }

                val parsed = CsvImportUtil.parseCsvString(csvContent, "Google_Sheet.csv")
                Result.success(parsed)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import Google Sheet", e)
            Result.failure(e)
        }
    }

    /**
     * Parses OpenDocument Spreadsheets (.ods) and OpenDocument Text (.odt) tables.
     */
    fun parseOpenDocument(inputStream: InputStream, fileName: String): CsvParseResult {
        var contentXml: String? = null
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "content.xml") {
                    contentXml = zip.bufferedReader(Charsets.UTF_8).readText()
                    break
                }
                entry = zip.nextEntry
            }
        }

        val xml = contentXml
            ?: return CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Structure OpenDocument invalide (content.xml manquant)."))

        val records = extractRowsFromOpenDocumentXml(xml)
        return CsvImportUtil.parseRecordsToResult(records, fileName, ',')
    }

    fun extractRowsFromOpenDocumentXml(xml: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val parser = createXmlPullParser().apply {
            setInput(StringReader(xml))
        }

        var eventType = parser.eventType
        var currentRow: MutableList<String>? = null
        val currentCellText = StringBuilder()
        var inCell = false
        var repeatCount = 1

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val localName = parser.name.substringAfter(":")
                    if (localName == "table-row") {
                        currentRow = mutableListOf()
                    } else if (localName == "table-cell") {
                        inCell = true
                        currentCellText.setLength(0)
                        val repeated = parser.getAttributeValue(null, "number-columns-repeated")
                            ?: parser.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "number-columns-repeated")
                        repeatCount = (repeated?.toIntOrNull() ?: 1).coerceAtMost(30)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inCell) {
                        currentCellText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val localName = parser.name.substringAfter(":")
                    if (localName == "table-cell") {
                        inCell = false
                        val text = currentCellText.toString().trim()
                        if (currentRow != null) {
                            if (text.isNotEmpty()) {
                                repeat(repeatCount) { currentRow.add(text) }
                            } else {
                                currentRow.add("")
                            }
                        }
                        repeatCount = 1
                    } else if (localName == "table-row") {
                        if (currentRow != null) {
                            // Trim trailing blanks
                            while (currentRow.isNotEmpty() && currentRow.last().isBlank()) {
                                currentRow.removeAt(currentRow.size - 1)
                            }
                            if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                records.add(currentRow)
                            }
                        }
                        currentRow = null
                    }
                }
            }
            eventType = parser.next()
        }

        return records
    }

    /**
     * Parses modern Excel OpenXML (.xlsx) files.
     */
    fun parseExcelXlsx(inputStream: InputStream, fileName: String): CsvParseResult {
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xl/sharedStrings.xml" || name.startsWith("xl/worksheets/sheet1.xml") || name == "xl/worksheets/sheet.xml") {
                    files[name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }

        // 1. Parse shared strings table
        val sharedStrings = mutableListOf<String>()
        files["xl/sharedStrings.xml"]?.let { bytes ->
            val parser = createXmlPullParser().apply {
                setInput(InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8))
            }
            var eventType = parser.eventType
            var inT = false
            val currentStr = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") inT = true
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) currentStr.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") inT = false
                        else if (parser.name == "si") {
                            sharedStrings.add(currentStr.toString())
                            currentStr.setLength(0)
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        // 2. Parse sheet1.xml
        val sheetBytes = files["xl/worksheets/sheet1.xml"]
            ?: files.entries.firstOrNull { it.key.contains("sheet") }?.value
            ?: return CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Feuille Excel introuvable dans le classeur."))

        val records = mutableListOf<List<String>>()
        val parser = createXmlPullParser().apply {
            setInput(InputStreamReader(ByteArrayInputStream(sheetBytes), Charsets.UTF_8))
        }

        var eventType = parser.eventType
        var currentRow: MutableList<String>? = null
        var currentCellType: String? = null
        val currentCellValue = StringBuilder()
        var inV = false
        var inT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t")
                            currentCellValue.setLength(0)
                        }
                        "v" -> inV = true
                        "t" -> inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || inT) {
                        currentCellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inV = false
                        "t" -> inT = false
                        "c" -> {
                            val raw = currentCellValue.toString().trim()
                            val cellText = if (currentCellType == "s") {
                                val idx = raw.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                            } else {
                                raw
                            }
                            currentRow?.add(cellText)
                        }
                        "row" -> {
                            if (currentRow != null && currentRow.any { it.isNotBlank() }) {
                                records.add(currentRow)
                            }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return CsvImportUtil.parseRecordsToResult(records, fileName, ',')
    }

    /**
     * Parses classic .xls files (either XML Spreadsheet 2003, XLSX renamed to .xls, or text/CSV).
     */
    private fun parseExcelXls(bytes: ByteArray, fileName: String): CsvParseResult {
        // Case A: XLSX renamed to .xls (starts with PK)
        if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            return parseExcelXlsx(ByteArrayInputStream(bytes), fileName)
        }

        val text = String(bytes, Charsets.UTF_8)
        // Case B: XML Spreadsheet 2003 (<Workbook...)
        if (text.contains("<Workbook", ignoreCase = true) || text.contains("urn:schemas-microsoft-com:office:spreadsheet")) {
            return parseXmlSpreadsheet2003(text, fileName)
        }

        // Case C: CSV/TSV exported as .xls
        return CsvImportUtil.parseCsvString(text, fileName)
    }

    /**
     * Parses XML Spreadsheet 2003 format (common for web application Excel exports).
     */
    fun parseXmlSpreadsheet2003(xml: String, fileName: String): CsvParseResult {
        val records = mutableListOf<List<String>>()
        val parser = createXmlPullParser().apply {
            setInput(StringReader(xml))
        }

        var eventType = parser.eventType
        var currentRow: MutableList<String>? = null
        val currentDataText = StringBuilder()
        var inData = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfter(":")
                    when (tag) {
                        "Row" -> currentRow = mutableListOf()
                        "Data" -> {
                            inData = true
                            currentDataText.setLength(0)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inData) {
                        currentDataText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfter(":")
                    when (tag) {
                        "Data" -> {
                            inData = false
                            currentRow?.add(currentDataText.toString().trim())
                        }
                        "Row" -> {
                            if (currentRow != null && currentRow.any { it.isNotBlank() }) {
                                records.add(currentRow)
                            }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return CsvImportUtil.parseRecordsToResult(records, fileName, ',')
    }

    /**
     * Parses vCalendar / iCalendar (.vcs, .ics) and vCard (.vcf) files into GameItem entities.
     */
    fun parseVcs(content: String, fileName: String): CsvParseResult {
        // If content does not contain calendar or vcard tags, fallback to CSV parser
        if (!content.contains("BEGIN:", ignoreCase = true)) {
            return CsvImportUtil.parseCsvString(content, fileName)
        }

        val lines = content.lines()
        val games = mutableListOf<GameItem>()
        var inEvent = false
        var currentTitle = ""
        var currentPlatform = "Autre"
        var currentGenre = ""
        var currentYear = 2000
        var currentPublisher = ""
        var currentNotes = ""
        var currentPrice = 0.0
        var currentCover = ""

        fun commitCurrentGame() {
            if (currentTitle.isNotBlank()) {
                games.add(
                    GameItem(
                        title = currentTitle,
                        platform = currentPlatform,
                        genre = currentGenre,
                        releaseYear = currentYear,
                        publisher = currentPublisher,
                        purchasePrice = currentPrice,
                        estimatedValue = currentPrice,
                        notes = currentNotes,
                        coverUrl = currentCover
                    )
                )
            }
            currentTitle = ""
            currentPlatform = "Autre"
            currentGenre = ""
            currentYear = 2000
            currentPublisher = ""
            currentNotes = ""
            currentPrice = 0.0
            currentCover = ""
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("BEGIN:VEVENT", ignoreCase = true) ||
                trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) ||
                trimmed.startsWith("BEGIN:VTODO", ignoreCase = true)
            ) {
                inEvent = true
                commitCurrentGame()
            } else if (trimmed.startsWith("END:VEVENT", ignoreCase = true) ||
                trimmed.startsWith("END:VCARD", ignoreCase = true) ||
                trimmed.startsWith("END:VTODO", ignoreCase = true)
            ) {
                commitCurrentGame()
                inEvent = false
            } else if (inEvent) {
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx > 0) {
                    val key = trimmed.substring(0, colonIdx).uppercase().substringBefore(';')
                    val value = trimmed.substring(colonIdx + 1).trim()

                    when (key) {
                        "SUMMARY", "FN" -> currentTitle = value
                        "LOCATION" -> currentPlatform = value
                        "CATEGORIES" -> {
                            val parts = value.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
                            if (parts.isNotEmpty()) {
                                if (currentPlatform == "Autre" || currentPlatform.isBlank()) {
                                    currentPlatform = parts[0]
                                    if (parts.size > 1 && currentGenre.isBlank()) currentGenre = parts[1]
                                } else {
                                    if (currentGenre.isBlank()) currentGenre = parts[0]
                                }
                            }
                        }
                        "DESCRIPTION", "NOTE" -> {
                            currentNotes = value
                            // Extract details if formatted as key-value
                            if (value.contains("Editeur", ignoreCase = true) || value.contains("Publisher", ignoreCase = true)) {
                                val match = VCS_PUBLISHER_REGEX.find(value)
                                match?.groupValues?.getOrNull(1)?.trim()?.let { currentPublisher = it }
                            }
                            if (value.contains("Genre", ignoreCase = true)) {
                                val match = VCS_GENRE_REGEX.find(value)
                                match?.groupValues?.getOrNull(1)?.trim()?.let { currentGenre = it }
                            }
                            if (value.contains("Prix", ignoreCase = true) || value.contains("Price", ignoreCase = true)) {
                                val match = VCS_PRICE_REGEX.find(value)
                                match?.groupValues?.getOrNull(1)?.replace(",", ".")?.toDoubleOrNull()?.let { currentPrice = it }
                            }
                        }
                        "DTSTART" -> {
                            val digits = value.filter { it.isDigit() }
                            if (digits.length >= 4) {
                                digits.substring(0, 4).toIntOrNull()?.let {
                                    if (it in 1970..2035) currentYear = it
                                }
                            }
                        }
                        "URL" -> currentCover = value
                    }
                }
            }
        }
        commitCurrentGame()

        return if (games.isNotEmpty()) {
            CsvParseResult(
                games = games,
                totalRows = games.size,
                skippedRows = 0,
                detectedDelimiter = ',',
                fileName = fileName
            )
        } else {
            CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Aucun événement ou fiche de jeu trouvé dans le fichier VCS/ICS."))
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            name = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to query display name for $uri", e)
            }
        }
        return name ?: uri.lastPathSegment
    }
}
