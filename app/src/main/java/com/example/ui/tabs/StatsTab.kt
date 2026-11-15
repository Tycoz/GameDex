package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameItem
import com.example.data.pricecharting.PriceChartingPreferenceManager
import com.example.ui.components.getCompletenessColor
import com.example.ui.components.getPlatformColor
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import java.util.Locale

@Composable
fun StatsTab(
    allGames: List<GameItem>,
    isPriceUpdating: Boolean = false,
    onRefreshPrices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lastUpdateFormatted = remember(isPriceUpdating) {
        PriceChartingPreferenceManager.getLastUpdateFormatted(context)
    }
    val lastCount = remember(isPriceUpdating) {
        PriceChartingPreferenceManager.getLastUpdatedGamesCount(context)
    }
    val lastDiff = remember(isPriceUpdating) {
        PriceChartingPreferenceManager.getLastUpdateValueDifference(context)
    }

    val collectionGames = remember(allGames) { allGames.filter { !it.isWishlist } }
    val wishlistGames = remember(allGames) { allGames.filter { it.isWishlist } }

    val totalEstValue = remember(collectionGames) { collectionGames.sumOf { it.estimatedValue } }
    val totalInvested = remember(collectionGames) { collectionGames.sumOf { it.purchasePrice } }
    val netGain = totalEstValue - totalInvested
    val gainPercent = if (totalInvested > 0) ((netGain / totalInvested) * 100) else 0.0
    val wishVal = remember(wishlistGames) { wishlistGames.sumOf { it.estimatedValue } }

    val platformCounts = remember(collectionGames) {
        collectionGames
            .groupBy { it.platform }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val completenessCounts = remember(collectionGames) {
        collectionGames
            .groupBy { it.completeness }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val topValuable = remember(collectionGames) {
        collectionGames
            .sortedByDescending { it.estimatedValue }
            .take(5)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Global Financial Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldMint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = EmeraldMint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Valeur de la Collection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            color = BrandPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PriceCharting.com",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = String.format(Locale.FRANCE, "%.2f €", totalEstValue),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldMint
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Investi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.FRANCE, "%.2f €", totalInvested), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("Plus-value estimée", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.FRANCE, if (netGain >= 0) "+%.2f €" else "%.2f €", netGain),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netGain >= 0) EmeraldMint else MaterialTheme.colorScheme.error
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rendement", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.FRANCE, if (gainPercent >= 0) "+%.1f %%" else "%.1f %%", gainPercent),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gainPercent >= 0) EmeraldMint else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Section: PriceCharting Monthly Valuation Update
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Actualisation des Cotes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Source : https://www.pricecharting.com/",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = EmeraldMint.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "1x / mois",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldMint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dernière actualisation : $lastUpdateFormatted",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (lastCount > 0) {
                        val diffFormatted = String.format(Locale.FRANCE, if (lastDiff >= 0) "+%.2f €" else "%.2f €", lastDiff)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$lastCount jeu(x) mis à jour lors de la dernière passe ($diffFormatted)",
                            fontSize = 11.sp,
                            color = if (lastDiff >= 0) EmeraldMint else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRefreshPrices,
                        enabled = !isPriceUpdating && collectionGames.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refresh_pricecharting_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isPriceUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Actualisation PriceCharting...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Actualiser maintenant (PriceCharting)")
                        }
                    }
                }
            }
        }

        // Section: Overview Cards (Collection vs Wishlist)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Collection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${collectionGames.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BrandPrimary)
                        Text("jeux possédés", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wishlist", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${wishlistGames.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AmberGold)
                        Text(String.format(Locale.FRANCE, "budget: %.0f €", wishVal), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Section: Platform Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Répartition par Console / Plateforme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (platformCounts.isEmpty()) {
                        Text("Aucune donnée disponible", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val maxCount = platformCounts.firstOrNull()?.second ?: 1
                        platformCounts.forEach { (platform, count) ->
                            val progress = count.toFloat() / maxCount.toFloat()
                            val color = remember(platform) { getPlatformColor(platform) }

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(platform, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("$count jeu${if (count > 1) "x" else ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = color.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Completeness Breakdown (CIB vs Loose vs Sealed)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Complétude Physique",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (completenessCounts.isEmpty()) {
                        Text("Aucune donnée", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        completenessCounts.forEach { (comp, count) ->
                            val percent = if (collectionGames.isNotEmpty()) (count.toDouble() / collectionGames.size * 100) else 0.0
                            val color = remember(comp) { getCompletenessColor(comp) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(comp, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }

                                Text(
                                    text = String.format(Locale.FRANCE, "%d (%.0f %%)", count, percent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Top 5 Most Valuable Games
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Top Pièces Rares & Cotes Élevées",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topValuable.isEmpty()) {
                        Text("Ajoutez des jeux avec leur cote estimée pour voir votre classement.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        topValuable.forEachIndexed { index, game ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (index == 0) AmberGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (index == 0) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = game.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${game.platform} • ${game.completeness}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.FRANCE, "%.2f €", game.estimatedValue),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = EmeraldMint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
