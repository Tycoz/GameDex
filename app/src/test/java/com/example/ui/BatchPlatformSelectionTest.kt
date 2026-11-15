package com.example.ui

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GameItem
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.GameRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatchPlatformSelectionTest : FirestoreEmulatorTestBase() {

    private lateinit var repository: GameRepository
    private lateinit var viewModel: GameDexViewModel

    @Before
    fun setUp() {
        setUpFirebase()
        repository = GameRepository(firestore)
        viewModel = GameDexViewModel(repository)
    }

    @Test
    fun selectionMode_toggleAndSelection_updatesStateFlows() {
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedGameIds.value.isEmpty())

        // Toggle selection for game 1
        viewModel.toggleGameSelection("game_1")
        assertTrue("Selecting a game should automatically activate selection mode", viewModel.isSelectionMode.value)
        assertTrue("Game 1 should be in selected IDs", viewModel.selectedGameIds.value.contains("game_1"))
        assertEquals(1, viewModel.selectedGameIds.value.size)

        // Toggle selection for game 2
        viewModel.toggleGameSelection("game_2")
        assertEquals(2, viewModel.selectedGameIds.value.size)
        assertTrue(viewModel.selectedGameIds.value.contains("game_2"))

        // Unselect game 1
        viewModel.toggleGameSelection("game_1")
        assertEquals(1, viewModel.selectedGameIds.value.size)
        assertFalse(viewModel.selectedGameIds.value.contains("game_1"))
        assertTrue(viewModel.selectedGameIds.value.contains("game_2"))

        // Select all games
        val gameList = listOf("game_1", "game_2", "game_3", "game_4")
        viewModel.selectAllGames(gameList)
        assertEquals(4, viewModel.selectedGameIds.value.size)

        // Clear selection
        viewModel.clearSelection()
        assertTrue(viewModel.selectedGameIds.value.isEmpty())

        // Turning off selection mode clears selection
        viewModel.toggleGameSelection("game_1")
        assertTrue(viewModel.isSelectionMode.value)
        viewModel.setSelectionMode(false)
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedGameIds.value.isEmpty())
    }

    @Test
    fun tabChange_clearsSelectionMode() {
        viewModel.toggleGameSelection("game_100")
        assertTrue(viewModel.isSelectionMode.value)

        // Switch tab to Wishlist (tab 1)
        viewModel.setTab(1)
        assertFalse("Changing tabs should exit selection mode", viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedGameIds.value.isEmpty())
    }
}
