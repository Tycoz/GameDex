package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WishlistWorkScheduler {

    private const val PERIODIC_WORK_TAG = "wishlist_daily_price_checker"
    private const val IMMEDIATE_WORK_TAG = "wishlist_immediate_price_checker"

    /**
     * Schedules a periodic background check once every 24 hours.
     * Requires network connection.
     */
    fun scheduleDailyPriceCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<WishlistPriceCheckerWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS // 1h flex interval
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WishlistPriceCheckerWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    /**
     * Triggers an immediate one-time check for testing or user-initiated scan.
     */
    fun triggerImmediatePriceCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<WishlistPriceCheckerWorker>()
            .setConstraints(constraints)
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun cancelDailyPriceCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WishlistPriceCheckerWorker.UNIQUE_PERIODIC_WORK_NAME)
    }
}
