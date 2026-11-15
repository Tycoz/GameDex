package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GameItem
import com.example.data.model.priceAlert
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.GoodCondition
import com.example.ui.theme.LooseAccent
import com.example.ui.theme.MintCondition
import com.example.ui.theme.SealedAccent
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(
    game: GameItem,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val platformColor = remember(game.platform) { getPlatformColor(game.platform) }
    val completenessColor = remember(game.completeness) { getCompletenessColor(game.completeness) }
    val stableId = remember(game.id, game.title) { game.id.ifBlank { game.title.hashCode().toString() } }
    val coverRequest = remember(context, game.coverUrl) {
        if (game.coverUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(game.coverUrl)
                .crossfade(true)
                .build()
        } else null
    }
    val wishlistAlert = remember(game.isWishlist, game.notes) {
        if (game.isWishlist) game.priceAlert else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    onSelectToggle()
                }
            )
            .testTag("game_card_$stableId"),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, BrandPrimary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Game Visual / Cover Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(platformColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (coverRequest != null) {
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = "Jaquette ${game.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = platformColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = game.format.take(8),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                // Platform & Region Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = platformColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = game.platform,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = platformColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (game.region.isNotBlank()) {
                            Text(
                                text = game.region,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onSelectToggle() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandPrimary,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("checkbox_game_$stableId")
                            )
                        } else {
                            IconButton(
                                onClick = onToggleWishlist,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("toggle_wishlist_btn")
                            ) {
                                Icon(
                                    imageVector = if (game.isWishlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (game.isWishlist) "Dans la wishlist" else "Ajouter à la wishlist",
                                    tint = if (game.isWishlist) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Title
                Text(
                    text = game.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Completeness badge & Year
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Surface(
                        color = completenessColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = game.completeness,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = completenessColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (game.releaseYear > 0) {
                        Text(
                            text = " • ${game.releaseYear}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (game.rating > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${game.rating}/5",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Financials & Discogs-like Valuation Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Cote estimée",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (game.estimatedValue > 0) String.format(Locale.FRANCE, "%.2f €", game.estimatedValue) else "Non coté",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldMint
                        )
                    }

                    if (!game.isWishlist && game.purchasePrice > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Prix payé",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.FRANCE, "%.2f €", game.purchasePrice),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (wishlistAlert != null) {
                        val deal = wishlistAlert.deal
                        val maxPrice = wishlistAlert.maxTargetPrice
                        val alertActive = wishlistAlert.isEnabled && maxPrice > 0.0
                        if (deal != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = DealBadgeBgColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🔥 ${String.format(Locale.FRANCE, "%.2f", deal.price)} € (${deal.platform})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DealBadgeTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (maxPrice > 0.0) {
                                    Text(
                                        text = "Max: ${String.format(Locale.FRANCE, "%.2f", maxPrice)} €",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (maxPrice > 0.0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = AmberGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🎯 Max: ${String.format(Locale.FRANCE, "%.2f", maxPrice)} €",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (alertActive) {
                                    Text(
                                        text = "Alerte active",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EmeraldMint
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = AmberGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "En Wishlist",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val DealBadgeBgColor = Color(0xFF09B1BA).copy(alpha = 0.18f)
private val DealBadgeTextColor = Color(0xFF0F766E)

private val NintendoColor = Color(0xFFE52521)
private val PlayStationColor = Color(0xFF003791)
private val XboxColor = Color(0xFF107C10)
private val SegaColor = Color(0xFF0089CF)
private val AtariColor = Color(0xFFE04030)
private val NeoGeoColor = Color(0xFFD97706)
private val PcEngineColor = Color(0xFF0D9488)
private val AmigaColor = Color(0xFF7C3AED)
private val GameBoyColor = Color(0xFF8B5CF6)
private val RetroOtherColor = Color(0xFFB45309)
private val SteamDeckColor = Color(0xFF1E293B)
private val PcColor = Color(0xFFF97316)

fun getPlatformColor(platform: String): Color {
    val lower = platform.lowercase()
    return when {
        lower.contains("nintendo") || lower.contains("nes") || lower.contains("snes") || lower.contains("switch") || lower.contains("mario") || lower.contains("famicom") -> NintendoColor
        lower.contains("playstation") || lower.contains("ps1") || lower.contains("ps2") || lower.contains("ps3") || lower.contains("ps4") || lower.contains("ps5") || lower.contains("psp") || lower.contains("vita") -> PlayStationColor
        lower.contains("xbox") -> XboxColor
        lower.contains("sega") || lower.contains("mega drive") || lower.contains("dreamcast") || lower.contains("saturn") || lower.contains("game gear") || lower.contains("master system") -> SegaColor
        lower.contains("atari") || lower.contains("lynx") || lower.contains("jaguar") -> AtariColor
        lower.contains("neo-geo") || lower.contains("snk") -> NeoGeoColor
        lower.contains("pc-engine") || lower.contains("nec") || lower.contains("turbografx") || lower.contains("pc-fx") -> PcEngineColor
        lower.contains("amiga") || lower.contains("commodore") -> AmigaColor
        lower.contains("game boy") -> GameBoyColor
        lower.contains("vectrex") || lower.contains("coleco") || lower.contains("intellivision") || lower.contains("odyssey") || lower.contains("videopac") || lower.contains("cd-i") -> RetroOtherColor
        lower.contains("steam deck") -> SteamDeckColor
        lower.contains("pc") -> PcColor
        else -> BrandPrimary
    }
}

fun getCompletenessColor(completeness: String): Color {
    return when {
        completeness.contains("Scellé", ignoreCase = true) -> SealedAccent
        completeness.contains("CIB", ignoreCase = true) -> MintCondition
        completeness.contains("Loose", ignoreCase = true) -> LooseAccent
        else -> GoodCondition
    }
}
