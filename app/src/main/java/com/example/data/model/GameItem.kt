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
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to title.trim(),
            "platform" to platform.trim(),
            "genre" to genre.trim(),
            "releaseYear" to releaseYear,
            "publisher" to publisher.trim(),
            "format" to format.trim(),
            "region" to region.trim(),
            "barcode" to barcode.trim(),
            "isWishlist" to isWishlist,
            "boxCondition" to boxCondition.trim(),
            "manualCondition" to manualCondition.trim(),
            "mediaCondition" to mediaCondition.trim(),
            "completeness" to completeness.trim(),
            "purchasePrice" to purchasePrice,
            "estimatedValue" to estimatedValue,
            "purchaseDate" to purchaseDate.trim(),
            "rating" to rating.coerceIn(0, 5),
            "status" to status.trim(),
            "notes" to notes.trim(),
            "coverUrl" to coverUrl.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "platform" to platform.trim(),
            "genre" to genre.trim(),
            "releaseYear" to releaseYear,
            "publisher" to publisher.trim(),
            "format" to format.trim(),
            "region" to region.trim(),
            "barcode" to barcode.trim(),
            "isWishlist" to isWishlist,
            "boxCondition" to boxCondition.trim(),
            "manualCondition" to manualCondition.trim(),
            "mediaCondition" to mediaCondition.trim(),
            "completeness" to completeness.trim(),
            "purchasePrice" to purchasePrice,
            "estimatedValue" to estimatedValue,
            "purchaseDate" to purchaseDate.trim(),
            "rating" to rating.coerceIn(0, 5),
            "status" to status.trim(),
            "notes" to notes.trim(),
            "coverUrl" to coverUrl.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map
    }
}
