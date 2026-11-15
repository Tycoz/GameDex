package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameItemIntegrityTest {

    @Test
    fun toCreateMap_sanitizesBlankRequiredFieldsAndInvalidNumbers() {
        val raw = GameItem(
            title = "   ",
            platform = "",
            purchasePrice = -12.5,
            estimatedValue = Double.NaN,
            rating = 99
        )

        val map = raw.toCreateMap(userId = "user-123")

        assertEquals("user-123", map["userId"])
        assertEquals("Jeu sans titre", map["title"])
        assertEquals("Autre", map["platform"])
        assertEquals(0.0, map["purchasePrice"] as Double, 0.001)
        assertEquals(0.0, map["estimatedValue"] as Double, 0.001)
        assertEquals(5, map["rating"])
    }

    @Test
    fun toUpdateMap_clampsAllStringFieldsToFirestoreRuleLimits() {
        val raw = GameItem(
            id = "game-1",
            title = "A".repeat(300),
            platform = "B".repeat(120),
            genre = "C".repeat(100),
            publisher = "D".repeat(200),
            format = "E".repeat(100),
            region = "F".repeat(100),
            barcode = "G".repeat(100),
            boxCondition = "H".repeat(100),
            manualCondition = "I".repeat(100),
            mediaCondition = "J".repeat(100),
            completeness = "K".repeat(100),
            purchaseDate = "L".repeat(100),
            status = "M".repeat(100),
            notes = "N".repeat(2000),
            coverUrl = "https://example.com/${"x".repeat(800)}",
            rating = -3
        )

        val map = raw.toUpdateMap()

        assertTrue((map["title"] as String).length <= GameItem.MAX_TITLE_LENGTH)
        assertTrue((map["platform"] as String).length <= GameItem.MAX_PLATFORM_LENGTH)
        assertTrue((map["genre"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["publisher"] as String).length <= GameItem.MAX_PUBLISHER_LENGTH)
        assertTrue((map["format"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["region"] as String).length <= GameItem.MAX_TINY_TEXT_LENGTH)
        assertTrue((map["barcode"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["boxCondition"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["manualCondition"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["mediaCondition"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["completeness"] as String).length <= GameItem.MAX_SHORT_TEXT_LENGTH)
        assertTrue((map["purchaseDate"] as String).length <= GameItem.MAX_TINY_TEXT_LENGTH)
        assertTrue((map["status"] as String).length <= GameItem.MAX_TINY_TEXT_LENGTH)
        assertTrue((map["notes"] as String).length <= GameItem.MAX_NOTES_LENGTH)
        assertTrue((map["coverUrl"] as String).length <= GameItem.MAX_COVER_URL_LENGTH)
        assertEquals(0, map["rating"])
    }

    @Test
    fun applyToNotes_preservesPriceAlertConfigWithinFirestoreMaxNotesLength() {
        val longNotes = "Note détaillée ".repeat(100) // > 1400 chars
        val alert = PriceAlert(
            maxTargetPrice = 45.0,
            isEnabled = true,
            deal = DealFinding(
                price = 39.99,
                platform = "Vinted",
                url = "https://www.vinted.fr/items/123456",
                title = "Chrono Trigger SNES"
            ),
            lastChecked = 1700000000000L
        )

        val combined = PriceAlertSerializer.applyToNotes(longNotes, alert)
        assertTrue(combined.length <= GameItem.MAX_NOTES_LENGTH)

        val parsed = PriceAlertSerializer.deserialize(combined)
        assertTrue(parsed.isEnabled)
        assertEquals(45.0, parsed.maxTargetPrice, 0.001)
        assertNotNull(parsed.deal)
        assertEquals(39.99, parsed.deal!!.price, 0.001)
        assertEquals("Vinted", parsed.deal!!.platform)
    }
}
