package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import com.example.util.CsvExportUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class ExportScope(val label: String) {
    OWNED("Jeux possédés"),
    WISHLIST("Wishlist"),
    ALL("Tous les jeux")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCollectionSheet(
    allGames: List<GameItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var exportScope by remember { mutableStateOf(ExportScope.OWNED) }
    var selectedPlatform by remember { mutableStateOf("Toutes") }
    var isPlatformDropdownOpen by remember { mutableStateOf(false) }
    var delimiter by remember { mutableStateOf(',') } // ',' for Google Drive / Google Sheets, ';' for Excel FR
    var isPreviewExpanded by remember { mutableStateOf(false) }

    val ownedCount = remember(allGames) { allGames.count { !it.isWishlist } }
    val wishlistCount = allGames.size - ownedCount

    // Platform options from available games
    val platforms = remember(allGames) {
        val set = allGames.map { it.platform }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("Toutes") + set
    }

    // Filter games according to user selection
    val filteredGames by remember(allGames, exportScope, selectedPlatform) {
        derivedStateOf {
            allGames.filter { game ->
                val scopeMatch = when (exportScope) {
                    ExportScope.OWNED -> !game.isWishlist
                    ExportScope.WISHLIST -> game.isWishlist
                    ExportScope.ALL -> true
                }
                val platformMatch = selectedPlatform == "Toutes" || game.platform == selectedPlatform
                scopeMatch && platformMatch
            }
        }
    }

    val totalEstimatedValue by remember(filteredGames) {
        derivedStateOf {
            filteredGames.sumOf { it.estimatedValue }
        }
    }

    // Generated CSV string based on active delimiter
    val generatedCsv by remember(filteredGames, delimiter) {
        derivedStateOf {
            CsvExportUtil.generateCsv(filteredGames, delimiter)
        }
    }

    val defaultFileName = remember(exportScope) {
        val scopeKey = when (exportScope) {
            ExportScope.OWNED -> "owned"
            ExportScope.WISHLIST -> "wishlist"
            ExportScope.ALL -> "all"
        }
        CsvExportUtil.generateDefaultFileName(scopeKey)
    }

    // Storage Access Framework launcher to save CSV directly to device
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val csvSnapshot = generatedCsv
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    CsvExportUtil.writeCsvToUri(context, uri, csvSnapshot)
                }
                if (success) {
                    Toast.makeText(
                        context,
                        "Fichier CSV exporté avec succès sur votre appareil !",
                        Toast.LENGTH_LONG
                    ).show()
                    onDismiss()
                } else {
                    Toast.makeText(
                        context,
                        "Erreur lors de l'enregistrement du fichier.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("export_collection_sheet")
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
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Exporter la collection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Format tableau tableur (.CSV)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("export_dialog_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = BrandPrimary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Jeux prêts pour l'export",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${filteredGames.size} jeux",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = BrandPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Valeur estimée totale",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.FRANCE, "%.2f €", totalEstimatedValue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = EmeraldMint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 1: Périmètre de l'export
            Text(
                text = "1. Périmètre de l'exportation",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportScope.entries.forEach { scope ->
                    val selected = exportScope == scope
                    val count = when (scope) {
                        ExportScope.OWNED -> ownedCount
                        ExportScope.WISHLIST -> wishlistCount
                        ExportScope.ALL -> allGames.size
                    }

                    FilterChip(
                        selected = selected,
                        onClick = { exportScope = scope },
                        label = { Text("${scope.label} ($count)") },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("export_scope_${scope.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2: Filtrer par machine & Délimiteur
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Platform filter
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { isPlatformDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_platform_filter_button")
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
                                    text = "Console",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedPlatform,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isPlatformDropdownOpen,
                        onDismissRequest = { isPlatformDropdownOpen = false }
                    ) {
                        platforms.forEach { platform ->
                            DropdownMenuItem(
                                text = { Text(platform) },
                                onClick = {
                                    selectedPlatform = platform
                                    isPlatformDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Delimiter Selector
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Format séparateur",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (delimiter == ',') "Virgule (Drive)" else "Point-virgule (Excel)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { delimiter = if (delimiter == ',') ';' else ',' },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = delimiter.toString(),
                                    fontWeight = FontWeight.Black,
                                    color = BrandPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 3: Destination de l'export
            Text(
                text = "2. Choisissez votre destination d'export",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Option A: Sur mon appareil
            ExportDestinationCard(
                title = "Enregistrer sur mon appareil",
                subtitle = "Enregistre le fichier CSV directement dans vos Téléchargements ou Documents locaux via le sélecteur Android.",
                icon = Icons.Default.Download,
                iconTint = BrandPrimary,
                testTag = "export_action_device",
                onClick = {
                    if (filteredGames.isEmpty()) {
                        Toast.makeText(context, "Aucun jeu à exporter.", Toast.LENGTH_SHORT).show()
                        return@ExportDestinationCard
                    }
                    createDocumentLauncher.launch(defaultFileName)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option B: Google Drive
            ExportDestinationCard(
                title = "Exporter vers mon compte Google Drive",
                subtitle = "Envoi direct vers votre Google Drive (choix du dossier de destination dans Drive ou via l'application Google Drive).",
                icon = Icons.Default.CloudUpload,
                iconTint = EmeraldMint,
                testTag = "export_action_drive",
                onClick = {
                    if (filteredGames.isEmpty()) {
                        Toast.makeText(context, "Aucun jeu à exporter.", Toast.LENGTH_SHORT).show()
                        return@ExportDestinationCard
                    }
                    val csvSnapshot = generatedCsv
                    coroutineScope.launch {
                        val fileUri = withContext(Dispatchers.IO) {
                            CsvExportUtil.createCacheCsvFile(context, csvSnapshot, "gamedex_drive_export")
                        }
                        if (fileUri != null) {
                            val opened = CsvExportUtil.openGoogleDriveExport(context, fileUri)
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    "Impossible d'ouvrir Google Drive directement. Utilisez l'option Partager ou Enregistrer sur l'appareil.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "Erreur de préparation du fichier CSV.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option C: Partager / Autres applications
            ExportDestinationCard(
                title = "Partager le fichier CSV",
                subtitle = "Envoyer par e-mail, application de messagerie ou tout autre service cloud installé sur votre appareil.",
                icon = Icons.Default.Share,
                iconTint = AmberGold,
                testTag = "export_action_share",
                onClick = {
                    if (filteredGames.isEmpty()) {
                        Toast.makeText(context, "Aucun jeu à exporter.", Toast.LENGTH_SHORT).show()
                        return@ExportDestinationCard
                    }
                    val csvSnapshot = generatedCsv
                    coroutineScope.launch {
                        val fileUri = withContext(Dispatchers.IO) {
                            CsvExportUtil.createCacheCsvFile(context, csvSnapshot, "gamedex_export")
                        }
                        if (fileUri != null) {
                            CsvExportUtil.shareCsvFile(context, fileUri)
                        } else {
                            Toast.makeText(context, "Erreur de préparation du fichier CSV.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 4: Aperçu du tableau CSV & Copie
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPreviewExpanded = !isPreviewExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aperçu & Copier le texte CSV",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Icon(
                            imageVector = if (isPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isPreviewExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .horizontalScroll(rememberScrollState())
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = generatedCsv.take(2000) + if (generatedCsv.length > 2000) "\n... (+${generatedCsv.length - 2000} caractères)" else "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    CsvExportUtil.copyCsvToClipboard(context, generatedCsv)
                                    Toast.makeText(context, "Texte CSV copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_copy_clipboard_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier tout le CSV")
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
private fun ExportDestinationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
