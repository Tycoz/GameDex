package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GameItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val platform: String = "",
    val genre: String = "",
    val releaseYear: Int = 2000,
    val publisher: String = "",
    val format: String = "Cartouche", // Cartouche, Disque, Coffret Collector, Dématérialisé
    val region: String = "PAL", // PAL, NTSC-U, NTSC-J, Region Free
    val barcode: String = "",
    val isWishlist: Boolean = false,
    val boxCondition: String = "Très bon état", // Neuf sous blister, Comme neuf, Très bon état, Bon état, Moyen, Mauvais, Sans boîte
    val manualCondition: String = "Présent & Parfait", // Présent & Parfait, Très bon, Usé, Sans notice
    val mediaCondition: String = "Parfait / Mint", // Parfait / Mint, Micro-rayures légères, Rayé mais lisible, HS
    val completeness: String = "CIB", // CIB (Complet en boîte), Boîte sans notice, Loose, Neuf scellé
    val purchasePrice: Double = 0.0,
    val estimatedValue: Double = 0.0,
    val purchaseDate: String = "",
    val rating: Int = 0, // 0..5
    val status: String = "Non commencé", // Non commencé, En cours, Terminé, 100%
    val notes: String = "",
    val coverUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(userId: String): Map<String, Any> {
        val sanitized = sanitizedForFirestore()
        return mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to sanitized.title,
            "platform" to sanitized.platform,
            "genre" to sanitized.genre,
            "releaseYear" to sanitized.releaseYear,
            "publisher" to sanitized.publisher,
            "format" to sanitized.format,
            "region" to sanitized.region,
            "barcode" to sanitized.barcode,
            "isWishlist" to sanitized.isWishlist,
            "boxCondition" to sanitized.boxCondition,
            "manualCondition" to sanitized.manualCondition,
            "mediaCondition" to sanitized.mediaCondition,
            "completeness" to sanitized.completeness,
            "purchasePrice" to sanitized.purchasePrice,
            "estimatedValue" to sanitized.estimatedValue,
            "purchaseDate" to sanitized.purchaseDate,
            "rating" to sanitized.rating,
            "status" to sanitized.status,
            "notes" to sanitized.notes,
            "coverUrl" to sanitized.coverUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        val sanitized = sanitizedForFirestore()
        return mutableMapOf<String, Any>(
            "title" to sanitized.title,
            "platform" to sanitized.platform,
            "genre" to sanitized.genre,
            "releaseYear" to sanitized.releaseYear,
            "publisher" to sanitized.publisher,
            "format" to sanitized.format,
            "region" to sanitized.region,
            "barcode" to sanitized.barcode,
            "isWishlist" to sanitized.isWishlist,
            "boxCondition" to sanitized.boxCondition,
            "manualCondition" to sanitized.manualCondition,
            "mediaCondition" to sanitized.mediaCondition,
            "completeness" to sanitized.completeness,
            "purchasePrice" to sanitized.purchasePrice,
            "estimatedValue" to sanitized.estimatedValue,
            "purchaseDate" to sanitized.purchaseDate,
            "rating" to sanitized.rating,
            "status" to sanitized.status,
            "notes" to sanitized.notes,
            "coverUrl" to sanitized.coverUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun sanitizedForFirestore(): GameItem {
        val safePurchase = if (purchasePrice.isFinite()) purchasePrice.coerceAtLeast(0.0) else 0.0
        val safeEstimated = if (estimatedValue.isFinite()) estimatedValue.coerceAtLeast(0.0) else 0.0
        return copy(
            title = title.trim().ifBlank { "Jeu sans titre" }.take(MAX_TITLE_LENGTH),
            platform = platform.trim().ifBlank { "Autre" }.take(MAX_PLATFORM_LENGTH),
            genre = genre.trim().take(MAX_SHORT_TEXT_LENGTH),
            publisher = publisher.trim().take(MAX_PUBLISHER_LENGTH),
            format = format.trim().take(MAX_SHORT_TEXT_LENGTH),
            region = region.trim().take(MAX_TINY_TEXT_LENGTH),
            barcode = barcode.trim().take(MAX_SHORT_TEXT_LENGTH),
            boxCondition = boxCondition.trim().take(MAX_SHORT_TEXT_LENGTH),
            manualCondition = manualCondition.trim().take(MAX_SHORT_TEXT_LENGTH),
            mediaCondition = mediaCondition.trim().take(MAX_SHORT_TEXT_LENGTH),
            completeness = completeness.trim().take(MAX_SHORT_TEXT_LENGTH),
            purchasePrice = safePurchase,
            estimatedValue = safeEstimated,
            purchaseDate = purchaseDate.trim().take(MAX_TINY_TEXT_LENGTH),
            rating = rating.coerceIn(0, 5),
            status = status.trim().take(MAX_TINY_TEXT_LENGTH),
            notes = notes.trim().take(MAX_NOTES_LENGTH),
            coverUrl = coverUrl.trim().take(MAX_COVER_URL_LENGTH)
        )
    }

    companion object {
        const val MAX_TITLE_LENGTH = 150
        const val MAX_PLATFORM_LENGTH = 50
        const val MAX_PUBLISHER_LENGTH = 100
        const val MAX_SHORT_TEXT_LENGTH = 50
        const val MAX_TINY_TEXT_LENGTH = 30
        const val MAX_NOTES_LENGTH = 1000
        const val MAX_COVER_URL_LENGTH = 500
    }
}
