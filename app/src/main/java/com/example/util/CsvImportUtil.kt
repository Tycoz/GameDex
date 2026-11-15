package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.GameItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale

data class CsvParseResult(
    val games: List<GameItem>,
    val totalRows: Int,
    val skippedRows: Int,
    val detectedDelimiter: Char,
    val fileName: String = "",
    val errorMessages: List<String> = emptyList()
)

object CsvImportUtil {

    private const val TAG = "CsvImportUtil"

    private val COMBINING_MARKS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val NON_ALNUM_REGEX = "[^a-z0-9]".toRegex()
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    private val TITLE_HEADER_REGEX = "^(titre|title|nom|name|game|jeu|game title|game name|nom du jeu|nom de jeu)$".toRegex()
    private val PLATFORM_HEADER_REGEX = "^(plateforme|platform|console|system|systeme|machine|support de jeu|support)$".toRegex()
    private val GENRE_HEADER_REGEX = "^(genre|categorie|category|type de jeu)$".toRegex()
    private val YEAR_HEADER_REGEX = "^(annee|annee de sortie|year|release year|date de sortie)$".toRegex()
    private val PUBLISHER_HEADER_REGEX = "^(editeur|publisher|developpeur|developer|studio)$".toRegex()
    private val FORMAT_HEADER_REGEX = "^(format|type de support|media format)$".toRegex()
    private val REGION_HEADER_REGEX = "^(region|zone|pays|country)$".toRegex()
    private val COMPLETENESS_HEADER_REGEX = "^(completude|completeness|packaging|cib|contenu|pack)$".toRegex()
    private val GENERAL_CONDITION_HEADER_REGEX = "^(etat|condition|general condition|etat general|condition globale)$".toRegex()
    private val STATUS_HEADER_REGEX = "^(statut|status|progression|etat de jeu)$".toRegex()
    private val NOTES_HEADER_REGEX = "^(notes|comments|remarques|details|memo)$".toRegex()
    private val RATING_HEADER_REGEX = "^(note|rating|score|evaluation|eval)$".toRegex()
    private val PRICE_HEADER_REGEX = "^(prix|price|cout|cost|achat)$".toRegex()

    /**
     * Reads and parses a CSV file from an Android content Uri.
     */
    fun parseCsvFromUri(context: Context, uri: Uri): CsvParseResult {
        return try {
            val fileName = getFileName(context, uri) ?: "collection_import.csv"
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    parseCsvReader(reader, fileName)
                }
            } ?: CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Impossible d'ouvrir le fichier."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse CSV URI", e)
            CsvParseResult(emptyList(), 0, 0, ',', "Erreur", listOf(e.localizedMessage ?: "Erreur de lecture"))
        }
    }

    /**
     * Parses CSV text provided as a string.
     */
    fun parseCsvString(csvContent: String, fileName: String = "texte_presse_papier.csv"): CsvParseResult {
        val reader = BufferedReader(csvContent.reader())
        return parseCsvReader(reader, fileName)
    }

    /**
     * Robust RFC 4180 CSV parser supporting quoted values with embedded commas, semicolons, and newlines.
     */
    private fun parseCsvReader(reader: BufferedReader, fileName: String): CsvParseResult {
        val rawContent = reader.readText()
        if (rawContent.isBlank()) {
            return CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Le fichier est vide."))
        }

        // Clean UTF-8 BOM if present
        val cleanContent = rawContent.removePrefix("\uFEFF")
        val delimiter = detectDelimiter(cleanContent)

        // Parse records (respecting quotes that span across newlines)
        val records = parseCsvRecords(cleanContent, delimiter)
        if (records.isEmpty()) {
            return CsvParseResult(emptyList(), 0, 0, ',', fileName, listOf("Aucune donnée trouvée."))
        }

        return parseRecordsToResult(records, fileName, delimiter)
    }

    /**
     * Converts a tabular list of records (header row + data rows) into a CsvParseResult.
     * Can be used by CSV, TSV, OpenDocument (ODS/ODT), Excel (XLS/XLSX), and Google Sheets imports.
     */
    fun parseRecordsToResult(
        records: List<List<String>>,
        fileName: String,
        detectedDelimiter: Char = ','
    ): CsvParseResult {
        if (records.isEmpty()) {
            return CsvParseResult(emptyList(), 0, 0, detectedDelimiter, fileName, listOf("Aucune donnée trouvée."))
        }

        // Identify header row
        val headerRow = records[0]
        val headerMap = buildHeaderMap(headerRow)

        if (!headerMap.containsKey(FieldType.TITLE)) {
            return CsvParseResult(
                emptyList(),
                records.size - 1,
                records.size - 1,
                detectedDelimiter,
                fileName,
                listOf("Colonne de titre introuvable. Assurez-vous d'avoir une colonne 'Titre', 'Title' ou 'Nom'.")
            )
        }

        val games = mutableListOf<GameItem>()
        var skipped = 0
        val errors = mutableListOf<String>()

        for (i in 1 until records.size) {
            val row = records[i]
            if (row.all { it.isBlank() }) continue // Skip empty line

            val title = headerMap[FieldType.TITLE]?.let { row.getOrNull(it)?.trim() } ?: ""
            if (title.isBlank()) {
                skipped++
                continue
            }

            val platform = headerMap[FieldType.PLATFORM]?.let { row.getOrNull(it)?.trim() } ?: "Autre"
            val genre = headerMap[FieldType.GENRE]?.let { row.getOrNull(it)?.trim() } ?: ""
            val yearStr = headerMap[FieldType.RELEASE_YEAR]?.let { row.getOrNull(it)?.trim() } ?: ""
            val releaseYear = parseYear(yearStr)
            val publisher = headerMap[FieldType.PUBLISHER]?.let { row.getOrNull(it)?.trim() } ?: ""
            val format = headerMap[FieldType.FORMAT]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "Cartouche"
            val region = headerMap[FieldType.REGION]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "PAL"
            val completeness = headerMap[FieldType.COMPLETENESS]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "CIB"
            val boxCondition = headerMap[FieldType.BOX_CONDITION]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "Très bon état"
            val manualCondition = headerMap[FieldType.MANUAL_CONDITION]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "Présent & Parfait"
            val mediaCondition = headerMap[FieldType.MEDIA_CONDITION]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "Parfait / Mint"
            val status = headerMap[FieldType.STATUS]?.let { row.getOrNull(it)?.trim() }?.ifBlank { null } ?: "Non commencé"
            val ratingStr = headerMap[FieldType.RATING]?.let { row.getOrNull(it)?.trim() } ?: ""
            val rating = parseRating(ratingStr)
            val purchasePriceStr = headerMap[FieldType.PURCHASE_PRICE]?.let { row.getOrNull(it)?.trim() } ?: ""
            val purchasePrice = parsePrice(purchasePriceStr)
            val estimatedValueStr = headerMap[FieldType.ESTIMATED_VALUE]?.let { row.getOrNull(it)?.trim() } ?: ""
            val estimatedValue = parsePrice(estimatedValueStr)
            val purchaseDate = headerMap[FieldType.PURCHASE_DATE]?.let { row.getOrNull(it)?.trim() } ?: ""
            val barcode = headerMap[FieldType.BARCODE]?.let { row.getOrNull(it)?.trim() } ?: ""
            val wishlistStr = headerMap[FieldType.IS_WISHLIST]?.let { row.getOrNull(it)?.trim() } ?: ""
            var isWishlist = parseIsWishlist(wishlistStr)
            if (!isWishlist && wishlistStr.isBlank()) {
                val normStatus = normalize(status)
                if (normStatus.contains("wish") || normStatus.contains("souhait") || normStatus.contains("recherche")) {
                    isWishlist = true
                }
            }
            val notes = headerMap[FieldType.NOTES]?.let { row.getOrNull(it)?.trim() } ?: ""
            val coverUrl = headerMap[FieldType.COVER_URL]?.let { row.getOrNull(it)?.trim() } ?: ""

            // If estimated value was omitted, fallback to purchase price if available
            val finalEstimatedValue = if (estimatedValue <= 0.0 && purchasePrice > 0.0) purchasePrice else estimatedValue

            val game = GameItem(
                title = title,
                platform = platform,
                genre = genre,
                releaseYear = releaseYear,
                publisher = publisher,
                format = format,
                region = region,
                completeness = completeness,
                boxCondition = boxCondition,
                manualCondition = manualCondition,
                mediaCondition = mediaCondition,
                status = status,
                rating = rating,
                purchasePrice = purchasePrice,
                estimatedValue = finalEstimatedValue,
                purchaseDate = purchaseDate,
                barcode = barcode,
                isWishlist = isWishlist,
                notes = notes,
                coverUrl = coverUrl
            )
            games.add(game)
        }

        return CsvParseResult(
            games = games,
            totalRows = records.size - 1,
            skippedRows = skipped,
            detectedDelimiter = detectedDelimiter,
            fileName = fileName,
            errorMessages = errors
        )
    }

    /**
     * Parses CSV text into records and fields, correctly handling quoted values with embedded newlines,
     * commas, semicolons, and doubled quotes.
     */
    fun parseCsvRecords(content: String, delimiter: Char = detectDelimiter(content)): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRecord = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length && content[i + 1] == '"') {
                        // Escaped quote: "" -> "
                        currentField.append('"')
                        i++
                    } else {
                        // Closing quote
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    delimiter -> {
                        currentRecord.add(currentField.toString().trim())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        // Check if followed by \n
                        if (i + 1 < content.length && content[i + 1] == '\n') {
                            i++
                        }
                        currentRecord.add(currentField.toString().trim())
                        currentField.setLength(0)
                        records.add(ArrayList(currentRecord))
                        currentRecord.clear()
                    }
                    '\n' -> {
                        currentRecord.add(currentField.toString().trim())
                        currentField.setLength(0)
                        records.add(ArrayList(currentRecord))
                        currentRecord.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            records.add(currentRecord)
        }

        return records
    }

    /**
     * Detects delimiter by counting occurrences on the first non-empty line without splitting the full file.
     */
    fun detectDelimiter(content: String): Char {
        var lineStart = 0
        var firstLine: String? = null
        while (lineStart < content.length) {
            val nl = content.indexOf('\n', lineStart)
            val lineEnd = if (nl == -1) content.length else nl
            val candidate = content.substring(lineStart, lineEnd).trimEnd('\r')
            if (candidate.isNotBlank()) {
                firstLine = candidate
                break
            }
            if (nl == -1) break
            lineStart = nl + 1
        }
        if (firstLine == null) return ','

        var commaCount = 0
        var semiCount = 0
        var tabCount = 0
        for (ch in firstLine) {
            when (ch) {
                ',' -> commaCount++
                ';' -> semiCount++
                '\t' -> tabCount++
            }
        }

        return when {
            semiCount > commaCount && semiCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semiCount -> '\t'
            else -> ','
        }
    }

    private enum class FieldType {
        TITLE, PLATFORM, GENRE, RELEASE_YEAR, PUBLISHER, FORMAT, REGION,
        COMPLETENESS, BOX_CONDITION, MANUAL_CONDITION, MEDIA_CONDITION,
        STATUS, RATING, PURCHASE_PRICE, ESTIMATED_VALUE, PURCHASE_DATE,
        BARCODE, IS_WISHLIST, NOTES, COVER_URL
    }

    private fun normalize(str: String): String {
        val n = Normalizer.normalize(str, Normalizer.Form.NFD)
        return n.replace(COMBINING_MARKS_REGEX, "")
            .lowercase(Locale.ROOT)
            .replace(NON_ALNUM_REGEX, " ")
            .trim()
            .replace(WHITESPACE_REGEX, " ")
    }

    private fun buildHeaderMap(headerRow: List<String>): Map<FieldType, Int> {
        val map = mutableMapOf<FieldType, Int>()

        headerRow.forEachIndexed { index, rawHeader ->
            val norm = normalize(rawHeader)
            when {
                norm.matches(TITLE_HEADER_REGEX) ||
                        (norm.contains("titre") && !norm.contains("sous")) -> {
                    if (!map.containsKey(FieldType.TITLE)) map[FieldType.TITLE] = index
                }
                norm.matches(PLATFORM_HEADER_REGEX) ||
                        norm.contains("plateforme") || norm.contains("console") -> {
                    if (!map.containsKey(FieldType.PLATFORM)) map[FieldType.PLATFORM] = index
                }
                norm.matches(GENRE_HEADER_REGEX) -> {
                    if (!map.containsKey(FieldType.GENRE)) map[FieldType.GENRE] = index
                }
                norm.matches(YEAR_HEADER_REGEX) ||
                        norm.contains("annee") -> {
                    if (!map.containsKey(FieldType.RELEASE_YEAR)) map[FieldType.RELEASE_YEAR] = index
                }
                norm.matches(PUBLISHER_HEADER_REGEX) ||
                        norm.contains("editeur") -> {
                    if (!map.containsKey(FieldType.PUBLISHER)) map[FieldType.PUBLISHER] = index
                }
                norm.matches(FORMAT_HEADER_REGEX) -> {
                    if (!map.containsKey(FieldType.FORMAT)) map[FieldType.FORMAT] = index
                }
                norm.matches(REGION_HEADER_REGEX) -> {
                    if (!map.containsKey(FieldType.REGION)) map[FieldType.REGION] = index
                }
                norm.matches(COMPLETENESS_HEADER_REGEX) ||
                        norm.contains("completude") -> {
                    if (!map.containsKey(FieldType.COMPLETENESS)) map[FieldType.COMPLETENESS] = index
                }
                norm.contains("etat boite") || norm.contains("box condition") -> {
                    if (!map.containsKey(FieldType.BOX_CONDITION)) map[FieldType.BOX_CONDITION] = index
                }
                norm.contains("etat notice") || norm.contains("manual condition") -> {
                    if (!map.containsKey(FieldType.MANUAL_CONDITION)) map[FieldType.MANUAL_CONDITION] = index
                }
                norm.contains("etat support") || norm.contains("media condition") || norm.contains("etat disque") -> {
                    if (!map.containsKey(FieldType.MEDIA_CONDITION)) map[FieldType.MEDIA_CONDITION] = index
                }
                norm.matches(GENERAL_CONDITION_HEADER_REGEX) -> {
                    if (!map.containsKey(FieldType.BOX_CONDITION)) map[FieldType.BOX_CONDITION] = index
                }
                norm.matches(STATUS_HEADER_REGEX) -> {
                    if (!map.containsKey(FieldType.STATUS)) map[FieldType.STATUS] = index
                }
                norm.contains("notes") || norm.contains("remarque") || norm.contains("commentaire") ||
                        norm.contains("description") || norm.matches(NOTES_HEADER_REGEX) -> {
                    if (!map.containsKey(FieldType.NOTES)) map[FieldType.NOTES] = index
                }
                norm.matches(RATING_HEADER_REGEX) ||
                        norm.startsWith("note ") || norm.startsWith("note/") ||
                        norm.contains("rating") || norm.contains("note 5") -> {
                    if (!map.containsKey(FieldType.RATING)) map[FieldType.RATING] = index
                }
                norm.matches(PRICE_HEADER_REGEX) ||
                        norm.contains("prix d achat") || norm.contains("prix achat") || norm.contains("purchase price") ||
                        ((norm.contains("prix") || norm.contains("price")) && !norm.contains("estime") && !norm.contains("estimate")) -> {
                    if (!map.containsKey(FieldType.PURCHASE_PRICE)) map[FieldType.PURCHASE_PRICE] = index
                }
                norm.contains("valeur estimee") || norm.contains("valeur") || norm.contains("estimated value") ||
                        norm.contains("cote") || norm.contains("argus") || norm.contains("valeur marchande") || norm.contains("pricecharting") -> {
                    if (!map.containsKey(FieldType.ESTIMATED_VALUE)) map[FieldType.ESTIMATED_VALUE] = index
                }
                norm.contains("date d achat") || norm.contains("purchase date") -> {
                    if (!map.containsKey(FieldType.PURCHASE_DATE)) map[FieldType.PURCHASE_DATE] = index
                }
                norm.contains("code barre") || norm.contains("barcode") || norm.contains("ean") || norm.contains("upc") -> {
                    if (!map.containsKey(FieldType.BARCODE)) map[FieldType.BARCODE] = index
                }
                norm.contains("wishlist") || norm.contains("statut collection") || norm.contains("type") -> {
                    if (!map.containsKey(FieldType.IS_WISHLIST)) map[FieldType.IS_WISHLIST] = index
                }
                norm.contains("pochette") || norm.contains("cover") || norm.contains("image") || norm.contains("url") -> {
                    if (!map.containsKey(FieldType.COVER_URL)) map[FieldType.COVER_URL] = index
                }
            }
        }

        return map
    }

    private fun parseYear(str: String): Int {
        val digits = str.filter { it.isDigit() }
        val parsed = digits.toIntOrNull() ?: 2000
        return if (parsed in 1970..2035) parsed else 2000
    }

    private fun parseRating(str: String): Int {
        val clean = str.replace("/5", "").trim()
        val digits = clean.filter { it.isDigit() }
        return digits.toIntOrNull()?.coerceIn(0, 5) ?: 0
    }

    private fun parsePrice(str: String): Double {
        if (str.isBlank()) return 0.0
        val clean = str.replace("€", "").replace("$", "").replace("£", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace(WHITESPACE_REGEX, "")
            .replace(",", ".")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun parseIsWishlist(str: String): Boolean {
        val norm = normalize(str)
        if (norm == "false" || norm == "non" || norm == "0" || norm == "possede" || norm == "owned" || norm == "collection") {
            return false
        }
        return norm.contains("wish") || norm.contains("souhait") || norm.contains("recherche") || norm == "true" || norm == "1" || norm == "oui"
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
                Log.w(TAG, "Unable to query file display name", e)
            }
        }
        return name ?: uri.lastPathSegment
    }
}
