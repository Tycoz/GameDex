package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ai.PriceChartingData
import com.example.ai.PriceChartingScraper
import com.example.data.model.GameItem
import com.example.data.pricecharting.PriceChartingPreferenceManager
import com.example.data.repository.GameRepository
import com.example.notification.PriceChartingNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class MonthlyPriceUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "MonthlyPriceWorker"
        const val UNIQUE_PERIODIC_WORK_NAME = "monthly_pricecharting_update"

        /**
         * Determines the appropriate PriceCharting price tier based on the game's completeness.
         */
        fun selectPriceForGame(game: GameItem, priceData: PriceChartingData): Double {
            val comp = game.completeness.lowercase().trim()
            val candidate = when {
                comp.contains("grad") || comp.contains("wata") || comp.contains("vga") -> {
                    priceData.gradedPriceEur ?: priceData.newPriceEur ?: priceData.cibPriceEur ?: priceData.estimatedValueEur
                }
                comp.contains("loose") || comp.contains("sans boîte") -> {
                    priceData.loosePriceEur ?: priceData.cibPriceEur ?: priceData.estimatedValueEur
                }
                comp.contains("neuf") || comp.contains("scellé") || comp.contains("blister") -> {
                    priceData.newPriceEur ?: priceData.cibPriceEur ?: priceData.estimatedValueEur
                }
                else -> {
                    // CIB (Complet en boîte) or default
                    priceData.cibPriceEur ?: priceData.loosePriceEur ?: priceData.estimatedValueEur
                }
            }
            return if (candidate > 0.0) candidate else game.estimatedValue
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting monthly PriceCharting collection value update...")
        val prefs = PriceChartingPreferenceManager(applicationContext)

        if (!prefs.isMonthlyUpdateEnabled()) {
            Log.d(TAG, "Monthly PriceCharting update is disabled in preferences. Skipping.")
            return Result.success()
        }

        if (Firebase.auth.currentUser == null) {
            Log.d(TAG, "No authenticated user. Skipping monthly PriceCharting update.")
            return Result.success()
        }

        return try {
            val repository = GameRepository(applicationContext)
            val collectionGames = repository.observeGames(isWishlist = false).first()

            if (collectionGames.isEmpty()) {
                Log.d(TAG, "Collection is empty. Nothing to update.")
                prefs.recordSuccessfulUpdate(0, 0.0, 0.0)
                return Result.success()
            }

            Log.d(TAG, "Updating prices for ${collectionGames.size} collection games from PriceCharting...")
            var updatedCount = 0
            var totalValueDiff = 0.0
            var newTotalCollectionValue = 0.0

            for (game in collectionGames) {
                try {
                    val priceData = PriceChartingScraper.scrapePrice(game.title, game.platform)
                    if (priceData != null) {
                        val newPrice = selectPriceForGame(game, priceData)

                        // Update game if price changed by at least 0.10 €
                        val diff = newPrice - game.estimatedValue
                        if (abs(diff) >= 0.10) {
                            val updatedGame = game.copy(estimatedValue = newPrice)
                            val updateResult = repository.updateGame(updatedGame)
                            if (updateResult.isSuccess) {
                                updatedCount++
                                totalValueDiff += diff
                                newTotalCollectionValue += newPrice
                                Log.d(TAG, "Updated '${game.title}': ${game.estimatedValue}€ -> ${newPrice}€ (diff: $diff€)")
                            } else {
                                Log.w(TAG, "Failed to persist updated price for '${game.title}'", updateResult.exceptionOrNull())
                                newTotalCollectionValue += game.estimatedValue
                            }
                        } else {
                            newTotalCollectionValue += newPrice
                        }
                    } else {
                        newTotalCollectionValue += game.estimatedValue
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update price for '${game.title}'", e)
                    newTotalCollectionValue += game.estimatedValue
                }
            }

            prefs.recordSuccessfulUpdate(
                gamesUpdatedCount = updatedCount,
                totalValuationEur = newTotalCollectionValue,
                valueDiffEur = totalValueDiff
            )

            Log.d(TAG, "Monthly PriceCharting update complete: $updatedCount games updated. Total value: $newTotalCollectionValue €")

            // Send push notification if games were updated
            if (updatedCount > 0) {
                PriceChartingNotificationHelper.sendMonthlyUpdateNotification(
                    context = applicationContext,
                    gamesUpdatedCount = updatedCount,
                    totalCollectionValueEur = newTotalCollectionValue,
                    diffEur = totalValueDiff
                )
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in MonthlyPriceUpdateWorker", e)
            Result.retry()
        }
    }
}
