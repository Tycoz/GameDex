package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.marketplace.MarketplacePreferencesManager
import com.example.data.marketplace.MarketplacePriceChecker
import com.example.data.model.isPriceAlertEnabled
import com.example.data.model.maxTargetPrice
import com.example.data.model.priceAlert
import com.example.data.model.withPriceAlert
import com.example.data.repository.GameRepository
import com.example.notification.WishlistNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class WishlistPriceCheckerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "WishlistPriceWorker"
        const val UNIQUE_PERIODIC_WORK_NAME = "wishlist_daily_price_checker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic wishlist price check...")
        val prefs = MarketplacePreferencesManager(applicationContext)

        if (!prefs.isDailyCheckEnabled()) {
            Log.d(TAG, "Daily check disabled in preferences. Skipping.")
            return Result.success()
        }

        if (Firebase.auth.currentUser == null) {
            Log.d(TAG, "No authenticated user. Skipping wishlist price check.")
            return Result.success()
        }

        return try {
            val repository = GameRepository(applicationContext)
            val wishlistGames = repository.observeGames(isWishlist = true).first()

            val gamesToWatch = wishlistGames.filter { it.isPriceAlertEnabled && it.maxTargetPrice > 0.0 }
            Log.d(TAG, "Found ${gamesToWatch.size} wishlist games with active price alerts.")

            var notificationIndex = 1
            var dealsFoundCount = 0

            for (game in gamesToWatch) {
                val deal = MarketplacePriceChecker.checkGameDeals(game, prefs)
                if (deal != null && deal.price <= game.maxTargetPrice) {
                    dealsFoundCount++
                    Log.d(TAG, "Deal found for '${game.title}': ${deal.price}€ on ${deal.platform}")

                    // Update game with latest detected deal
                    val updatedGame = game.withPriceAlert(
                        game.priceAlert.copy(
                            deal = deal,
                            lastChecked = System.currentTimeMillis()
                        )
                    )
                    val updateRes = repository.updateGame(updatedGame)
                    if (updateRes.isFailure) {
                        Log.w(TAG, "Failed to persist deal for '${game.title}'", updateRes.exceptionOrNull())
                    }

                    // Post push notification
                    if (prefs.areNotificationsEnabled()) {
                        val notifId = (game.id.hashCode() + notificationIndex) and 0x7FFFFFFF
                        WishlistNotificationHelper.sendDealNotification(
                            context = applicationContext,
                            notificationId = notifId,
                            gameTitle = game.title,
                            platformName = game.platform,
                            dealPrice = deal.price,
                            maxPrice = game.maxTargetPrice,
                            dealUrl = deal.url,
                            marketplace = deal.platform
                        )
                        notificationIndex++
                    }
                }
            }

            prefs.setLastGlobalCheckTimestamp()
            Log.d(TAG, "Wishlist check complete: $dealsFoundCount deals detected.")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in WishlistPriceCheckerWorker", e)
            Result.retry()
        }
    }
}
