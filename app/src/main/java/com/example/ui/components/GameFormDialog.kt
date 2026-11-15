package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ai.GameScraperService
import com.example.data.model.ConsoleCatalog
import com.example.data.model.GameCatalog
import com.example.data.model.GameItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import androidx.compose.material.icons.filled.NotificationsActive
import com.example.data.model.PriceAlert
import com.example.data.model.cleanNotes
import com.example.data.model.priceAlert
import com.example.data.model.withPriceAlert
import com.example.ui.theme.EmeraldMint
import com.example.data.storage.GameCoverStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val FORM_COMPLETENESS_OPTIONS = listOf("CIB", "Loose", "Boîte sans notice", "Neuf scellé")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameFormDialog(
    initialGame: GameItem?,
    onDismiss: () -> Unit,
    onSave: (GameItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialGame?.title ?: "") }
    var platform by remember { mutableStateOf(initialGame?.platform ?: "Nintendo 64") }
    var isWishlist by remember { mutableStateOf(initialGame?.isWishlist ?: false) }
    var completeness by remember { mutableStateOf(initialGame?.completeness ?: "CIB") }
    var format by remember { mutableStateOf(initialGame?.format ?: "Cartouche") }
    var region by remember { mutableStateOf(initialGame?.region ?: "PAL") }
    var releaseYearText by remember { mutableStateOf(if ((initialGame?.releaseYear ?: 0) > 0) initialGame?.releaseYear.toString() else "1998") }
    var genre by remember { mutableStateOf(initialGame?.genre ?: "") }
    var publisher by remember { mutableStateOf(initialGame?.publisher ?: "") }
    var barcode by remember { mutableStateOf(initialGame?.barcode ?: "") }

    var boxCondition by remember { mutableStateOf(initialGame?.boxCondition ?: "Très bon état") }
    var manualCondition by remember { mutableStateOf(initialGame?.manualCondition ?: "Présent & Parfait") }
    var mediaCondition by remember { mutableStateOf(initialGame?.mediaCondition ?: "Parfait / Mint") }

    var purchasePriceText by remember { mutableStateOf(if ((initialGame?.purchasePrice ?: 0.0) > 0.0) initialGame?.purchasePrice.toString() else "0") }
    var estimatedValueText by remember { mutableStateOf(if ((initialGame?.estimatedValue ?: 0.0) > 0.0) initialGame?.estimatedValue.toString() else "30") }
    var purchaseDate by remember { mutableStateOf(initialGame?.purchaseDate ?: "") }

    var rating by remember { mutableIntStateOf(initialGame?.rating ?: 4) }
    var status by remember { mutableStateOf(initialGame?.status ?: "Non commencé") }
    var notes by remember { mutableStateOf(initialGame?.cleanNotes ?: "") }
    var coverUrl by remember { mutableStateOf(initialGame?.coverUrl ?: "") }

    val initialAlert = remember(initialGame) { initialGame?.priceAlert }
    var maxTargetPriceText by remember {
        mutableStateOf(if ((initialAlert?.maxTargetPrice ?: 0.0) > 0.0) initialAlert?.maxTargetPrice.toString() else "")
    }
    var isPriceAlertEnabled by remember {
        mutableStateOf(initialAlert?.isEnabled ?: false)
    }

    var titleError by remember { mutableStateOf(false) }
    var isQrScannerOpen by remember { mutableStateOf(false) }
    var isConsolePickerOpen by remember { mutableStateOf(false) }
    var isScrapingInline by remember { mutableStateOf(false) }
    var photoToEdit by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val directCoverPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            photoToEdit = bitmap
        }
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
                .testTag("game_form_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialGame == null || initialGame.id.isBlank()) "Ajouter un jeu" else "Modifier le jeu",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Photo & Scan Import Button
            Surface(
                onClick = { isQrScannerOpen = true },
                shape = RoundedCornerShape(14.dp),
                color = BrandPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.2.dp, BrandPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_scan_qr_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandPrimary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Scanner le jeu par photo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Pochette ou cartouche avec recadrage IA",
                                fontSize = 11.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switch: Collection vs Wishlist
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isWishlist) "Mode : Wishlist / Wantlist" else "Mode : Ma Collection (Possédé)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isWishlist) AmberGold else BrandPrimary
                        )
                        Text(
                            text = if (isWishlist) "Jeu recherché pour acquisition future" else "Exemplaire physique dans votre ludothèque",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isWishlist,
                        onCheckedChange = { isWishlist = it },
                        modifier = Modifier.testTag("form_wishlist_switch")
                    )
                }
            }

            if (isWishlist) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_price_alert_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberGold.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Alerte de Prix Max (Vinted & eBay)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Scan quotidien 1x/jour et notification si le prix est atteint",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isPriceAlertEnabled,
                                onCheckedChange = { isPriceAlertEnabled = it },
                                modifier = Modifier.testTag("form_price_alert_switch")
                            )
                        }

                        if (isPriceAlertEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = maxTargetPriceText,
                                onValueChange = { maxTargetPriceText = it },
                                label = { Text("Prix d'achat max souhaité (€)", fontSize = 12.sp) },
                                placeholder = { Text("Ex: 40.0", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_max_target_price_input"),
                                shape = RoundedCornerShape(10.dp),
                                prefix = { Text("🎯 ", fontSize = 13.sp) },
                                suffix = { Text("€", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Titre du jeu *") },
                placeholder = { Text("Ex: Super Mario 64") },
                isError = titleError,
                supportingText = if (titleError) { { Text("Le titre est obligatoire") } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_title_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Scrape Action Button & Cover Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Saisissez d'abord un nom de jeu pour lancer le scraping.", Toast.LENGTH_SHORT).show()
                        } else {
                            isScrapingInline = true
                            coroutineScope.launch {
                                try {
                                    val result = GameScraperService.scrapeGame(title, platform)
                                    result.onSuccess { data ->
                                        title = data.title
                                        if (data.platform != "Autre") platform = data.platform
                                        if (data.publisher.isNotBlank()) publisher = data.publisher
                                        if (data.releaseYear > 0) releaseYearText = data.releaseYear.toString()
                                        if (data.coverUrl.isNotBlank()) coverUrl = data.coverUrl
                                        if (data.genre.isNotBlank()) genre = data.genre
                                        format = data.format
                                        if (data.estimatedValue > 0.0) {
                                            estimatedValueText = String.format(Locale.US, "%.2f", data.estimatedValue)
                                        }
                                        if (notes.isBlank() && data.description.isNotBlank()) notes = data.description
                                        Toast.makeText(
                                            context,
                                            "Scraping réussi : Jaquette, cote PriceCharting (${String.format(Locale.FRANCE, "%.2f €", data.estimatedValue)}), ${data.platform} !",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Scraping : ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isScrapingInline = false
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = BrandPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("form_scrape_inline_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScrapingInline) {
                            CircularProgressIndicator(
                                color = BrandPrimary,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scraping en cours...", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scrapper les infos & jaquette",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { directCoverPhotoLauncher.launch(null) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.testTag("form_direct_cover_photo_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Prendre en photo la jaquette",
                                tint = BrandPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Photo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (coverUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val coverRequest = remember(context, coverUrl) {
                            ImageRequest.Builder(context)
                                .data(coverUrl)
                                .crossfade(true)
                                .build()
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 30.dp, height = 38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandPrimary.copy(alpha = 0.15f))
                        ) {
                            AsyncImage(
                                model = coverRequest,
                                contentDescription = "Jaquette",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    val loaded = withContext(Dispatchers.IO) {
                                        GameCoverStorage.loadBitmapFromCoverUrl(context, coverUrl)
                                    }
                                    if (loaded != null) {
                                        photoToEdit = loaded
                                    } else {
                                        Toast.makeText(context, "Prenez une photo pour recadrer et réorienter la jaquette.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldMint.copy(alpha = 0.15f),
                            modifier = Modifier.testTag("form_edit_cover_photo_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = "Recadrer / Réorienter",
                                    tint = EmeraldMint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Ajuster",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldMint
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Platform selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plateforme / Console *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Wikipédia : ${ConsoleCatalog.allConsoles.size} consoles",
                    fontSize = 11.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("Console / Support") },
                placeholder = { Text("Ex: Super Nintendo, Atari 2600, PS5...") },
                trailingIcon = {
                    IconButton(
                        onClick = { isConsolePickerOpen = true },
                        modifier = Modifier.testTag("open_console_picker_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Parcourir toutes les consoles Wikipédia",
                            tint = BrandPrimary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_platform_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { isConsolePickerOpen = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                border = BorderStroke(1.2.dp, BrandPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_browse_consoles_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Parcourir la liste Wikipédia (1re à 9e génération)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConsoleCatalog.popularPlatforms.forEach { plat ->
                    val isSelected = platform.equals(plat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            platform = plat
                            ConsoleCatalog.findByName(plat)?.let { c ->
                                if (format.isBlank() || format == "Cartouche" || format == "Disque") {
                                    format = c.defaultFormat
                                }
                                if (releaseYearText == "1998" || releaseYearText.isBlank() || releaseYearText == "0") {
                                    releaseYearText = c.releaseYear.toString()
                                }
                            }
                        },
                        label = { Text(plat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completeness picker
            Text("Complétude (Format physique)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FORM_COMPLETENESS_OPTIONS.forEach { comp ->
                    val isSelected = completeness == comp
                    FilterChip(
                        selected = isSelected,
                        onClick = { completeness = comp },
                        label = { Text(comp, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Format & Region Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = format,
                    onValueChange = { format = it },
                    label = { Text("Support") },
                    placeholder = { Text("Cartouche / Disque") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Région") },
                    placeholder = { Text("PAL / NTSC-U") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Year & Genre Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = releaseYearText,
                    onValueChange = { releaseYearText = it },
                    label = { Text("Année") },
                    placeholder = { Text("1998") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    placeholder = { Text("Action / RPG") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Publisher & Barcode Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text("Éditeur") },
                    placeholder = { Text("Nintendo / Capcom") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Code-barres / EAN") },
                    placeholder = { Text("045496...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discogs-like Grading Matrix
            Text("Gradation de l'exemplaire (État)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))

            GradingSelector(label = "État de la Boîte", current = boxCondition, onSelect = { boxCondition = it })
            Spacer(modifier = Modifier.height(6.dp))
            GradingSelector(label = "État de la Notice", current = manualCondition, onSelect = { manualCondition = it })
            Spacer(modifier = Modifier.height(6.dp))
            GradingSelector(label = "Support (Disque/Cartouche)", current = mediaCondition, onSelect = { mediaCondition = it })

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing & Valuation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = estimatedValueText,
                    onValueChange = { estimatedValueText = it },
                    label = { Text("Cote estimée (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_estimated_value_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = purchasePriceText,
                    onValueChange = { purchasePriceText = it },
                    label = { Text("Prix payé (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rating Stars (0..5)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Note personnelle :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = if (rating == star) 0 else star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star étoiles",
                                tint = if (star <= rating) AmberGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes / Shelf Storage
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes personnelles & Emplacement étagère") },
                placeholder = { Text("Ex: Étagère A3, version française, blister d'origine...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }

                    val price = purchasePriceText.trim().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                    val value = estimatedValueText.trim().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                    val year = releaseYearText.trim().toIntOrNull() ?: 2000

                    val maxPrice = maxTargetPriceText.trim().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                    val updatedAlert = (initialAlert ?: PriceAlert()).copy(
                        maxTargetPrice = maxPrice,
                        isEnabled = isWishlist && isPriceAlertEnabled && maxPrice > 0.0
                    )

                    val updatedGame = (initialGame ?: GameItem()).copy(
                        title = title.trim(),
                        platform = platform.trim(),
                        isWishlist = isWishlist,
                        completeness = completeness.trim(),
                        format = format.trim(),
                        region = region.trim(),
                        releaseYear = year,
                        genre = genre.trim(),
                        publisher = publisher.trim(),
                        barcode = barcode.trim(),
                        boxCondition = boxCondition.trim(),
                        manualCondition = manualCondition.trim(),
                        mediaCondition = mediaCondition.trim(),
                        purchasePrice = price,
                        estimatedValue = value,
                        purchaseDate = purchaseDate.trim(),
                        rating = rating,
                        status = status.trim(),
                        notes = notes.trim(),
                        coverUrl = coverUrl.trim()
                    ).withPriceAlert(updatedAlert)

                    onSave(updatedGame)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("form_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (initialGame == null || initialGame.id.isBlank()) "Enregistrer dans GameDex" else "Mettre à jour",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (isQrScannerOpen) {
        QrScanDialog(
            onDismiss = { isQrScannerOpen = false },
            onGameScanned = { scannedGame ->
                title = scannedGame.title
                platform = scannedGame.platform
                completeness = scannedGame.completeness
                format = scannedGame.format
                region = scannedGame.region
                if (scannedGame.releaseYear > 0) releaseYearText = scannedGame.releaseYear.toString()
                if (scannedGame.genre.isNotBlank()) genre = scannedGame.genre
                if (scannedGame.publisher.isNotBlank()) publisher = scannedGame.publisher
                if (scannedGame.barcode.isNotBlank()) barcode = scannedGame.barcode
                if (scannedGame.boxCondition.isNotBlank()) boxCondition = scannedGame.boxCondition
                if (scannedGame.manualCondition.isNotBlank()) manualCondition = scannedGame.manualCondition
                if (scannedGame.mediaCondition.isNotBlank()) mediaCondition = scannedGame.mediaCondition
                if (scannedGame.estimatedValue > 0.0) estimatedValueText = scannedGame.estimatedValue.toString()
                if (scannedGame.purchasePrice > 0.0) purchasePriceText = scannedGame.purchasePrice.toString()
                if (scannedGame.notes.isNotBlank()) notes = scannedGame.notes
                if (scannedGame.coverUrl.isNotBlank()) coverUrl = scannedGame.coverUrl
                titleError = false
                isQrScannerOpen = false
            }
        )
    }

    if (isConsolePickerOpen) {
        ConsolePickerSheet(
            selectedPlatformName = platform,
            onDismiss = { isConsolePickerOpen = false },
            onConsoleSelected = { console ->
                platform = console.name
                if (format.isBlank() || format == "Cartouche" || format == "Disque") {
                    format = console.defaultFormat
                }
                if (releaseYearText == "1998" || releaseYearText.isBlank() || releaseYearText == "0") {
                    releaseYearText = console.releaseYear.toString()
                }
            }
        )
    }

    photoToEdit?.let { bitmap ->
        CoverPhotoEditorDialog(
            initialBitmap = bitmap,
            onDismiss = { photoToEdit = null },
            onConfirm = { editedBitmap ->
                val targetTitle = title.ifBlank { "game" }
                photoToEdit = null
                coroutineScope.launch {
                    val savedUrl = withContext(Dispatchers.IO) {
                        GameCoverStorage.saveCoverBitmap(context, editedBitmap, targetTitle)
                    }
                    if (savedUrl.isNotBlank()) {
                        coverUrl = savedUrl
                        Toast.makeText(context, "Jaquette recadrée et réorientée avec succès !", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun GradingSelector(
    label: String,
    current: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GameCatalog.conditions.forEach { cond ->
                val isSelected = current == cond
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(cond) },
                    label = { Text(cond, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
