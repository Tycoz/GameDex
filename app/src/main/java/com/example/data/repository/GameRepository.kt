package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.GameItem
import com.example.data.model.OperationType
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GameRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google pour accéder à la collection.")
    }

    /**
     * Observe games for the authenticated user filtered by collection or wishlist.
     */
    fun observeGames(isWishlist: Boolean): Flow<List<GameItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/games"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("games")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isWishlist", isWishlist)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(GameItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    /**
     * Observe ALL games (both collection & wishlist) for statistics & totals.
     */
    fun observeAllGames(): Flow<List<GameItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/games"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("games")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(GameItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    /**
     * Get a specific game document by ID.
     */
    suspend fun getGameById(gameId: String): Result<GameItem> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("games").document(gameId)
            try {
                val snapshot = docRef.get().await()
                val game = snapshot.toObject(GameItem::class.java)
                if (game != null) {
                    Result.success(game)
                } else {
                    Result.failure(NoSuchElementException("Jeu introuvable."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, docRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a new game to collection or wishlist.
     */
    suspend fun addGame(game: GameItem): Result<String> {
        return try {
            val uid = requireUserId()
            val collectionRef = db.collection("users").document(uid).collection("games")
            val payload = game.toCreateMap(uid)
            try {
                val docRef = collectionRef.add(payload).await()
                Result.success(docRef.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, collectionRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch import multiple games into the user's Firestore collection.
     * Divides operations into chunks of 400 to respect Firestore's 500-write limit per batch.
     */
    suspend fun importGamesBatch(games: List<GameItem>): Result<Int> {
        return try {
            val uid = requireUserId()
            val collectionRef = db.collection("users").document(uid).collection("games")
            try {
                val chunks = games.chunked(400)
                var totalCount = 0
                for (chunk in chunks) {
                    val batch = db.batch()
                    for (game in chunk) {
                        val docRef = collectionRef.document()
                        val payload = game.toCreateMap(uid)
                        batch.set(docRef, payload)
                        totalCount++
                    }
                    batch.commit().await()
                }
                Result.success(totalCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, collectionRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing game item.
     */
    suspend fun updateGame(game: GameItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(game.id.isNotBlank()) { "Game ID must not be blank for update" }
            val docRef = db.collection("users").document(uid).collection("games").document(game.id)
            val payload = game.toUpdateMap()
            try {
                docRef.update(payload).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch update the platform/console for a list of game IDs.
     */
    suspend fun updatePlatformForGames(gameIds: List<String>, newPlatform: String): Result<Int> {
        return try {
            val uid = requireUserId()
            val cleanIds = gameIds.filter { it.isNotBlank() }.distinct()
            if (cleanIds.isEmpty()) return Result.success(0)
            val sanitizedPlatform = newPlatform.trim()
            require(sanitizedPlatform.isNotBlank()) { "Platform name must not be blank" }

            val collectionRef = db.collection("users").document(uid).collection("games")
            try {
                val chunks = cleanIds.chunked(400)
                var totalCount = 0
                for (chunk in chunks) {
                    val batch = db.batch()
                    for (id in chunk) {
                        val docRef = collectionRef.document(id)
                        batch.update(
                            docRef,
                            mapOf(
                                "platform" to sanitizedPlatform,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        totalCount++
                    }
                    batch.commit().await()
                }
                Result.success(totalCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, collectionRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a game from collection or wishlist.
     */
    suspend fun deleteGame(gameId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(gameId.isNotBlank()) { "Game ID must not be blank for deletion" }
            val docRef = db.collection("users").document(uid).collection("games").document(gameId)
            try {
                docRef.delete().await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Move game between Collection and Wishlist.
     */
    suspend fun toggleWishlist(gameId: String, currentIsWishlist: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(gameId.isNotBlank()) { "Game ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("games").document(gameId)
            try {
                docRef.update(
                    mapOf(
                        "isWishlist" to !currentIsWishlist,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
