package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ai.GameScraperService
import com.example.ai.ScrapedGameData
import com.example.data.model.ConsoleCatalog
import com.example.data.model.GameItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScraperSheet(
    onDismiss: () -> Unit,
    onAddScrapedGame: (GameItem) -> Unit,
    onCustomizeGame: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Toutes") }
    var isPlatformDropdownOpen by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var scrapedData by remember { mutableStateOf<ScrapedGameData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val platformOptions = remember {
        listOf("Toutes") + ConsoleCatalog.popularPlatforms.filter { it != "Toutes" }
    }

    fun performScrape() {
        if (query.isBlank()) {
            Toast.makeText(context, "Veuillez entrer le nom d'un jeu.", Toast.LENGTH_SHORT).show()
            return
        }
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null
        scrapedData = null

        coroutineScope.launch {
            try {
                val result = GameScraperService.scrapeGame(query, selectedPlatform)
                result.onSuccess { data ->
                    scrapedData = data
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Échec du scraping des données."
                }
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("game_scraper_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Scraper un jeu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Jaquette, console, éditeur et année automatiques",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("scraper_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Inputs
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Nom du jeu à scrapper") },
                placeholder = { Text("Ex: Chrono Trigger, Super Mario 64, Zelda...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = BrandPrimary
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performScrape() }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scraper_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Platform Filter Dropdown & Scrape Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { isPlatformDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scraper_platform_filter_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Console (optionnel)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedPlatform,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isPlatformDropdownOpen,
                        onDismissRequest = { isPlatformDropdownOpen = false }
                    ) {
                        platformOptions.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    selectedPlatform = p
                                    isPlatformDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { performScrape() },
                    enabled = !isLoading && query.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.testTag("scraper_launch_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scraping...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scrapper")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Display
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Scraped Result Display
            scrapedData?.let { data ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scraped_result_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldMint.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = data.source,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldMint
                                    )
                                }
                            }

                            Text(
                                text = String.format(Locale.FRANCE, "%.2f € (Cote)", data.estimatedValue),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Game info with cover
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(width = 90.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (data.coverUrl.isNotBlank()) {
                                    val coverRequest = remember(context, data.coverUrl) {
                                        ImageRequest.Builder(context)
                                            .data(data.coverUrl)
                                            .crossfade(true)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = coverRequest,
                                        contentDescription = data.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = data.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = data.platform,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Éditeur : ${data.publisher}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Année : ${if (data.releaseYear > 0) data.releaseYear.toString() else "Inconnue"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (data.genre.isNotBlank()) {
                                    Text(
                                        text = "Genre : ${data.genre}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (data.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = data.description,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // PriceCharting Valuation Card
                        if (data.priceLooseEur != null || data.priceCibEur != null || data.priceNewEur != null || data.priceChartingUrl != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Cote PriceCharting.com",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (data.priceChartingUrl != null) {
                                            TextButton(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, data.priceChartingUrl.toUri())
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = "Ouvrir PriceCharting",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = BrandPrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Voir fiche", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Loose
                                        PriceTierBadge(
                                            label = "Loose (Seul)",
                                            priceEur = data.priceLooseEur,
                                            priceUsd = data.priceLooseUsd,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // CIB (Complet)
                                        PriceTierBadge(
                                            label = "CIB (Complet)",
                                            priceEur = data.priceCibEur ?: data.estimatedValue,
                                            priceUsd = data.priceCibUsd,
                                            isHighlighted = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Neuf / Scellé
                                        PriceTierBadge(
                                            label = "Neuf (Scellé)",
                                            priceEur = data.priceNewEur,
                                            priceUsd = data.priceNewUsd,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val item = GameItem(
                                        title = data.title,
                                        platform = data.platform,
                                        publisher = data.publisher,
                                        releaseYear = data.releaseYear,
                                        genre = data.genre,
                                        format = data.format,
                                        region = data.region,
                                        estimatedValue = data.estimatedValue,
                                        purchasePrice = data.estimatedValue,
                                        purchaseDate = "2024-01-01",
                                        notes = data.description,
                                        coverUrl = data.coverUrl,
                                        isWishlist = false
                                    )
                                    onAddScrapedGame(item)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scraper_add_to_collection_button")
                            ) {
                                Text("Ajouter (+)", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val item = GameItem(
                                        title = data.title,
                                        platform = data.platform,
                                        publisher = data.publisher,
                                        releaseYear = data.releaseYear,
                                        genre = data.genre,
                                        format = data.format,
                                        region = data.region,
                                        estimatedValue = data.estimatedValue,
                                        notes = data.description,
                                        coverUrl = data.coverUrl,
                                        isWishlist = true
                                    )
                                    onAddScrapedGame(item)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scraper_add_to_wishlist_button")
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wishlist", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val item = GameItem(
                                        title = data.title,
                                        platform = data.platform,
                                        publisher = data.publisher,
                                        releaseYear = data.releaseYear,
                                        genre = data.genre,
                                        format = data.format,
                                        region = data.region,
                                        estimatedValue = data.estimatedValue,
                                        purchasePrice = data.estimatedValue,
                                        notes = data.description,
                                        coverUrl = data.coverUrl
                                    )
                                    onCustomizeGame(item)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scraper_customize_button")
                            ) {
                                Text("Détailler", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PriceTierBadge(
    label: String,
    priceEur: Double?,
    priceUsd: Double?,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlighted) BrandPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = if (isHighlighted) BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.4f)) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (priceEur != null && priceEur > 0.0) String.format(Locale.FRANCE, "%.1f €", priceEur) else "--",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) BrandPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (priceUsd != null && priceUsd > 0.0) {
                Text(
                    text = String.format(Locale.US, "($%.0f)", priceUsd),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
