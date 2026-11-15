package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test


class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addGame_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore)

        val game = GameItem(
            userId = aliceUid,
            title = "Super Mario 64",
            platform = "Nintendo 64",
            genre = "Plateforme 3D",
            releaseYear = 1996,
            publisher = "Nintendo",
            format = "Cartouche",
            region = "PAL",
            barcode = "045496870027",
            isWishlist = false,
            boxCondition = "Très bon état",
            manualCondition = "Présent & Parfait",
            mediaCondition = "Parfait / Mint",
            completeness = "CIB",
            purchasePrice = 45.0,
            estimatedValue = 65.0,
            purchaseDate = "2024-01-15",
            rating = 5,
            status = "Terminé"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addGame(game)
        }
        assertTrue(result.isSuccess)
        val gameId = result.getOrThrow()
        assertTrue(gameId.isNotBlank())
    }

    @Test
    fun observeGames_authenticatedOwner_emitsGames() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore)

        val game = GameItem(
            userId = aliceUid,
            title = "The Legend of Zelda: Ocarina of Time",
            platform = "Nintendo 64",
            isWishlist = false,
            estimatedValue = 70.0
        )
        val gameId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addGame(game).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGames(isWishlist = false).first { list ->
                list.any { it.id == gameId }
            }
        }
        assertTrue(emitted.any { it.id == gameId })
    }

    @Test
    fun getGameById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GameRepository(firestore)

        val game = GameItem(
            userId = aliceUid,
            title = "Metal Gear Solid",
            platform = "PlayStation",
            isWishlist = false
        )
        val gameId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addGame(game).getOrThrow()
        }

        // Bob signs in and attempts to access Alice's game document
        signInTestUser(BOB_EMAIL)
        var permissionDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("games")
                    .document(gameId)
                    .get()
                    .await()
            }
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            if (firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                permissionDenied = true
            }
        }
        assertTrue("Expected PERMISSION_DENIED for cross-user access", permissionDenied)
    }


    @Test
    fun observeGames_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GameRepository(firestore)

        var failed = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeGames(isWishlist = false).first()
            }
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Expected failure for unauthenticated query", failed)
    }


    @Test
    fun toggleWishlist_movesGameBetweenWishlistAndCollection() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore)

        val game = GameItem(
            userId = aliceUid,
            title = "Chrono Trigger",
            platform = "Super Nintendo",
            isWishlist = true,
            estimatedValue = 250.0
        )
        val gameId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addGame(game).getOrThrow()
        }

        // Toggle from wishlist (true) to collection (false)
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleWishlist(gameId, currentIsWishlist = true)
        }
        assertTrue(updateResult.isSuccess)

        val updatedGame = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getGameById(gameId).getOrThrow()
        }
        assertEquals(false, updatedGame.isWishlist)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@gamedex.test"
        const val BOB_EMAIL = "bob@gamedex.test"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
