package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.GameItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportUtil {

    private val HEADERS = listOf(
        "Titre",
        "Plateforme",
        "Genre",
        "Année",
        "Éditeur",
        "Format",
        "Région",
        "Complétude",
        "État Boîte",
        "État Notice",
        "État Support",
        "Statut",
        "Note (/5)",
        "Prix d'achat (€)",
        "Valeur estimée (€)",
        "Date d'achat",
        "Code-barres",
        "Statut Collection",
        "Notes & Remarques",
        "URL Pochette"
    )

    /**
     * Generates a CSV formatted string with UTF-8 BOM (\uFEFF) so Excel, Google Sheets,
     * and Apple Numbers display French accents correctly.
     */
    fun generateCsv(
        games: List<GameItem>,
        delimiter: Char = ','
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM
        sb.append('\uFEFF')

        // Header line
        sb.append(HEADERS.joinToString(separator = delimiter.toString()) { escapeCell(it, delimiter) })
        sb.append("\r\n")

        // Rows
        for (game in games) {
            val collectionStatus = if (game.isWishlist) "Wishlist" else "Possédé"
            val purchasePriceStr = if (game.purchasePrice > 0) String.format(Locale.US, "%.2f", game.purchasePrice) else "0.00"
            val estimatedValueStr = if (game.estimatedValue > 0) String.format(Locale.US, "%.2f", game.estimatedValue) else "0.00"
            val ratingStr = if (game.rating > 0) "${game.rating}/5" else "Non noté"

            val row = listOf(
                game.title,
                game.platform,
                game.genre,
                if (game.releaseYear > 0) game.releaseYear.toString() else "",
                game.publisher,
                game.format,
                game.region,
                game.completeness,
                game.boxCondition,
                game.manualCondition,
                game.mediaCondition,
                game.status,
                ratingStr,
                purchasePriceStr,
                estimatedValueStr,
                game.purchaseDate,
                game.barcode,
                collectionStatus,
                game.notes,
                game.coverUrl
            )

            sb.append(row.joinToString(separator = delimiter.toString()) { escapeCell(it, delimiter) })
            sb.append("\r\n")
        }

        return sb.toString()
    }

    private fun escapeCell(value: String, delimiter: Char): String {
        val needsQuotes = value.contains(delimiter) ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')

        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Writes CSV content directly to a user-selected SAF Uri (Storage Access Framework).
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Creates a temporary CSV file in the app's cache directory and returns a shareable FileProvider Uri.
     */
    fun createCacheCsvFile(context: Context, csvContent: String, baseName: String = "collection_gamedex"): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "${baseName}_$dateSuffix.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Prepares an Intent to send the CSV file directly to Google Drive.
     * If the Google Drive app is installed, targets it directly; otherwise falls back to system chooser.
     */
    fun openGoogleDriveExport(context: Context, fileUri: Uri): Boolean {
        val driveIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            setPackage("com.google.android.apps.docs")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Collection GameDex (CSV)")
            putExtra(Intent.EXTRA_TEXT, "Exportation de ma collection de jeux vidéo au format CSV.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (context.packageManager.resolveActivity(driveIntent, 0) != null) {
                context.startActivity(driveIntent)
                true
            } else {
                // Fallback to chooser where Drive is typically present
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Collection GameDex (CSV) pour Google Drive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Enregistrer dans Google Drive"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Standard Android Share Sheet for sharing the CSV to any application.
     */
    fun shareCsvFile(context: Context, fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Exportation Collection Jeux Vidéo (GameDex)")
            putExtra(Intent.EXTRA_TEXT, "Voici l'export CSV de ma collection de jeux vidéo.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Partager le fichier CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Copies CSV text to the system clipboard.
     */
    fun copyCsvToClipboard(context: Context, csvContent: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Export CSV GameDex", csvContent)
        clipboard.setPrimaryClip(clip)
    }

    fun generateDefaultFileName(scope: String): String {
        val dateSuffix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val cleanScope = when (scope) {
            "wishlist" -> "wishlist"
            "owned" -> "collection"
            else -> "jeux"
        }
        return "gamedex_${cleanScope}_$dateSuffix.csv"
    }
}
