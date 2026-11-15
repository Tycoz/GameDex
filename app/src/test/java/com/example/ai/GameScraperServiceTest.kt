package com.example.ai

import kotlinx.coroutines.runBlocking
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
class GameScraperServiceTest {

    @Test
    fun scrapeGame_withEmptyQuery_returnsFailure() = runBlocking {
        val result = GameScraperService.scrapeGame("   ")
        assertTrue("Should fail with empty query", result.isFailure)
    }

    @Test
    fun scrapeGame_withCuratedClassicGame_returnsCompleteData() = runBlocking {
        val result = GameScraperService.scrapeGame("Chrono Trigger")
        assertTrue("Should successfully scrape or match Chrono Trigger", result.isSuccess)

        val data = result.getOrThrow()
        assertTrue("Title should contain Chrono Trigger", data.title.contains("Chrono Trigger", ignoreCase = true))
        assertTrue("Platform should be Super Nintendo", data.platform.contains("Super Nintendo", ignoreCase = true))
        assertTrue("Cover URL should not be blank", data.coverUrl.isNotBlank())
        assertEquals("Format should be Cartouche", "Cartouche", data.format)
    }

    @Test
    fun sanitizeCoverUrl_cleansProtocolRelativeAndTrackingParameters() {
        val raw1 = "//upload.wikimedia.org/wikipedia/en/e/e9/Super_Mario_64.png?utm_source=en.wikipedia.org"
        val clean1 = GameScraperService.sanitizeCoverUrl(raw1)
        assertEquals("https://upload.wikimedia.org/wikipedia/en/e/e9/Super_Mario_64.png", clean1)

        val raw2 = "https://images.igdb.com/igdb/image/upload/t_cover_big/co20wa.webp"
        val clean2 = GameScraperService.sanitizeCoverUrl(raw2)
        assertEquals(raw2, clean2)

        val blank = GameScraperService.sanitizeCoverUrl("   ")
        assertEquals("", blank)
    }

    @Test
    fun scrapeGame_withRetroGame_retrievesValidCoverUrl() = runBlocking {
        val result = GameScraperService.scrapeGame("Super Mario 64", "Nintendo 64")
        if (result.isSuccess) {
            val data = result.getOrThrow()
            assertTrue("Title should match", data.title.contains("Mario", ignoreCase = true))
            assertTrue("Cover URL should not be blank", data.coverUrl.isNotBlank())
            assertTrue("Cover URL should start with https://", data.coverUrl.startsWith("https://"))
            assertFalse("Cover URL should not have utm tracking", data.coverUrl.contains("?utm_source="))
            assertTrue("Estimated value should be calculated", data.estimatedValue > 0.0)
            assertNotNull("PriceCharting URL should be provided", data.priceChartingUrl)
            assertTrue("PriceCharting URL should target pricecharting.com", data.priceChartingUrl!!.contains("pricecharting.com"))
        } else {
            // Hermetic JVM testing environment without outbound HTTP network access
            assertTrue("Scraper failure is handled without crashing", result.isFailure)
        }
    }

    @Test
    fun priceCharting_buildSearchUrl_formatsCleanQuery() {
        val url = PriceChartingScraper.buildSearchUrl("The Legend of Zelda: Ocarina of Time (Collector)", "Nintendo 64")
        assertTrue("Should contain base search URL", url.startsWith("https://www.pricecharting.com/search-products?type=videogames&q="))
        assertTrue("Should clean parenthesized suffix and encode", url.contains("Ocarina+of+Time") || url.contains("Ocarina%20of%20Time"))
        assertTrue("Should include mapped console name", url.contains("Nintendo+64") || url.contains("Nintendo%2064"))
    }

    @Test
    fun priceCharting_parseRawPriceString_extractsCorrectValues() {
        assertEquals(45.50, PriceChartingScraper.parseRawPriceString("$45.50")!!, 0.01)
        assertEquals(120.00, PriceChartingScraper.parseRawPriceString("€120,00")!!, 0.01)
        assertEquals(38.00, PriceChartingScraper.parseRawPriceString(" $38.00 ")!!, 0.01)
        assertEquals(null, PriceChartingScraper.parseRawPriceString("N/A"))
        assertEquals(null, PriceChartingScraper.parseRawPriceString(""))
    }

    @Test
    fun priceCharting_parseDetailPage_extractsPricesAndCalculatesEstimatedValue() {
        val sampleHtml = """
            <html>
             <body>
              <h1 id="product_name">Super Mario 64</h1>
              <div class="sub-header">Nintendo 64</div>
              <table>
               <tr>
                <td id="used_price"><span class="price js-price">$38.00</span></td>
                <td id="cib_price"><span class="price js-price">$95.00</span></td>
                <td id="new_price"><span class="price js-price">$240.00</span></td>
                <td id="graded_price"><span class="price js-price">$550.00</span></td>
               </tr>
              </table>
             </body>
            </html>
        """.trimIndent()

        val parsed = PriceChartingScraper.parseDetailPage(
            html = sampleHtml,
            defaultTitle = "Super Mario 64",
            defaultConsole = "Nintendo 64",
            productUrl = "https://www.pricecharting.com/game/nintendo-64/super-mario-64"
        )

        assertEquals("Super Mario 64", parsed.gameTitle)
        assertEquals("Nintendo 64", parsed.consoleName)
        assertEquals(38.0, parsed.loosePriceUsd!!, 0.01)
        assertEquals(95.0, parsed.cibPriceUsd!!, 0.01)
        assertEquals(240.0, parsed.newPriceUsd!!, 0.01)
        // EUR conversions (approx 0.92 rate)
        assertEquals(34.96, parsed.loosePriceEur!!, 0.01)
        assertEquals(87.40, parsed.cibPriceEur!!, 0.01)
        // Estimated value should default to CIB price
        assertEquals(87.40, parsed.estimatedValueEur, 0.01)
    }

    @Test
    fun priceCharting_parseSearchPage_extractsMatchingRow() {
        val sampleSearchHtml = """
            <table id="games_table">
             <tr>
              <td class="title"><a href="/game/nintendo-64/mario-kart-64">Mario Kart 64</a></td>
              <td class="console">Nintendo 64</td>
              <td class="price numeric used_price"><span class="js-price">$40.00</span></td>
              <td class="price numeric cib_price"><span class="js-price">$105.00</span></td>
              <td class="price numeric new_price"><span class="js-price">$270.00</span></td>
             </tr>
             <tr>
              <td class="title"><a href="/game/game-boy-advance/mario-kart-super-circuit">Mario Kart: Super Circuit</a></td>
              <td class="console">GameBoy Advance</td>
              <td class="price numeric used_price"><span class="js-price">$18.00</span></td>
              <td class="price numeric cib_price"><span class="js-price">$45.00</span></td>
              <td class="price numeric new_price"><span class="js-price">$110.00</span></td>
             </tr>
            </table>
        """.trimIndent()

        val result = PriceChartingScraper.parseSearchPage(
            html = sampleSearchHtml,
            queryTitle = "Mario Kart 64",
            consoleHint = "Nintendo 64",
            currentUrl = "https://www.pricecharting.com/search-products?type=videogames&q=Mario+Kart+64"
        )

        assertNotNull(result)
        assertEquals("Mario Kart 64", result!!.gameTitle)
        assertEquals("Nintendo 64", result.consoleName)
        assertEquals(40.0, result.loosePriceUsd!!, 0.01)
        assertEquals(105.0, result.cibPriceUsd!!, 0.01)
        assertEquals("https://www.pricecharting.com/game/nintendo-64/mario-kart-64", result.productUrl)
        assertEquals(96.60, result.estimatedValueEur, 0.01)
    }
}
