package com.example.data.marketplace

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DealFinding
import com.example.data.model.GameItem
import com.example.data.model.PriceAlert
import com.example.data.model.PriceAlertSerializer
import com.example.data.model.cleanNotes
import com.example.data.model.detectedDeal
import com.example.data.model.isPriceAlertEnabled
import com.example.data.model.maxTargetPrice
import com.example.data.model.priceAlert
import com.example.data.model.withPriceAlert
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WishlistPriceAlertTest {

    @Test
    fun serializeAndDeserialize_priceAlert_roundTripMatches() {
        val originalAlert = PriceAlert(
            maxTargetPrice = 45.0,
            isEnabled = true,
            deal = DealFinding(
                price = 38.50,
                platform = "Vinted",
                url = "https://www.vinted.fr/items/12345-super-mario",
                title = "Super Mario 64 N64",
                foundAt = 1730000000000L
            ),
            lastChecked = 1730000005000L
        )

        val serialized = PriceAlertSerializer.serialize(originalAlert)
        assertTrue(serialized.contains("max=45.0"))
        assertTrue(serialized.contains("enabled=true"))
        assertTrue(serialized.contains("dealPrice=38.5"))
        assertTrue(serialized.contains("dealPlatform=Vinted"))

        val notesWithUserText = "Exemplaire en boîte que je cherche depuis longtemps.\n$serialized"
        val parsed = PriceAlertSerializer.deserialize(notesWithUserText)

        assertEquals(45.0, parsed.maxTargetPrice, 0.01)
        assertTrue(parsed.isEnabled)
        assertNotNull(parsed.deal)
        assertEquals(38.50, parsed.deal!!.price, 0.01)
        assertEquals("Vinted", parsed.deal!!.platform)
        assertEquals("https://www.vinted.fr/items/12345-super-mario", parsed.deal!!.url)
        assertEquals("Super Mario 64 N64", parsed.deal!!.title)

        val clean = PriceAlertSerializer.stripConfigFromNotes(notesWithUserText)
        assertEquals("Exemplaire en boîte que je cherche depuis longtemps.", clean)
    }

    @Test
    fun gameItemExtension_withPriceAlert_updatesPropertiesAccurately() {
        val game = GameItem(
            title = "The Legend of Zelda: Majora's Mask",
            platform = "Nintendo 64",
            isWishlist = true,
            notes = "Recherche version boîte PAL holofoil."
        )

        // Initially no alert
        assertEquals(0.0, game.maxTargetPrice, 0.001)
        assertFalse(game.isPriceAlertEnabled)
        assertNull(game.detectedDeal)

        // Set alert
        val updatedGame = game.withPriceAlert(
            maxTargetPrice = 60.0,
            isEnabled = true
        )

        assertEquals(60.0, updatedGame.maxTargetPrice, 0.001)
        assertTrue(updatedGame.isPriceAlertEnabled)
        assertEquals("Recherche version boîte PAL holofoil.", updatedGame.cleanNotes)

        // Add detected deal
        val deal = DealFinding(
            price = 52.0,
            platform = "eBay",
            url = "https://www.ebay.fr/itm/987654321",
            title = "Zelda Majora's Mask N64 complet"
        )
        val gameWithDeal = updatedGame.withPriceAlert(
            updatedGame.priceAlert.copy(deal = deal)
        )

        assertNotNull(gameWithDeal.detectedDeal)
        assertEquals(52.0, gameWithDeal.detectedDeal!!.price, 0.01)
        assertEquals("eBay", gameWithDeal.detectedDeal!!.platform)
        assertTrue(gameWithDeal.detectedDeal!!.price <= gameWithDeal.maxTargetPrice)
    }

    @Test
    fun marketplacePreferences_savesAndRetrievesAccounts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = MarketplacePreferencesManager(context)

        // Test Vinted
        prefs.saveVintedAccount(
            username = "johan_retrogaming",
            domain = "vinted.fr",
            condition = "Très bon état"
        )
        val vinted = prefs.getVintedAccount()
        assertEquals("johan_retrogaming", vinted.username)
        assertEquals("vinted.fr", vinted.domain)
        assertTrue(vinted.isConnected)
        assertEquals("Très bon état", vinted.filterCondition)

        // Test eBay
        prefs.saveEbayAccount(
            username = "johan-collector-fr",
            domain = "ebay.fr",
            buyItNowOnly = true
        )
        val ebay = prefs.getEbayAccount()
        assertEquals("johan-collector-fr", ebay.username)
        assertEquals("ebay.fr", ebay.domain)
        assertTrue(ebay.isConnected)
        assertTrue(ebay.buyItNowOnly)

        assertEquals(2, prefs.connectedAccountsCount)

        // Test Disconnect Vinted
        prefs.disconnectVinted()
        assertFalse(prefs.getVintedAccount().isConnected)
        assertEquals(1, prefs.connectedAccountsCount)
    }

    @Test
    fun marketplacePriceChecker_withMatchingTargetPrice_findsOpportunity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = MarketplacePreferencesManager(context)
        prefs.saveVintedAccount("test_user", "vinted.fr")

        val game = GameItem(
            title = "Super Mario 64",
            platform = "Nintendo 64",
            isWishlist = true,
            estimatedValue = 45.0
        ).withPriceAlert(maxTargetPrice = 50.0, isEnabled = true)

        val deal = MarketplacePriceChecker.checkGameDeals(game, prefs)
        assertNotNull("Should identify a deal within budget", deal)
        assertTrue(deal!!.price <= 50.0)
        assertTrue(deal.url.contains("vinted") || deal.url.contains("ebay"))
    }
}
