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

object MonthlyPriceUpdateScheduler {

    private const val PERIODIC_WORK_TAG = "pricecharting_monthly_updater"
    private const val IMMEDIATE_WORK_TAG = "pricecharting_immediate_updater"

    /**
     * Schedules periodic collection price updates once a month (30 days interval).
     * Runs when network is connected and battery is not low.
     */
    fun scheduleMonthlyPriceUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val monthlyWorkRequest = PeriodicWorkRequestBuilder<MonthlyPriceUpdateWorker>(
            30, TimeUnit.DAYS,
            24, TimeUnit.HOURS // 24h flex interval
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MonthlyPriceUpdateWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            monthlyWorkRequest
        )
    }

    /**
     * Triggers an immediate one-time price update for testing or user-initiated refresh.
     */
    fun triggerImmediatePriceUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<MonthlyPriceUpdateWorker>()
            .setConstraints(constraints)
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun cancelMonthlyPriceUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MonthlyPriceUpdateWorker.UNIQUE_PERIODIC_WORK_NAME)
    }
}
