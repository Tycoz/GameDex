package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GameItem
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.platform.LocalContext
import com.example.notification.PriceChartingNotificationHelper
import com.example.notification.WishlistNotificationHelper
import com.example.ui.components.ExportCollectionSheet
import com.example.ui.components.GameDetailSheet
import com.example.ui.components.GameFormDialog
import com.example.ui.components.GameScraperSheet
import com.example.ui.components.ImportCollectionSheet
import com.example.ui.components.MarketplaceAccountsSheet
import com.example.ui.components.QrScanDialog
import com.example.worker.MonthlyPriceUpdateScheduler
import com.example.worker.WishlistWorkScheduler
import com.example.ui.tabs.CollectionTab
import com.example.ui.tabs.StatsTab
import com.example.ui.tabs.WishlistTab
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDexApp(
    viewModel: GameDexViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val collectionState by viewModel.filteredCollection.collectAsStateWithLifecycle()
    val wishlistState by viewModel.filteredWishlist.collectAsStateWithLifecycle()
    val allGamesForStats by viewModel.allGamesForStats.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val selectedCompleteness by viewModel.selectedCompleteness.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val selectedGameForDetail by viewModel.selectedGameForDetail.collectAsStateWithLifecycle()
    val gameForEditing by viewModel.gameForEditing.collectAsStateWithLifecycle()
    val isFormOpen by viewModel.isFormDialogOpen.collectAsStateWithLifecycle()
    val messageEvent by viewModel.messageEvent.collectAsStateWithLifecycle()
    val isPriceUpdating by viewModel.isPriceUpdating.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedGameIds by viewModel.selectedGameIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var userMenuExpanded by remember { mutableStateOf(false) }
    var isGlobalQrScanOpen by remember { mutableStateOf(false) }
    var isExportSheetOpen by remember { mutableStateOf(false) }
    var isImportSheetOpen by remember { mutableStateOf(false) }
    var isScraperSheetOpen by remember { mutableStateOf(false) }
    var isMarketplaceSheetOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        WishlistNotificationHelper.createNotificationChannel(context)
        WishlistWorkScheduler.scheduleDailyPriceCheck(context)
        PriceChartingNotificationHelper.createNotificationChannel(context)
        MonthlyPriceUpdateScheduler.scheduleMonthlyPriceUpdate(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentUser = Firebase.auth.currentUser
    val userDisplayName = currentUser?.displayName ?: currentUser?.email ?: "Collectionneur"
    val userInitial = userDisplayName.firstOrNull()?.uppercase() ?: "G"

    LaunchedEffect(messageEvent) {
        messageEvent?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "GameDex",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Logo",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isGlobalQrScanOpen = true },
                        modifier = Modifier.testTag("topbar_qr_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scanner un jeu par photo (IA)",
                            tint = BrandPrimary
                        )
                    }

                    IconButton(
                        onClick = { isScraperSheetOpen = true },
                        modifier = Modifier.testTag("topbar_scraper_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = "Scrapper les infos et jaquette",
                            tint = BrandPrimary
                        )
                    }

                    IconButton(
                        onClick = { isImportSheetOpen = true },
                        modifier = Modifier.testTag("topbar_import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Importer une collection CSV",
                            tint = EmeraldMint
                        )
                    }

                    IconButton(
                        onClick = { isExportSheetOpen = true },
                        modifier = Modifier.testTag("topbar_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Exporter la collection en CSV",
                            tint = BrandPrimary
                        )
                    }

                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = { userMenuExpanded = true },
                            modifier = Modifier.testTag("user_profile_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BrandPrimary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userInitial,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = userMenuExpanded,
                            onDismissRequest = { userMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = userDisplayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {}
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Scrapper un jeu (Jaquette & Infos)")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.TravelExplore,
                                        contentDescription = null,
                                        tint = BrandPrimary
                                    )
                                },
                                onClick = {
                                    userMenuExpanded = false
                                    isScraperSheetOpen = true
                                },
                                modifier = Modifier.testTag("menu_scraper")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Importer un CSV (Collection)")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = EmeraldMint
                                    )
                                },
                                onClick = {
                                    userMenuExpanded = false
                                    isImportSheetOpen = true
                                },
                                modifier = Modifier.testTag("menu_import_csv")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Exporter en CSV (Appareil / Drive)")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = BrandPrimary
                                    )
                                },
                                onClick = {
                                    userMenuExpanded = false
                                    isExportSheetOpen = true
                                },
                                modifier = Modifier.testTag("menu_export_csv")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Comptes Vinted & eBay (Alertes)")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = AmberGold
                                    )
                                },
                                onClick = {
                                    userMenuExpanded = false
                                    isMarketplaceSheetOpen = true
                                },
                                modifier = Modifier.testTag("menu_marketplace_accounts")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    userMenuExpanded = false
                                    onSignOut()
                                },
                                modifier = Modifier.testTag("menu_sign_out")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Collection") },
                    label = { Text("Collection") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = BrandPrimary
                    ),
                    modifier = Modifier.testTag("nav_collection")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Wishlist") },
                    label = { Text("Wishlist") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = AmberGold
                    ),
                    modifier = Modifier.testTag("nav_wishlist")
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Statistiques") },
                    label = { Text("Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = BrandPrimary
                    ),
                    modifier = Modifier.testTag("nav_stats")
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = currentTab == 0 || currentTab == 1) {
                FloatingActionButton(
                    onClick = {
                        val isWish = currentTab == 1
                        viewModel.openNewGameForm(GameItem(isWishlist = isWish))
                    },
                    containerColor = if (currentTab == 1) AmberGold else BrandPrimary,
                    contentColor = if (currentTab == 1) Color.Black else Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_add_game")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter un jeu"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                0 -> CollectionTab(
                    state = collectionState,
                    searchQuery = searchQuery,
                    onSearchChange = viewModel::setSearchQuery,
                    selectedPlatform = selectedPlatform,
                    onPlatformSelect = viewModel::setSelectedPlatform,
                    selectedCompleteness = selectedCompleteness,
                    onCompletenessSelect = viewModel::setSelectedCompleteness,
                    sortOrder = sortOrder,
                    onSortSelect = viewModel::setSortOrder,
                    onGameClick = viewModel::openGameDetail,
                    onToggleWishlist = viewModel::toggleWishlist,
                    onImportClick = { isImportSheetOpen = true },
                    isSelectionMode = isSelectionMode,
                    selectedGameIds = selectedGameIds,
                    onToggleSelectionMode = { viewModel.setSelectionMode(!isSelectionMode) },
                    onToggleGameSelection = viewModel::toggleGameSelection,
                    onSelectAll = viewModel::selectAllGames,
                    onClearSelection = viewModel::clearSelection,
                    onBatchChangePlatform = viewModel::batchChangePlatformForSelectedGames
                )
                1 -> WishlistTab(
                    state = wishlistState,
                    searchQuery = searchQuery,
                    onSearchChange = viewModel::setSearchQuery,
                    selectedPlatform = selectedPlatform,
                    onPlatformSelect = viewModel::setSelectedPlatform,
                    onGameClick = viewModel::openGameDetail,
                    onToggleWishlist = viewModel::toggleWishlist,
                    onOpenMarketplaceSheet = { isMarketplaceSheetOpen = true }
                )
                2 -> StatsTab(
                    allGames = allGamesForStats,
                    isPriceUpdating = isPriceUpdating,
                    onRefreshPrices = { viewModel.refreshAllGamePricesFromPriceCharting(context) }
                )
            }
        }
    }

    // Game Detail Modal Sheet
    selectedGameForDetail?.let { game ->
        GameDetailSheet(
            game = game,
            onDismiss = viewModel::closeGameDetail,
            onEdit = {
                viewModel.closeGameDetail()
                viewModel.openEditGameForm(it)
            },
            onToggleWishlist = viewModel::toggleWishlist,
            onDelete = viewModel::deleteGame,
            onUpdatePriceCharting = { g, onDone ->
                viewModel.updateSingleGamePriceFromPriceCharting(g) {
                    onDone()
                }
            }
        )
    }

    // Add / Edit Form Modal Sheet
    if (isFormOpen) {
        GameFormDialog(
            initialGame = gameForEditing,
            onDismiss = viewModel::closeFormDialog,
            onSave = viewModel::saveGame
        )
    }

    // Global QR Scanner Sheet
    if (isGlobalQrScanOpen) {
        QrScanDialog(
            onDismiss = { isGlobalQrScanOpen = false },
            onGameScanned = { scannedGame ->
                isGlobalQrScanOpen = false
                viewModel.openNewGameForm(scannedGame)
            }
        )
    }

    // CSV Export Sheet (Device / Google Drive)
    if (isExportSheetOpen) {
        ExportCollectionSheet(
            allGames = allGamesForStats,
            onDismiss = { isExportSheetOpen = false }
        )
    }

    // CSV Import Sheet (Device / Google Drive / Text)
    if (isImportSheetOpen) {
        ImportCollectionSheet(
            existingGames = allGamesForStats,
            onImportConfirmed = { gamesToImport, onDone ->
                viewModel.importGames(gamesToImport) { result ->
                    onDone(result.isSuccess)
                    if (result.isSuccess) {
                        isImportSheetOpen = false
                    }
                }
            },
            onDismiss = { isImportSheetOpen = false }
        )
    }

    // Game Scraper Sheet (Wikipedia & AI)
    if (isScraperSheetOpen) {
        GameScraperSheet(
            onDismiss = { isScraperSheetOpen = false },
            onAddScrapedGame = { scrapedGame ->
                viewModel.saveGame(scrapedGame)
            },
            onCustomizeGame = { scrapedGame ->
                viewModel.openNewGameForm(scrapedGame)
            }
        )
    }

    // Vinted & eBay Accounts Sheet (Daily Price Alerts)
    if (isMarketplaceSheetOpen) {
        MarketplaceAccountsSheet(
            onDismiss = { isMarketplaceSheetOpen = false },
            onImmediateCheckRequested = {
                // Background scan triggered
            }
        )
    }
}

