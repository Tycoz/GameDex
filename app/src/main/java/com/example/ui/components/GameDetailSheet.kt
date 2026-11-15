package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.ai.GameScraperService
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import com.example.data.model.cleanNotes
import com.example.data.model.detectedDeal
import com.example.data.model.isPriceAlertEnabled
import com.example.data.model.maxTargetPrice
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GameItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(
    game: GameItem,
    onDismiss: () -> Unit,
    onEdit: (GameItem) -> Unit,
    onToggleWishlist: (GameItem) -> Unit,
    onDelete: (String) -> Unit,
    onUpdatePriceCharting: (GameItem, () -> Unit) -> Unit = { _, done -> done() }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isScraping by remember { mutableStateOf(false) }
    var isUpdatingPrice by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val platformColor = remember(game.platform) { getPlatformColor(game.platform) }
    val completenessColor = remember(game.completeness) { getCompletenessColor(game.completeness) }
    val coverRequest = remember(context, game.coverUrl) {
        if (game.coverUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(game.coverUrl)
                .crossfade(true)
                .build()
        } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("game_detail_sheet")
        ) {
            // Top Bar: Close and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = platformColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = game.platform,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = platformColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (isScraping) return@IconButton
                            coroutineScope.launch {
                                isScraping = true
                                try {
                                    val result = GameScraperService.scrapeGame(game.title, game.platform)
                                    result.onSuccess { scraped ->
                                        val updated = game.copy(
                                            coverUrl = if (scraped.coverUrl.isNotBlank()) scraped.coverUrl else game.coverUrl,
                                            publisher = if (game.publisher.isBlank() || game.publisher == "Inconnu") scraped.publisher else game.publisher,
                                            releaseYear = if (game.releaseYear == 2000 && scraped.releaseYear > 0) scraped.releaseYear else game.releaseYear,
                                            genre = if (game.genre.isBlank()) scraped.genre else game.genre,
                                            estimatedValue = if (game.estimatedValue == 0.0 || scraped.estimatedValue > 0.0) scraped.estimatedValue else game.estimatedValue,
                                            notes = if (game.notes.isBlank()) scraped.description else game.notes
                                        )
                                        onEdit(updated)
                                        Toast.makeText(context, "Données, cote PriceCharting et jaquette scrappées !", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Échec : ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isScraping = false
                                }
                            }
                        },
                        modifier = Modifier.testTag("detail_scrape_button")
                    ) {
                        if (isScraping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = BrandPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = "Scrapper les infos et la jaquette",
                                tint = BrandPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = { onEdit(game) },
                        modifier = Modifier.testTag("edit_game_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = BrandPrimary)
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_game_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Release Card Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 118.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(platformColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverRequest != null) {
                        AsyncImage(
                            model = coverRequest,
                            contentDescription = game.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = platformColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = listOfNotNull(
                            game.publisher.ifBlank { null },
                            if (game.releaseYear > 0) game.releaseYear.toString() else null,
                            game.region.ifBlank { null }
                        ).joinToString(" • "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = completenessColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = game.completeness,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = completenessColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (game.format.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = game.format,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (game.rating > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { starIndex ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (starIndex < game.rating) AmberGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Discogs-like Physical Grading Section
            Text(
                text = "État & Gradation Physique (Grading)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    GradingRow(label = "Boîte / Boîtier", value = game.boxCondition)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    GradingRow(label = "Notice / Livret", value = game.manualCondition)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    GradingRow(label = "Cartouche / Disque", value = game.mediaCondition)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Financial & Valuation Section
            Text(
                text = "Données Financières & Historique",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cote Marchande", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (game.estimatedValue > 0) String.format(Locale.FRANCE, "%.2f €", game.estimatedValue) else "Non coté",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldMint
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Prix d'acquisition", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (game.purchasePrice > 0) String.format(Locale.FRANCE, "%.2f €", game.purchasePrice) else "Non renseigné",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PriceCharting Manual Valuation Request Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_pricecharting_valuation_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BrandPrimary.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cote PriceCharting.com",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }

                        Surface(
                            color = BrandPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "État : ${game.completeness}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Calculé à partir des transactions réelles sur https://www.pricecharting.com/ selon la complétude (${game.completeness}) et la console (${game.platform}).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (!isUpdatingPrice) {
                                isUpdatingPrice = true
                                onUpdatePriceCharting(game) {
                                    isUpdatingPrice = false
                                }
                            }
                        },
                        enabled = !isUpdatingPrice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_manual_pricecharting_update"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isUpdatingPrice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Interrogation de PriceCharting...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Actualiser la valeur (PriceCharting)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (game.isWishlist) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_price_alert_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberGold.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Surveillance Prix Vinted & eBay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (game.isPriceAlertEnabled) "Alerte active • Vérification 1x par jour" else "Surveillance désactivée",
                                        fontSize = 11.sp,
                                        color = if (game.isPriceAlertEnabled) EmeraldMint else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (game.isPriceAlertEnabled) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            if (game.maxTargetPrice > 0.0) {
                                Surface(
                                    color = AmberGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🎯 Max : ${String.format(Locale.FRANCE, "%.2f", game.maxTargetPrice)} €",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmberGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        val deal = game.detectedDeal
                        if (deal != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF09B1BA).copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🔥 Offre détectée sur ${deal.platform}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F766E)
                                        )
                                        Text(
                                            text = "${String.format(Locale.FRANCE, "%.2f", deal.price)} €",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F766E)
                                        )
                                    }
                                    if (game.maxTargetPrice > 0.0 && game.maxTargetPrice > deal.price) {
                                        Text(
                                            text = "Économie de ${String.format(Locale.FRANCE, "%.2f", game.maxTargetPrice - deal.price)} € par rapport à votre prix max !",
                                            fontSize = 11.sp,
                                            color = Color(0xFF115E59)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            if (deal.url.isNotBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, deal.url.toUri()).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Impossible d'ouvrir l'annonce.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF09B1BA))
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Voir l'annonce sur ${deal.platform}", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onEdit(game)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (game.maxTargetPrice > 0.0) "Modifier le prix max ou l'alerte" else "Fixer un prix max d'achat",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (game.purchaseDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Date d'achat : ${game.purchaseDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (game.barcode.isNotBlank()) {
                Text(
                    text = "Code-barres / Référence : ${game.barcode}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val cleanNotesText = game.cleanNotes
            if (cleanNotesText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Notes & Emplacement",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cleanNotesText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (game.isWishlist) {
                Spacer(modifier = Modifier.height(24.dp))

                // Action: Transfer from wishlist to collection
                Button(
                    onClick = { onToggleWishlist(game) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("detail_transfer_to_collection_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transférer dans ma Collection",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce jeu ?") },
            text = { Text("Êtes-vous sûr de vouloir retirer \"${game.title}\" de votre ${if (game.isWishlist) "wishlist" else "collection"} ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(game.id)
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun GradingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value.ifBlank { "Non spécifié" },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
