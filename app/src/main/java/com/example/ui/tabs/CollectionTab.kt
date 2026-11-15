package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConsoleCatalog
import com.example.data.model.GameItem
import com.example.ui.SortOrder
import com.example.ui.UiState
import com.example.ui.components.ConsolePickerSheet
import com.example.ui.components.GameCard
import com.example.ui.components.rememberCollapsibleHeaderScrollState
import com.example.ui.theme.BrandPrimary

private val COMPLETENESS_FILTERS = listOf("Toutes", "CIB", "Loose", "Boîte sans notice", "Neuf scellé")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionTab(
    state: UiState<List<GameItem>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedPlatform: String,
    onPlatformSelect: (String) -> Unit,
    selectedCompleteness: String,
    onCompletenessSelect: (String) -> Unit,
    sortOrder: SortOrder,
    onSortSelect: (SortOrder) -> Unit,
    onGameClick: (GameItem) -> Unit,
    onToggleWishlist: (GameItem) -> Unit,
    onImportClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedGameIds: Set<String> = emptySet(),
    onToggleSelectionMode: () -> Unit = {},
    onToggleGameSelection: (String) -> Unit = {},
    onSelectAll: (List<String>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBatchChangePlatform: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isConsolePickerOpen by remember { mutableStateOf(false) }
    var isBatchConsolePickerOpen by remember { mutableStateOf(false) }

    val collectionPlatforms = remember(state) {
        if (state is UiState.Success) {
            state.data.map { it.platform }.filter { it.isNotBlank() }.distinct()
        } else {
            emptyList()
        }
    }

    val displayedPlatforms = remember(collectionPlatforms, selectedPlatform) {
        val list = mutableListOf("Toutes")
        if (selectedPlatform != "Toutes" && !list.contains(selectedPlatform)) {
            list.add(selectedPlatform)
        }
        collectionPlatforms.forEach { p ->
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
        // Search & Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher dans la collection...", fontSize = 13.sp) },
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
                    .weight(1f)
                    .height(50.dp)
                    .testTag("collection_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { isSortMenuOpen = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("collection_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Trier",
                        tint = BrandPrimary
                    )
                }

                DropdownMenu(
                    expanded = isSortMenuOpen,
                    onDismissRequest = { isSortMenuOpen = false }
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                    color = if (order == sortOrder) BrandPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSortSelect(order)
                                isSortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Collapsible Filter Rows (hide when scrolling down, reappear when scrolling up)
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
                                tint = BrandPrimary
                            )
                        },
                        label = { Text("Toutes les consoles (${ConsoleCatalog.allConsoles.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = BrandPrimary.copy(alpha = 0.08f),
                            labelColor = BrandPrimary
                        ),
                        modifier = Modifier.testTag("collection_open_consoles_picker")
                    )

                    displayedPlatforms.forEach { platform ->
                        val isSelected = selectedPlatform == platform
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlatformSelect(platform) },
                            label = { Text(platform, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_platform_$platform")
                        )
                    }
                }

                // Completeness filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    COMPLETENESS_FILTERS.forEach { comp ->
                        val isSelected = selectedCompleteness == comp
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCompletenessSelect(comp) },
                            label = { Text(comp, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }
        }

        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandPrimary)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
            is UiState.Success -> {
                val games = state.data

                if (games.isEmpty()) {
                    EmptyCollectionState(
                        isFiltered = searchQuery.isNotBlank() || selectedPlatform != "Toutes" || selectedCompleteness != "Toutes",
                        onImportClick = onImportClick
                    )
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
                                .testTag("collection_list"),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (isSelectionMode) 170.dp else 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(games, key = { it.id.ifBlank { "${it.title}_${it.platform}_${it.completeness}" } }) { game ->
                                GameCard(
                                    game = game,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleGameSelection(game.id)
                                        } else {
                                            onGameClick(game)
                                        }
                                    },
                                    onToggleWishlist = { onToggleWishlist(game) },
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedGameIds.contains(game.id),
                                    onSelectToggle = { onToggleGameSelection(game.id) }
                                )
                            }
                        }

                        if (isSelectionMode) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    .testTag("batch_selection_bar"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = BrandPrimary,
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = "${selectedGameIds.size}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (selectedGameIds.size <= 1) "${selectedGameIds.size} jeu sélectionné" else "${selectedGameIds.size} jeux sélectionnés",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        val allVisibleSelected = remember(games, selectedGameIds) {
                                            games.isNotEmpty() && games.all { it.id in selectedGameIds }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = {
                                                    if (allVisibleSelected) {
                                                        onClearSelection()
                                                    } else {
                                                        onSelectAll(games.map { it.id })
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (allVisibleSelected) "Tout désél." else "Tout sélect.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            IconButton(
                                                onClick = onToggleSelectionMode,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Fermer la sélection",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { isBatchConsolePickerOpen = true },
                                        enabled = selectedGameIds.isNotEmpty(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("btn_batch_change_console"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BrandPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsEsports,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Changer la console de jeux (${selectedGameIds.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isBatchConsolePickerOpen) {
        ConsolePickerSheet(
            selectedPlatformName = selectedPlatform,
            onDismiss = { isBatchConsolePickerOpen = false },
            onConsoleSelected = { console ->
                onBatchChangePlatform(console.name)
                isBatchConsolePickerOpen = false
            }
        )
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
fun EmptyCollectionState(
    isFiltered: Boolean,
    onImportClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BrandPrimary.copy(alpha = 0.1f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isFiltered) "Aucun jeu ne correspond à vos filtres" else "Votre collection est encore vide",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFiltered) "Essayez d'effacer la recherche ou de changer de plateforme." else "Commencez à ajouter vos jeux physiques avec le bouton + ou importez un fichier existant !",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (!isFiltered) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_collection_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importer CSV")
                }
            }
        }
    }
}
