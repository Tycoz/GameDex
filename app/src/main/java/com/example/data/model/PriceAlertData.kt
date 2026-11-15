package com.example.data.model

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Information about a deal found on Vinted or eBay below user's target price.
 */
data class DealFinding(
    val price: Double,
    val platform: String, // "Vinted" or "eBay"
    val url: String,
    val title: String = "",
    val foundAt: Long = System.currentTimeMillis()
)

/**
 * Price alert configuration associated with a wishlist item.
 */
data class PriceAlert(
    val maxTargetPrice: Double = 0.0,
    val isEnabled: Boolean = false,
    val deal: DealFinding? = null,
    val lastChecked: Long = 0L
)

object PriceAlertSerializer {
    private const val PREFIX = "<!--ALERT_CFG:"
    private const val SUFFIX = "-->"

    fun serialize(alert: PriceAlert): String {
        if (alert.maxTargetPrice <= 0.0 && !alert.isEnabled && alert.deal == null) {
            return ""
        }
        val safeTitle = try { URLEncoder.encode(alert.deal?.title ?: "", "UTF-8") } catch (e: Exception) { "" }
        val safeUrl = try { URLEncoder.encode(alert.deal?.url ?: "", "UTF-8") } catch (e: Exception) { "" }
        val platform = alert.deal?.platform ?: ""
        val dealPrice = alert.deal?.price ?: 0.0
        val foundAt = alert.deal?.foundAt ?: 0L

        return "$PREFIX" +
                "max=${alert.maxTargetPrice};" +
                "enabled=${alert.isEnabled};" +
                "dealPrice=$dealPrice;" +
                "dealPlatform=$platform;" +
                "dealUrl=$safeUrl;" +
                "dealTitle=$safeTitle;" +
                "foundAt=$foundAt;" +
                "lastChecked=${alert.lastChecked}" +
                "$SUFFIX"
    }

    fun deserialize(notes: String): PriceAlert {
        val start = notes.indexOf(PREFIX)
        if (start == -1) return PriceAlert()
        val end = notes.indexOf(SUFFIX, start + PREFIX.length)
        if (end == -1) return PriceAlert()

        val content = notes.substring(start + PREFIX.length, end)
        val pairs = content.split(";").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }.toMap()

        val maxPrice = pairs["max"]?.toDoubleOrNull() ?: 0.0
        val isEnabled = pairs["enabled"]?.toBooleanStrictOrNull() ?: false
        val lastChecked = pairs["lastChecked"]?.toLongOrNull() ?: 0L

        val dealPrice = pairs["dealPrice"]?.toDoubleOrNull() ?: 0.0
        val dealPlatform = pairs["dealPlatform"] ?: ""
        val dealUrlRaw = pairs["dealUrl"] ?: ""
        val dealTitleRaw = pairs["dealTitle"] ?: ""
        val foundAt = pairs["foundAt"]?.toLongOrNull() ?: 0L

        val deal = if (dealPrice > 0.0 && dealPlatform.isNotBlank()) {
            val decodedUrl = try { URLDecoder.decode(dealUrlRaw, "UTF-8") } catch (e: Exception) { dealUrlRaw }
            val decodedTitle = try { URLDecoder.decode(dealTitleRaw, "UTF-8") } catch (e: Exception) { dealTitleRaw }
            DealFinding(
                price = dealPrice,
                platform = dealPlatform,
                url = decodedUrl,
                title = decodedTitle,
                foundAt = foundAt
            )
        } else {
            null
        }

        return PriceAlert(
            maxTargetPrice = maxPrice,
            isEnabled = isEnabled,
            deal = deal,
            lastChecked = lastChecked
        )
    }

    fun stripConfigFromNotes(notes: String): String {
        val start = notes.indexOf(PREFIX)
        if (start == -1) return notes.trim()
        val end = notes.indexOf(SUFFIX, start + PREFIX.length)
        if (end == -1) return notes.trim()

        val before = notes.substring(0, start)
        val after = notes.substring(end + SUFFIX.length)
        return (before + after).trim()
    }

    fun applyToNotes(existingNotes: String, alert: PriceAlert): String {
        val clean = stripConfigFromNotes(existingNotes)
        val serialized = serialize(alert)
        if (serialized.isBlank()) {
            return clean.take(GameItem.MAX_NOTES_LENGTH)
        }
        if (clean.isBlank()) {
            return serialized.take(GameItem.MAX_NOTES_LENGTH)
        }
        val availableCleanLength = (GameItem.MAX_NOTES_LENGTH - serialized.length - 1).coerceAtLeast(0)
        val boundedClean = clean.take(availableCleanLength).trimEnd()
        return if (boundedClean.isBlank()) {
            serialized.take(GameItem.MAX_NOTES_LENGTH)
        } else {
            "$boundedClean\n$serialized"
        }
    }
}

val GameItem.priceAlert: PriceAlert
    get() = PriceAlertSerializer.deserialize(this.notes)

val GameItem.maxTargetPrice: Double
    get() = priceAlert.maxTargetPrice

val GameItem.isPriceAlertEnabled: Boolean
    get() = isWishlist && priceAlert.isEnabled && priceAlert.maxTargetPrice > 0.0

val GameItem.detectedDeal: DealFinding?
    get() = priceAlert.deal

val GameItem.cleanNotes: String
    get() = PriceAlertSerializer.stripConfigFromNotes(this.notes)

fun GameItem.withPriceAlert(alert: PriceAlert): GameItem {
    val updatedNotes = PriceAlertSerializer.applyToNotes(this.notes, alert)
    return this.copy(notes = updatedNotes)
}

fun GameItem.withPriceAlert(
    maxTargetPrice: Double,
    isEnabled: Boolean,
    deal: DealFinding? = null,
    lastChecked: Long = System.currentTimeMillis()
): GameItem {
    val newAlert = PriceAlert(
        maxTargetPrice = maxTargetPrice,
        isEnabled = isEnabled,
        deal = deal,
        lastChecked = lastChecked
    )
    return withPriceAlert(newAlert)
}
