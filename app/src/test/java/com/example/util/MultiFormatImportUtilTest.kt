package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiFormatImportUtilTest {

    @Test
    fun extractGoogleSheetExportUrl_withStandardUrl_extractsIdAndBuildsCsvUrl() {
        val standardUrl = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit?usp=sharing"
        val result = MultiFormatImportUtil.extractGoogleSheetExportUrl(standardUrl)

        assertTrue(result.isSuccess)
        val exportUrl = result.getOrThrow()
        assertEquals(
            "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/export?format=csv",
            exportUrl
        )
    }

    @Test
    fun extractGoogleSheetExportUrl_withGidParam_preservesGid() {
        val urlWithGid = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=123456789"
        val result = MultiFormatImportUtil.extractGoogleSheetExportUrl(urlWithGid)

        assertTrue(result.isSuccess)
        val exportUrl = result.getOrThrow()
        assertEquals(
            "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/export?format=csv&gid=123456789",
            exportUrl
        )
    }

    @Test
    fun extractGoogleSheetExportUrl_withInvalidUrl_returnsFailure() {
        val invalidUrl = "https://example.com/not-a-google-sheet"
        val result = MultiFormatImportUtil.extractGoogleSheetExportUrl(invalidUrl)

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun parseVcs_withVEventCalendarEntries_extractsGameItems() {
        val vcsContent = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Video Game Tracker//FR
            BEGIN:VEVENT
            UID:game-001@collection
            SUMMARY:Super Mario Odyssey
            LOCATION:Nintendo Switch
            CATEGORIES:Plates-formes
            DTSTART:20171027T000000Z
            DESCRIPTION:Editeur: Nintendo; Prix: 45.00; Jeu complet avec boîte
            URL:https://example.com/mario.jpg
            END:VEVENT
            BEGIN:VEVENT
            UID:game-002@collection
            SUMMARY:The Legend of Zelda: Breath of the Wild
            LOCATION:Nintendo Switch
            CATEGORIES:Aventure, Action
            DTSTART:20170303T000000Z
            DESCRIPTION:Publisher: Nintendo; Price: 50.00; Chef d'oeuvre
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = MultiFormatImportUtil.parseVcs(vcsContent, "test_collection.vcs")

        assertEquals(2, result.games.size)
        assertEquals(0, result.errorMessages.size)

        val game1 = result.games[0]
        assertEquals("Super Mario Odyssey", game1.title)
        assertEquals("Nintendo Switch", game1.platform)
        assertEquals("Plates-formes", game1.genre)
        assertEquals(2017, game1.releaseYear)
        assertEquals("Nintendo", game1.publisher)
        assertEquals(45.0, game1.purchasePrice, 0.01)
        assertEquals("https://example.com/mario.jpg", game1.coverUrl)
        assertTrue(game1.notes.contains("Jeu complet avec boîte"))

        val game2 = result.games[1]
        assertEquals("The Legend of Zelda: Breath of the Wild", game2.title)
        assertEquals("Nintendo Switch", game2.platform)
        assertEquals("Aventure", game2.genre)
        assertEquals(2017, game2.releaseYear)
        assertEquals("Nintendo", game2.publisher)
        assertEquals(50.0, game2.purchasePrice, 0.01)
    }

    @Test
    fun parseVcs_withVCardEntries_extractsGames() {
        val vcfContent = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Chrono Trigger
            NOTE:Editeur: Square; Prix: 120.00; Version Super Nintendo cartouche japonaise
            CATEGORIES:Super Nintendo, RPG
            END:VCARD
        """.trimIndent()

        val result = MultiFormatImportUtil.parseVcs(vcfContent, "chrono.vcf")

        assertEquals(1, result.games.size)
        val game = result.games[0]
        assertEquals("Chrono Trigger", game.title)
        assertEquals("Super Nintendo", game.platform)
        assertEquals("RPG", game.genre)
        assertEquals("Square", game.publisher)
        assertEquals(120.0, game.purchasePrice, 0.01)
    }

    @Test
    fun parseXmlSpreadsheet2003_withXmlRows_extractsValidGames() {
        val xml2003 = """<?xml version="1.0"?>
            <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
             xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
             <Worksheet ss:Name="Jeux">
              <Table>
               <Row>
                <Cell><Data ss:Type="String">Titre</Data></Cell>
                <Cell><Data ss:Type="String">Plateforme</Data></Cell>
                <Cell><Data ss:Type="String">Année</Data></Cell>
                <Cell><Data ss:Type="String">Prix</Data></Cell>
                <Cell><Data ss:Type="String">Statut</Data></Cell>
               </Row>
               <Row>
                <Cell><Data ss:Type="String">Final Fantasy VII</Data></Cell>
                <Cell><Data ss:Type="String">PlayStation</Data></Cell>
                <Cell><Data ss:Type="Number">1997</Data></Cell>
                <Cell><Data ss:Type="Number">35.00</Data></Cell>
                <Cell><Data ss:Type="String">Terminé</Data></Cell>
               </Row>
               <Row>
                <Cell><Data ss:Type="String">Metroid Prime</Data></Cell>
                <Cell><Data ss:Type="String">GameCube</Data></Cell>
                <Cell><Data ss:Type="Number">2002</Data></Cell>
                <Cell><Data ss:Type="Number">25.00</Data></Cell>
                <Cell><Data ss:Type="String">En cours</Data></Cell>
               </Row>
              </Table>
             </Worksheet>
            </Workbook>
        """.trimIndent()

        val result = MultiFormatImportUtil.parseXmlSpreadsheet2003(xml2003, "test_excel_2003.xls")

        assertEquals(2, result.games.size)
        val ff7 = result.games[0]
        assertEquals("Final Fantasy VII", ff7.title)
        assertEquals("PlayStation", ff7.platform)
        assertEquals(1997, ff7.releaseYear)
        assertEquals(35.0, ff7.purchasePrice, 0.01)
        assertEquals("Terminé", ff7.status)

        val metroid = result.games[1]
        assertEquals("Metroid Prime", metroid.title)
        assertEquals("GameCube", metroid.platform)
        assertEquals(2002, metroid.releaseYear)
        assertEquals(25.0, metroid.purchasePrice, 0.01)
    }

    @Test
    fun extractRowsFromOpenDocumentXml_parsesRowsAndCells() {
        val odfContentXml = """<?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                <office:body>
                    <office:spreadsheet>
                        <table:table table:name="Collection">
                            <table:table-row>
                                <table:table-cell><text:p>Titre</text:p></table:table-cell>
                                <table:table-cell><text:p>Plateforme</text:p></table:table-cell>
                                <table:table-cell><text:p>Année</text:p></table:table-cell>
                            </table:table-row>
                            <table:table-row>
                                <table:table-cell><text:p>Pokemon Rouge</text:p></table:table-cell>
                                <table:table-cell><text:p>Game Boy</text:p></table:table-cell>
                                <table:table-cell><text:p>1996</text:p></table:table-cell>
                            </table:table-row>
                        </table:table>
                    </office:spreadsheet>
                </office:body>
            </office:document-content>
        """.trimIndent()

        val rows = MultiFormatImportUtil.extractRowsFromOpenDocumentXml(odfContentXml)

        assertEquals(2, rows.size)
        assertEquals(listOf("Titre", "Plateforme", "Année"), rows[0])
        assertEquals(listOf("Pokemon Rouge", "Game Boy", "1996"), rows[1])

        val parseResult = CsvImportUtil.parseRecordsToResult(rows, "test_ods.ods", ',')
        assertEquals(1, parseResult.games.size)
        val game = parseResult.games[0]
        assertEquals("Pokemon Rouge", game.title)
        assertEquals("Game Boy", game.platform)
        assertEquals(1996, game.releaseYear)
    }
}
