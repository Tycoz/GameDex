package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.GameImageRecognitionService
import com.example.data.model.GameItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Crop
import coil.request.ImageRequest
import com.example.util.GameCoverCropUtil
import com.example.data.storage.GameCoverStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Dialog/Sheet for scanning and identifying games exclusively via photo (Cover / Cartridge) with AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScanDialog(
    onDismiss: () -> Unit,
    onGameScanned: (GameItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State for Photo recognition
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzingPhoto by remember { mutableStateOf(false) }
    var recognizedGame by remember { mutableStateOf<GameItem?>(null) }
    var photoAiError by remember { mutableStateOf<String?>(null) }
    var isEditingCoverPhoto by remember { mutableStateOf(false) }
    var editorInitialBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun applyDemoPreset(presetKey: String) {
        val item = GameImageRecognitionService.getDemoRecognitionItem(presetKey)
        val bmp = capturedBitmap
        if (bmp != null) {
            coroutineScope.launch {
                val savedUrl = withContext(Dispatchers.IO) {
                    val cropped = GameCoverCropUtil.autoCropGameCover(bmp)
                    GameCoverStorage.saveCoverBitmap(context, cropped, item.title)
                }
                recognizedGame = if (savedUrl.isNotBlank()) item.copy(coverUrl = savedUrl) else item
                photoAiError = null
            }
        } else {
            recognizedGame = item
            photoAiError = null
        }
    }

    fun openCoverEditor() {
        val currentBitmap = capturedBitmap
        if (currentBitmap != null) {
            editorInitialBitmap = currentBitmap
            isEditingCoverPhoto = true
        } else {
            val cover = recognizedGame?.coverUrl ?: return
            coroutineScope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    GameCoverStorage.loadBitmapFromCoverUrl(context, cover)
                }
                if (loaded != null) {
                    editorInitialBitmap = loaded
                    isEditingCoverPhoto = true
                }
            }
        }
    }

    // Launchers
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            photoAiError = null
            recognizedGame = null
            isAnalyzingPhoto = true
            coroutineScope.launch {
                try {
                    val result = GameImageRecognitionService.recognizeGameFromPhoto(context, bitmap)
                    if (result.isSuccess) {
                        recognizedGame = result.getOrNull()
                    } else {
                        photoAiError = result.exceptionOrNull()?.message
                            ?: "Impossible d'identifier le jeu sur cette photo."
                    }
                } finally {
                    isAnalyzingPhoto = false
                }
            }
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoAiError = null
            recognizedGame = null
            isAnalyzingPhoto = true
            coroutineScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        GameImageRecognitionService.uriToBitmap(context, uri)
                    }
                    if (bitmap != null) {
                        capturedBitmap = bitmap
                        val result = GameImageRecognitionService.recognizeGameFromPhoto(context, bitmap)
                        if (result.isSuccess) {
                            recognizedGame = result.getOrNull()
                        } else {
                            photoAiError = result.exceptionOrNull()?.message
                                ?: "Impossible d'identifier le jeu sur cette photo."
                        }
                    } else {
                        photoAiError = "Impossible de charger l'image sélectionnée."
                    }
                } finally {
                    isAnalyzingPhoto = false
                }
            }
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
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("import_game_modal_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Scanner par photo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pochette, boîte ou cartouche identifiée par IA",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
                // Info banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Photographiez la pochette, la boîte ou la cartouche. L'IA Gemini identifie le titre, la plateforme, le format et estime la cote marchande.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons for Camera & Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            takePhotoLauncher.launch(null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_take_photo"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Prendre photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_pick_photo"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galerie photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Loading progress when analyzing image
                if (isAnalyzingPhoto) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = BrandPrimary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Analyse visuelle par l'IA en cours...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Détection du titre, console, cartouche/disque et estimation de cote",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                color = BrandPrimary
                            )
                        }
                    }
                }

                // Error message display with smart fallback
                if (photoAiError != null && !isAnalyzingPhoto) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Note sur l'IA Gemini :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = photoAiError ?: "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Vous pouvez tester immédiatement avec les exemples ci-dessous ou configurer votre clé GEMINI_API_KEY.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            if (capturedBitmap != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { openCoverEditor() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_open_crop_editor_fallback"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Recadrer / Pivoter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val sourceBitmap = capturedBitmap ?: return@Button
                                            coroutineScope.launch {
                                                val savedUrl = withContext(Dispatchers.IO) {
                                                    val cropped = GameCoverCropUtil.autoCropGameCover(sourceBitmap)
                                                    GameCoverStorage.saveCoverBitmap(context, cropped, "Nouveau Jeu")
                                                }
                                                val fallbackGame = GameItem(
                                                    title = "",
                                                    platform = "Nintendo Switch",
                                                    coverUrl = savedUrl,
                                                    notes = "Jaquette issue de la photo recadrée par l'application"
                                                )
                                                onGameScanned(fallbackGame)
                                                onDismiss()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("use_cropped_photo_fallback")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Utiliser jaquette",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Display Recognized Game Card
                recognizedGame?.let { game ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, EmeraldMint, RoundedCornerShape(16.dp))
                            .testTag("recognized_game_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldMint.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldMint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Jeu identifié & Jaquette prête !",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldMint
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = game.platform,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Auto-cropped game cover art
                                val displayCover = game.coverUrl
                                if (displayCover.isNotBlank()) {
                                    val coverRequest = remember(context, displayCover) {
                                        ImageRequest.Builder(context)
                                            .data(displayCover)
                                            .crossfade(true)
                                            .build()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(width = 68.dp, height = 88.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.5.dp, EmeraldMint.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        AsyncImage(
                                            model = coverRequest,
                                            contentDescription = "Jaquette recadrée",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    val fallbackBitmap = capturedBitmap
                                    if (fallbackBitmap != null) {
                                        val imageBitmap = remember(fallbackBitmap) { fallbackBitmap.asImageBitmap() }
                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = "Photo capturée",
                                            modifier = Modifier
                                                .size(width = 68.dp, height = 88.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Auto-crop badge indicator & Interactive Crop/Rotate button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldMint.copy(alpha = 0.18f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Crop,
                                                    contentDescription = null,
                                                    tint = EmeraldMint,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Jaquette prête",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldMint
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = { openCoverEditor() },
                                            shape = RoundedCornerShape(6.dp),
                                            color = BrandPrimary.copy(alpha = 0.12f),
                                            modifier = Modifier.testTag("btn_crop_rotate_scan_photo")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Crop,
                                                    contentDescription = "Recadrer / Réorienter",
                                                    tint = BrandPrimary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Recadrer / Pivoter",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = game.format,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = game.completeness,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = game.region,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Cote estimée : ${String.format(Locale.FRANCE, "%.2f", game.estimatedValue)} €",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onGameScanned(game)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_confirm_import_photo"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importer ce jeu dans ma collection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(14.dp))

                // Testing without camera hardware (Emulator presets)
                Text(
                    text = "Tester la reconnaissance de cartouche / boîte (Émulateur) :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Cliquez sur un exemple pour simuler une photo de cartouche ou de pochette",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoPhotoPresetCard(
                        title = "Super Mario 64",
                        subtitle = "Photo de Cartouche Nintendo 64 (Loose)",
                        badge = "Cartouche N64",
                        onClick = { applyDemoPreset("mario64_cartridge") }
                    )

                    DemoPhotoPresetCard(
                        title = "The Legend of Zelda: Ocarina of Time",
                        subtitle = "Photo de Boîte / Pochette originale dorée (CIB)",
                        badge = "Boîte CIB",
                        onClick = { applyDemoPreset("zelda_box") }
                    )

                    DemoPhotoPresetCard(
                        title = "Chrono Trigger",
                        subtitle = "Photo de Boîte Super Nintendo US (Collector)",
                        badge = "Cartouche SNES",
                        onClick = { applyDemoPreset("chrono_trigger") }
                    )

                    DemoPhotoPresetCard(
                        title = "Silent Hill 2",
                        subtitle = "Photo de Jaquette PS2 originale (PAL)",
                        badge = "Boîtier DVD PS2",
                        onClick = { applyDemoPreset("silenthill2_dvd") }
                    )
                }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (isEditingCoverPhoto) {
        val bitmapToEdit = editorInitialBitmap ?: capturedBitmap
        if (bitmapToEdit != null) {
            CoverPhotoEditorDialog(
                initialBitmap = bitmapToEdit,
                onDismiss = {
                    isEditingCoverPhoto = false
                    editorInitialBitmap = null
                },
                onConfirm = { editedBitmap ->
                    capturedBitmap = editedBitmap
                    val targetTitle = recognizedGame?.title ?: "photo_scan"
                    isEditingCoverPhoto = false
                    editorInitialBitmap = null
                    coroutineScope.launch {
                        val savedUrl = withContext(Dispatchers.IO) {
                            GameCoverStorage.saveCoverBitmap(
                                context,
                                editedBitmap,
                                targetTitle
                            )
                        }
                        if (recognizedGame != null && savedUrl.isNotBlank()) {
                            recognizedGame = recognizedGame?.copy(coverUrl = savedUrl)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DemoPhotoPresetCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("demo_preset_${title.replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BrandPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Backwards compatibility alias for PhotoScanDialog.
 */
@Composable
fun QrScanDialog(
    onDismiss: () -> Unit,
    onGameScanned: (GameItem) -> Unit
) {
    PhotoScanDialog(onDismiss = onDismiss, onGameScanned = onGameScanned)
}
