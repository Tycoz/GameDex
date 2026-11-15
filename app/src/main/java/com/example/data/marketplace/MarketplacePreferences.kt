package com.example.data.marketplace

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class MarketplaceAccount(
    val platform: String, // "Vinted" or "eBay"
    val username: String = "",
    val domain: String = "fr", // e.g., "vinted.fr", "ebay.fr"
    val isConnected: Boolean = false,
    val filterCondition: String = "Très bon état", // For Vinted
    val buyItNowOnly: Boolean = true, // For eBay
    val lastSyncedTimestamp: Long = 0L
)

class MarketplacePreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("marketplace_accounts_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VINTED_USERNAME = "vinted_username"
        private const val KEY_VINTED_DOMAIN = "vinted_domain"
        private const val KEY_VINTED_CONNECTED = "vinted_connected"
        private const val KEY_VINTED_CONDITION = "vinted_condition"
        private const val KEY_VINTED_LAST_SYNC = "vinted_last_sync"

        private const val KEY_EBAY_USERNAME = "ebay_username"
        private const val KEY_EBAY_DOMAIN = "ebay_domain"
        private const val KEY_EBAY_CONNECTED = "ebay_connected"
        private const val KEY_EBAY_BIN_ONLY = "ebay_bin_only"
        private const val KEY_EBAY_LAST_SYNC = "ebay_last_sync"

        private const val KEY_DAILY_CHECK_ENABLED = "daily_check_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LAST_GLOBAL_CHECK = "last_global_check"

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun getVintedAccount(): MarketplaceAccount {
        return MarketplaceAccount(
            platform = "Vinted",
            username = prefs.getString(KEY_VINTED_USERNAME, "") ?: "",
            domain = prefs.getString(KEY_VINTED_DOMAIN, "vinted.fr") ?: "vinted.fr",
            isConnected = prefs.getBoolean(KEY_VINTED_CONNECTED, false),
            filterCondition = prefs.getString(KEY_VINTED_CONDITION, "Très bon état") ?: "Très bon état",
            lastSyncedTimestamp = prefs.getLong(KEY_VINTED_LAST_SYNC, 0L)
        )
    }

    fun saveVintedAccount(username: String, domain: String = "vinted.fr", condition: String = "Très bon état") {
        prefs.edit {
            putString(KEY_VINTED_USERNAME, username.trim())
            putString(KEY_VINTED_DOMAIN, domain.trim())
            putBoolean(KEY_VINTED_CONNECTED, username.isNotBlank())
            putString(KEY_VINTED_CONDITION, condition)
            putLong(KEY_VINTED_LAST_SYNC, System.currentTimeMillis())
        }
    }

    fun disconnectVinted() {
        prefs.edit {
            putBoolean(KEY_VINTED_CONNECTED, false)
        }
    }

    fun getEbayAccount(): MarketplaceAccount {
        return MarketplaceAccount(
            platform = "eBay",
            username = prefs.getString(KEY_EBAY_USERNAME, "") ?: "",
            domain = prefs.getString(KEY_EBAY_DOMAIN, "ebay.fr") ?: "ebay.fr",
            isConnected = prefs.getBoolean(KEY_EBAY_CONNECTED, false),
            buyItNowOnly = prefs.getBoolean(KEY_EBAY_BIN_ONLY, true),
            lastSyncedTimestamp = prefs.getLong(KEY_EBAY_LAST_SYNC, 0L)
        )
    }

    fun saveEbayAccount(username: String, domain: String = "ebay.fr", buyItNowOnly: Boolean = true) {
        prefs.edit {
            putString(KEY_EBAY_USERNAME, username.trim())
            putString(KEY_EBAY_DOMAIN, domain.trim())
            putBoolean(KEY_EBAY_CONNECTED, username.isNotBlank())
            putBoolean(KEY_EBAY_BIN_ONLY, buyItNowOnly)
            putLong(KEY_EBAY_LAST_SYNC, System.currentTimeMillis())
        }
    }

    fun disconnectEbay() {
        prefs.edit {
            putBoolean(KEY_EBAY_CONNECTED, false)
        }
    }

    fun isDailyCheckEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_CHECK_ENABLED, true)
    }

    fun setDailyCheckEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DAILY_CHECK_ENABLED, enabled) }
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun getLastGlobalCheckTimestamp(): Long {
        return prefs.getLong(KEY_LAST_GLOBAL_CHECK, 0L)
    }

    fun setLastGlobalCheckTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY_LAST_GLOBAL_CHECK, timestamp) }
    }

    val connectedAccountsCount: Int
        get() {
            var count = 0
            if (getVintedAccount().isConnected) count++
            if (getEbayAccount().isConnected) count++
            return count
        }

    /**
     * Tests connectivity to Vinted endpoint.
     */
    suspend fun testVintedConnection(domain: String = "vinted.fr"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.$domain"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..399) {
                    Result.success("Connexion à Vinted ($domain) réussie.")
                } else {
                    Result.success("Service Vinted accessible (code ${response.code}).")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tests connectivity to eBay endpoint.
     */
    suspend fun testEbayConnection(domain: String = "ebay.fr"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.$domain"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..399) {
                    Result.success("Connexion à eBay ($domain) réussie.")
                } else {
                    Result.success("Service eBay accessible (code ${response.code}).")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
