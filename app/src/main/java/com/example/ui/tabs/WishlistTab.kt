package com.example.ui.tabs

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.data.marketplace.MarketplacePreferencesManager
import com.example.data.model.ConsoleCatalog
import com.example.data.model.GameItem
import com.example.data.model.detectedDeal
import com.example.data.model.maxTargetPrice
import com.example.ui.UiState
import com.example.ui.components.ConsolePickerSheet
import com.example.ui.components.GameCard
import com.example.ui.components.rememberCollapsibleHeaderScrollState
import com.example.ui.theme.AmberGold
import com.example.worker.WishlistWorkScheduler
import java.util.Locale

@Composable
fun WishlistTab(
    state: UiState<List<GameItem>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedPlatform: String,
    onPlatformSelect: (String) -> Unit,
    onGameClick: (GameItem) -> Unit,
    onToggleWishlist: (GameItem) -> Unit,
    onOpenMarketplaceSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isConsolePickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefsManager = remember(context) { MarketplacePreferencesManager(context) }
    var connectedCount by remember(prefsManager) { mutableIntStateOf(prefsManager.connectedAccountsCount) }

    DisposableEffect(context, prefsManager) {
        val sharedPrefs = context.applicationContext.getSharedPreferences("marketplace_accounts_prefs", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            connectedCount = prefsManager.connectedAccountsCount
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val wishlistPlatforms = remember(state) {
        if (state is UiState.Success) {
            state.data.map { it.platform }.filter { it.isNotBlank() }.distinct()
        } else {
            emptyList()
        }
    }

    val displayedPlatforms = remember(wishlistPlatforms, selectedPlatform) {
        val list = mutableListOf("Toutes")
        if (selectedPlatform != "Toutes" && !list.contains(selectedPlatform)) {
            list.add(selectedPlatform)
        }
        wishlistPlatforms.forEach { p ->
            if (!list.contains(p)) list.add(p)
        }
        ConsoleCatalog.popularPlatforms.forEach { p ->
            if (!list.contains(p)) list.add(p)
        }
        list.take(16)
    }

    val listState = rememberLazyListState()
    val headerScrollState = rememberCollapsibleHeaderScrollState(listState)
    val isListEmpty = state !is UiState.Success || state.data.isEmpty()

    LaunchedEffect(isListEmpty) {
        if (isListEmpty) {
            headerScrollState.showHeader()
        }
    }

    val showHeaderRows = headerScrollState.isHeaderVisible || isListEmpty

    Column(modifier = modifier.fillMaxSize()) {
        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher dans la wishlist...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Recherche",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("wishlist_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        }

        // Collapsible Filter & Marketplace Rows (hide when scrolling down, reappear when scrolling up)
        AnimatedVisibility(
            visible = showHeaderRows,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Platform Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button to open full Wikipedia consoles picker
                    FilterChip(
                        selected = selectedPlatform != "Toutes" && !ConsoleCatalog.popularPlatforms.contains(selectedPlatform),
                        onClick = { isConsolePickerOpen = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AmberGold
                            )
                        },
                        label = { Text("Toutes les consoles (${ConsoleCatalog.allConsoles.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AmberGold.copy(alpha = 0.12f),
                            labelColor = Color(0xFFB45309)
                        ),
                        modifier = Modifier.testTag("wishlist_open_consoles_picker")
                    )

                    displayedPlatforms.forEach { platform ->
                        val isSelected = selectedPlatform == platform
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlatformSelect(platform) },
                            label = { Text(platform, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("wishlist_chip_platform_$platform")
                        )
                    }
                }

                // Vinted & eBay Quick Access and Manual Price Check Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        onClick = onOpenMarketplaceSheet,
                        shape = RoundedCornerShape(10.dp),
                        color = if (connectedCount > 0) Color(0xFF09B1BA).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("btn_wishlist_marketplace_accounts")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = if (connectedCount > 0) Color(0xFF09B1BA) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (connectedCount > 0) "Vinted & eBay ($connectedCount liés)" else "Brancher Vinted / eBay",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedCount > 0) Color(0xFF0F766E) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            WishlistWorkScheduler.triggerImmediatePriceCheck(context)
                            Toast.makeText(context, "Scan Vinted & eBay lancé en arrière-plan...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        modifier = Modifier.testTag("btn_wishlist_scan_now")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vérifier les prix",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }
                }
            }
        }

        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AmberGold)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val games = state.data
                val totalCostEstimate = remember(games) { games.sumOf { it.estimatedValue } }
                val detectedDeals = remember(games) { games.filter { it.detectedDeal != null } }

                AnimatedVisibility(
                    visible = showHeaderRows,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(180))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Wishlist Budget Banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("wishlist_budget_banner"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Wishlist / Wantlist",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                    Text(
                                        text = "${games.size} jeux recherchés",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Budget estimé",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.FRANCE, "%.2f €", totalCostEstimate),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmberGold
                                    )
                                }
                            }
                        }

                        if (detectedDeals.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .testTag("wishlist_deals_banner"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF09B1BA).copy(alpha = 0.12f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF09B1BA).copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = Color(0xFF0F766E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🔥 ${detectedDeals.size} bonne(s) affaire(s) sous votre prix max !",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F766E)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    detectedDeals.take(3).forEach { gameWithDeal ->
                                        val deal = gameWithDeal.detectedDeal ?: return@forEach
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${gameWithDeal.title} (${gameWithDeal.platform})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = String.format(
                                                        Locale.FRANCE,
                                                        "Trouvé à %.2f € sur %s (votre max: %.2f €)",
                                                        deal.price,
                                                        deal.platform,
                                                        gameWithDeal.maxTargetPrice
                                                    ),
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF115E59)
                                                )
                                            }
                                            if (deal.url.isNotBlank()) {
                                                Surface(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, deal.url.toUri()).apply {
                                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {
                                                            Toast.makeText(context, "Impossible d'ouvrir l'annonce.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (deal.platform == "Vinted") Color(0xFF09B1BA) else Color(0xFF0064D2)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.OpenInBrowser,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Voir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (games.isEmpty()) {
                    EmptyWishlistState()
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(headerScrollState.nestedScrollConnection)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("wishlist_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(games, key = { it.id.ifBlank { "${it.title}_${it.platform}_${it.completeness}" } }) { game ->
                                GameCard(
                                    game = game,
                                    onClick = { onGameClick(game) },
                                    onToggleWishlist = { onToggleWishlist(game) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isConsolePickerOpen) {
        ConsolePickerSheet(
            selectedPlatformName = selectedPlatform,
            onDismiss = { isConsolePickerOpen = false },
            onConsoleSelected = { console ->
                onPlatformSelect(console.name)
            }
        )
    }
}

@Composable
private fun EmptyWishlistState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AmberGold.copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Votre Wishlist est vide",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ajoutez les pépites rétro et modernes que vous recherchez pour suivre leur cote et préparer vos prochains achats en brocante ou salon !",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
