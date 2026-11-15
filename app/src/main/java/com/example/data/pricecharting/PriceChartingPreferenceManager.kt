package com.example.data.pricecharting

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PriceChartingPreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "pricecharting_monthly_prefs"
        private const val KEY_MONTHLY_UPDATE_ENABLED = "monthly_update_enabled"
        private const val KEY_LAST_UPDATE_TIMESTAMP = "last_monthly_update_ts"
        private const val KEY_LAST_GAMES_COUNT = "last_games_updated_count"
        private const val KEY_LAST_TOTAL_VALUE = "last_total_value_eur"
        private const val KEY_LAST_VALUE_DIFF = "last_value_diff_eur"

        private val DATE_FORMATTER = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        }

        private fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun isMonthlyUpdateEnabled(context: Context): Boolean =
            prefs(context).getBoolean(KEY_MONTHLY_UPDATE_ENABLED, true)

        fun setMonthlyUpdateEnabled(context: Context, enabled: Boolean) =
            prefs(context).edit { putBoolean(KEY_MONTHLY_UPDATE_ENABLED, enabled) }

        fun getLastUpdatedGamesCount(context: Context): Int =
            prefs(context).getInt(KEY_LAST_GAMES_COUNT, 0)

        fun getLastUpdateValueDifference(context: Context): Double =
            prefs(context).getFloat(KEY_LAST_VALUE_DIFF, 0f).toDouble()

        fun getLastUpdateFormatted(context: Context): String {
            val ts = prefs(context).getLong(KEY_LAST_UPDATE_TIMESTAMP, 0L)
            if (ts <= 0L) return "Aucune actualisation encore"
            val formatter = DATE_FORMATTER.get()
                ?: SimpleDateFormat("d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
            return formatter.format(Date(ts))
        }

        fun recordSuccessfulUpdate(
            context: Context,
            gamesUpdated: Int,
            oldValueTotal: Double,
            newValueTotal: Double
        ) {
            val diff = newValueTotal - oldValueTotal
            prefs(context).edit {
                putLong(KEY_LAST_UPDATE_TIMESTAMP, System.currentTimeMillis())
                putInt(KEY_LAST_GAMES_COUNT, gamesUpdated)
                putFloat(KEY_LAST_TOTAL_VALUE, newValueTotal.toFloat())
                putFloat(KEY_LAST_VALUE_DIFF, diff.toFloat())
            }
        }
    }

    fun isMonthlyUpdateEnabled(): Boolean {
        return prefs.getBoolean(KEY_MONTHLY_UPDATE_ENABLED, true)
    }

    fun setMonthlyUpdateEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MONTHLY_UPDATE_ENABLED, enabled) }
    }

    fun getLastUpdateTimestamp(): Long {
        return prefs.getLong(KEY_LAST_UPDATE_TIMESTAMP, 0L)
    }

    fun getLastGamesUpdatedCount(): Int {
        return prefs.getInt(KEY_LAST_GAMES_COUNT, 0)
    }

    fun getLastTotalValuationEur(): Double {
        return prefs.getFloat(KEY_LAST_TOTAL_VALUE, 0f).toDouble()
    }

    fun getLastValuationDifferenceEur(): Double {
        return prefs.getFloat(KEY_LAST_VALUE_DIFF, 0f).toDouble()
    }

    fun recordSuccessfulUpdate(gamesUpdatedCount: Int, totalValuationEur: Double, valueDiffEur: Double = 0.0) {
        prefs.edit {
            putLong(KEY_LAST_UPDATE_TIMESTAMP, System.currentTimeMillis())
            putInt(KEY_LAST_GAMES_COUNT, gamesUpdatedCount)
            putFloat(KEY_LAST_TOTAL_VALUE, totalValuationEur.toFloat())
            putFloat(KEY_LAST_VALUE_DIFF, valueDiffEur.toFloat())
        }
    }

    fun getFormattedLastUpdate(): String {
        val ts = getLastUpdateTimestamp()
        if (ts <= 0L) return "Aucune actualisation encore"
        val formatter = DATE_FORMATTER.get()
            ?: SimpleDateFormat("d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        return formatter.format(Date(ts))
    }
}
