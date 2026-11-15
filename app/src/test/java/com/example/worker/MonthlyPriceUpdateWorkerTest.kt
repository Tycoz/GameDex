package com.example.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.PriceChartingData
import com.example.ai.PriceChartingScraper
import com.example.data.model.GameItem
import com.example.data.pricecharting.PriceChartingPreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MonthlyPriceUpdateWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun selectPriceForGame_selectsLoosePrice_whenGameIsLoose() {
        val game = GameItem(
            title = "Super Mario 64",
            platform = "Nintendo 64",
            completeness = "Loose"
        )
        val priceData = PriceChartingData(
            gameTitle = "Super Mario 64",
            consoleName = "Nintendo 64",
            productUrl = "https://www.pricecharting.com/game/nintendo-64/super-mario-64",
            loosePriceEur = 32.50,
            cibPriceEur = 85.00,
            newPriceEur = 250.00,
            gradedPriceEur = 900.00
        )

        val selected = MonthlyPriceUpdateWorker.selectPriceForGame(game, priceData)
        assertEquals(32.50, selected, 0.01)
    }

    @Test
    fun selectPriceForGame_selectsCibPrice_whenGameIsCib() {
        val game = GameItem(
            title = "The Legend of Zelda: Ocarina of Time",
            platform = "Nintendo 64",
            completeness = "CIB"
        )
        val priceData = PriceChartingData(
            gameTitle = "The Legend of Zelda: Ocarina of Time",
            consoleName = "Nintendo 64",
            productUrl = "https://www.pricecharting.com/game/nintendo-64/zelda-ocarina-of-time",
            loosePriceEur = 40.00,
            cibPriceEur = 95.00,
            newPriceEur = 320.00
        )

        val selected = MonthlyPriceUpdateWorker.selectPriceForGame(game, priceData)
        assertEquals(95.00, selected, 0.01)
    }

    @Test
    fun selectPriceForGame_selectsNewPrice_whenGameIsNeufOrScelle() {
        val gameNeuf = GameItem(
            title = "Metroid Prime",
            platform = "Nintendo GameCube",
            completeness = "Neuf"
        )
        val gameScelle = GameItem(
            title = "Metroid Prime 2",
            platform = "Nintendo GameCube",
            completeness = "Scellé"
        )
        val priceData = PriceChartingData(
            gameTitle = "Metroid Prime",
            consoleName = "Nintendo GameCube",
            productUrl = "https://www.pricecharting.com/game/gamecube/metroid-prime",
            loosePriceEur = 30.00,
            cibPriceEur = 60.00,
            newPriceEur = 180.00
        )

        val selectedNeuf = MonthlyPriceUpdateWorker.selectPriceForGame(gameNeuf, priceData)
        val selectedScelle = MonthlyPriceUpdateWorker.selectPriceForGame(gameScelle, priceData)

        assertEquals(180.00, selectedNeuf, 0.01)
        assertEquals(180.00, selectedScelle, 0.01)
    }

    @Test
    fun selectPriceForGame_selectsGradedPrice_whenGameIsGraded() {
        val game = GameItem(
            title = "Pokemon Rouge",
            platform = "Game Boy",
            completeness = "Gradé"
        )
        val priceData = PriceChartingData(
            gameTitle = "Pokemon Rouge",
            consoleName = "Game Boy",
            productUrl = "https://www.pricecharting.com/game/gameboy/pokemon-red",
            loosePriceEur = 60.00,
            cibPriceEur = 220.00,
            newPriceEur = 900.00,
            gradedPriceEur = 2500.00
        )

        val selected = MonthlyPriceUpdateWorker.selectPriceForGame(game, priceData)
        assertEquals(2500.00, selected, 0.01)
    }

    @Test
    fun priceChartingPreferenceManager_recordsAndRetrievesUpdatesProperly() {
        assertTrue("Monthly update should be enabled by default", PriceChartingPreferenceManager.isMonthlyUpdateEnabled(context))

        PriceChartingPreferenceManager.recordSuccessfulUpdate(
            context = context,
            gamesUpdated = 12,
            oldValueTotal = 450.0,
            newValueTotal = 520.0
        )

        assertEquals(12, PriceChartingPreferenceManager.getLastUpdatedGamesCount(context))
        assertEquals(70.0, PriceChartingPreferenceManager.getLastUpdateValueDifference(context), 0.01)

        val lastFormatted = PriceChartingPreferenceManager.getLastUpdateFormatted(context)
        assertTrue("Formatted date should not be 'Jamais'", lastFormatted != "Jamais")
    }

    @Test
    fun priceChartingScraper_slugMapping_handlesStandardAndSwitch2Consoles() {
        val n64Slug = PriceChartingScraper.mapPlatformToPriceChartingSlug("Nintendo 64")
        val switch2Slug = PriceChartingScraper.mapPlatformToPriceChartingSlug("Nintendo Switch 2")
        val snesSlug = PriceChartingScraper.mapPlatformToPriceChartingSlug("Super Nintendo")

        assertEquals("nintendo-64", n64Slug)
        assertEquals("nintendo-switch-2", switch2Slug)
        assertEquals("super-nintendo", snesSlug)
    }
}
