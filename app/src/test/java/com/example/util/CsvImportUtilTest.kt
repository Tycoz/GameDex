package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportUtilTest {

    @Test
    fun parseCsvString_withStandardExportHeaders_parsesAllFields() {
        val csv = """
            Titre,Plateforme,Genre,Année,Éditeur,Format,Région,Complétude,Statut,Note (/5),Prix d'achat (€),Valeur estimée (€),Statut Collection,Notes & Remarques
            Super Mario 64,Nintendo 64,Plates-formes,1996,Nintendo,Cartouche,PAL,CIB,Terminé,5/5,40.00,85.50,Possédé,Notice légèrement écornée
            The Legend of Zelda,NES,Aventure,1986,Nintendo,Cartouche,PAL,Loose,Non commencé,4,15.00,45.00,Wishlist,Version cartouche dorée
        """.trimIndent()

        val result = CsvImportUtil.parseCsvString(csv)

        assertEquals("Should parse 2 games", 2, result.games.size)
        assertEquals(',', result.detectedDelimiter)

        val game1 = result.games[0]
        assertEquals("Super Mario 64", game1.title)
        assertEquals("Nintendo 64", game1.platform)
        assertEquals("Plates-formes", game1.genre)
        assertEquals(1996, game1.releaseYear)
        assertEquals("Nintendo", game1.publisher)
        assertEquals("Cartouche", game1.format)
        assertEquals("PAL", game1.region)
        assertEquals("CIB", game1.completeness)
        assertEquals("Terminé", game1.status)
        assertEquals(5, game1.rating)
        assertEquals(40.0, game1.purchasePrice, 0.01)
        assertEquals(85.50, game1.estimatedValue, 0.01)
        assertFalse("Should be owned, not wishlist", game1.isWishlist)
        assertEquals("Notice légèrement écornée", game1.notes)

        val game2 = result.games[1]
        assertEquals("The Legend of Zelda", game2.title)
        assertEquals(1986, game2.releaseYear)
        assertTrue("Should be wishlist", game2.isWishlist)
    }

    @Test
    fun parseCsvString_withSemicolonDelimiter_detectsDelimiterAndParses() {
        val csv = """
            Titre;Plateforme;Année;Prix d'achat (€);Valeur estimée (€)
            Metroid Prime;GameCube;2002;25,00 €;55,00 €
            F-Zero GX;GameCube;2003;30,00;60,00
        """.trimIndent()

        val result = CsvImportUtil.parseCsvString(csv)

        assertEquals(';', result.detectedDelimiter)
        assertEquals(2, result.games.size)

        val game1 = result.games[0]
        assertEquals("Metroid Prime", game1.title)
        assertEquals("GameCube", game1.platform)
        assertEquals(2002, game1.releaseYear)
        assertEquals(25.0, game1.purchasePrice, 0.01)
        assertEquals(55.0, game1.estimatedValue, 0.01)
    }

    @Test
    fun parseCsvString_withEnglishHeaders_mapsAccurately() {
        val csv = """
            Title,Platform,Year,Publisher,Price,Estimated Value,Wishlist
            Castlevania: Symphony of the Night,PlayStation,1997,Konami,80.00,160.00,false
            Metal Gear Solid,PlayStation,1998,Konami,20.00,45.00,true
        """.trimIndent()

        val result = CsvImportUtil.parseCsvString(csv)

        assertEquals(2, result.games.size)
        val game1 = result.games[0]
        assertEquals("Castlevania: Symphony of the Night", game1.title)
        assertEquals("PlayStation", game1.platform)
        assertEquals(1997, game1.releaseYear)
        assertEquals("Konami", game1.publisher)
        assertEquals(80.0, game1.purchasePrice, 0.01)
        assertEquals(160.0, game1.estimatedValue, 0.01)
        assertFalse(game1.isWishlist)

        val game2 = result.games[1]
        assertEquals("Metal Gear Solid", game2.title)
        assertTrue(game2.isWishlist)
    }

    @Test
    fun parseCsvString_withQuotesAndEmbeddedCommas_handlesProperly() {
        val csv = """
            Titre,Plateforme,Notes & Remarques
            "Resident Evil, Director's Cut",PlayStation,"Comprend la démo de Resident Evil 2, état parfait."
            "Street Fighter II: The World Warrior",Super Nintendo,"Cartouche loose"
        """.trimIndent()

        val result = CsvImportUtil.parseCsvString(csv)

        assertEquals(2, result.games.size)
        val game1 = result.games[0]
        assertEquals("Resident Evil, Director's Cut", game1.title)
        assertEquals("PlayStation", game1.platform)
        assertEquals("Comprend la démo de Resident Evil 2, état parfait.", game1.notes)
    }

    @Test
    fun parseCsvString_withoutTitleHeader_returnsError() {
        val invalidCsv = """
            Année,Éditeur,Prix
            1990,Nintendo,20
        """.trimIndent()

        val result = CsvImportUtil.parseCsvString(invalidCsv)

        assertTrue("Should produce error when title is missing", result.errorMessages.isNotEmpty())
        assertEquals(0, result.games.size)
    }

    @Test
    fun parseCsvString_withFrenchHeaderSynonymsAndNonBreakingSpaces_parsesCorrectly() {
        val csv = """
            Jeu;Console;Année de sortie;Cote;Statut;Etat
            Pokémon Version Or;Game Boy Color;2000;120,50 €;Recherche;Très bon état
            Super Smash Bros Melee;GameCube;2001;75,00 €;Terminé;Mint
        """.trimIndent()

        val result = CsvImportUtil.parseCsvString(csv)

        assertEquals(2, result.games.size)
        assertEquals(';', result.detectedDelimiter)

        val game1 = result.games[0]
        assertEquals("Pokémon Version Or", game1.title)
        assertEquals("Game Boy Color", game1.platform)
        assertEquals(2000, game1.releaseYear)
        assertEquals(120.50, game1.estimatedValue, 0.01)
        assertTrue("Statut Recherche should be mapped to wishlist", game1.isWishlist)
        assertEquals("Très bon état", game1.boxCondition)

        val game2 = result.games[1]
        assertEquals("Super Smash Bros Melee", game2.title)
        assertEquals("GameCube", game2.platform)
        assertEquals(75.0, game2.estimatedValue, 0.01)
        assertFalse(game2.isWishlist)
    }

    @Test
    fun parseCsvString_withEmptyContent_returnsEmpty() {
        val result = CsvImportUtil.parseCsvString("")
        assertEquals(0, result.games.size)
        assertTrue(result.errorMessages.isNotEmpty())
    }
}
