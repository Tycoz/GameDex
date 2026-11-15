package com.example.util

import com.example.data.model.GameItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportUtilTest {

    @Test
    fun generateCsv_withEmptyList_producesHeaderWithBom() {
        val csv = CsvExportUtil.generateCsv(emptyList())

        // Check BOM
        assertTrue("CSV must start with UTF-8 BOM", csv.startsWith("\uFEFF"))

        val contentWithoutBom = csv.removePrefix("\uFEFF")
        val lines = contentWithoutBom.trim().split("\r\n")
        assertEquals(1, lines.size)

        val headers = lines[0].split(",")
        assertTrue("Header should contain Titre", headers.contains("Titre"))
        assertTrue("Header should contain Plateforme", headers.contains("Plateforme"))
        assertTrue("Header should contain Valeur estimée (€)", headers.contains("Valeur estimée (€)"))
    }

    @Test
    fun generateCsv_withGames_producesCorrectHeadersAndRows() {
        val testGames = listOf(
            GameItem(
                id = "game_1",
                title = "Super Mario World",
                platform = "Super Nintendo",
                genre = "Plates-formes",
                releaseYear = 1990,
                publisher = "Nintendo",
                format = "Cartouche",
                region = "PAL",
                completeness = "CIB",
                purchasePrice = 45.0,
                estimatedValue = 60.0,
                isWishlist = false,
                rating = 5,
                status = "Terminé"
            ),
            GameItem(
                id = "game_2",
                title = "Chrono Trigger",
                platform = "Super Nintendo",
                genre = "RPG",
                releaseYear = 1995,
                publisher = "Square",
                format = "Cartouche",
                region = "NTSC-J",
                completeness = "Loose",
                purchasePrice = 0.0,
                estimatedValue = 120.0,
                isWishlist = true,
                rating = 0,
                status = "Non commencé"
            )
        )

        val csv = CsvExportUtil.generateCsv(testGames, delimiter = ',')
        val lines = csv.removePrefix("\uFEFF").trim().split("\r\n")

        assertEquals("Should have 1 header line + 2 data rows", 3, lines.size)

        // Line 1: Super Mario World
        val row1 = lines[1]
        assertTrue("Row 1 contains title", row1.contains("Super Mario World"))
        assertTrue("Row 1 contains platform", row1.contains("Super Nintendo"))
        assertTrue("Row 1 contains price", row1.contains("45.00"))
        assertTrue("Row 1 contains value", row1.contains("60.00"))
        assertTrue("Row 1 contains Possédé", row1.contains("Possédé"))

        // Line 2: Chrono Trigger (Wishlist)
        val row2 = lines[2]
        assertTrue("Row 2 contains Chrono Trigger", row2.contains("Chrono Trigger"))
        assertTrue("Row 2 contains Wishlist", row2.contains("Wishlist"))
        assertTrue("Row 2 contains estimated value", row2.contains("120.00"))
    }

    @Test
    fun generateCsv_withSpecialCharacters_escapesCorrectly() {
        val gameWithCommasAndQuotes = GameItem(
            id = "game_special",
            title = "The Legend of Zelda: A Link to the Past, \"Collector Edition\"",
            platform = "Super Nintendo",
            notes = "Contient la carte d'Hyrule;\nNotice en français."
        )

        val csv = CsvExportUtil.generateCsv(listOf(gameWithCommasAndQuotes), delimiter = ',')
        val content = csv.removePrefix("\uFEFF")

        // In RFC 4180: quotes become double quotes and cell is wrapped in quotes
        assertTrue("Title should be quoted and inner quotes escaped", content.contains("\"The Legend of Zelda: A Link to the Past, \"\"Collector Edition\"\"\""))
    }

    @Test
    fun generateCsv_withSemicolonDelimiter_formatsWithSemicolons() {
        val testGames = listOf(
            GameItem(
                title = "Sonic the Hedgehog",
                platform = "Sega Mega Drive",
                publisher = "Sega"
            )
        )

        val csv = CsvExportUtil.generateCsv(testGames, delimiter = ';')
        val lines = csv.removePrefix("\uFEFF").trim().split("\r\n")

        assertTrue("Header should use semicolons", lines[0].contains(";"))
        assertFalse("Header should not use commas as delimiters", lines[0].contains(","))
        assertTrue("Row should use semicolons", lines[1].contains(";"))
    }

    @Test
    fun generateDefaultFileName_containsDateAndScope() {
        val ownedName = CsvExportUtil.generateDefaultFileName("owned")
        assertTrue(ownedName.startsWith("gamedex_collection_"))
        assertTrue(ownedName.endsWith(".csv"))

        val wishName = CsvExportUtil.generateDefaultFileName("wishlist")
        assertTrue(wishName.startsWith("gamedex_wishlist_"))
        assertTrue(wishName.endsWith(".csv"))
    }
}
