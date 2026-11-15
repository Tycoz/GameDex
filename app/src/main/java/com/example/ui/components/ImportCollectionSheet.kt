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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import com.example.util.CsvImportUtil
import com.example.util.CsvParseResult
import com.example.util.MultiFormatImportUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class ImportDestinationMode(val label: String) {
    ORIGINAL("Selon le CSV"),
    ALL_COLLECTION("Tous en Collection"),
    ALL_WISHLIST("Tous en Wishlist")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCollectionSheet(
    existingGames: List<GameItem>,
    onImportConfirmed: (List<GameItem>, onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var parseResult by remember { mutableStateOf<CsvParseResult?>(null) }
    var isPastingText by remember { mutableStateOf(false) }
    var pastedCsvText by remember { mutableStateOf("") }
    var isGoogleSheetsActive by remember { mutableStateOf(false) }
    var googleSheetUrlInput by remember { mutableStateOf("") }
    var isLoadingGoogleSheet by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isLoadingFile by remember { mutableStateOf(false) }
    var isParsingPastedText by remember { mutableStateOf(false) }
    var destinationMode by remember { mutableStateOf(ImportDestinationMode.ORIGINAL) }
    var skipExistingDuplicates by remember { mutableStateOf(true) }

    // Map of game index -> Boolean (selected for import)
    val selectionMap = remember(parseResult) {
        mutableStateMapOf<Int, Boolean>().apply {
            parseResult?.games?.indices?.forEach { put(it, true) }
        }
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }

    // Android Storage Access Framework picker for CSV / Excel / OpenDocument / Calendar files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoadingFile = true
            lastErrorMessage = null
            coroutineScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        MultiFormatImportUtil.parseFromUri(context, uri)
                    }
                    parseResult = result
                    if (result.games.isEmpty()) {
                        val err = result.errorMessages.firstOrNull() ?: "Aucun jeu détecté dans le fichier."
                        lastErrorMessage = err
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    } else {
                        lastErrorMessage = null
                    }
                } finally {
                    isLoadingFile = false
                }
            }
        }
    }

    // Existing games normalized key set for duplicate detection
    val existingKeys = remember(existingGames) {
        existingGames.map { "${it.title.trim().lowercase()}_${it.platform.trim().lowercase()}" }.toSet()
    }

    val finalGamesToImport by remember(parseResult, destinationMode, skipExistingDuplicates, selectionMap) {
        derivedStateOf {
            val raw = parseResult?.games ?: return@derivedStateOf emptyList<GameItem>()
            raw.filterIndexed { index, game ->
                val isSelected = selectionMap[index] ?: true
                if (!isSelected) return@filterIndexed false
                if (skipExistingDuplicates) {
                    val key = "${game.title.trim().lowercase()}_${game.platform.trim().lowercase()}"
                    if (existingKeys.contains(key)) return@filterIndexed false
                }
                true
            }.map { game ->
                when (destinationMode) {
                    ImportDestinationMode.ORIGINAL -> game
                    ImportDestinationMode.ALL_COLLECTION -> game.copy(isWishlist = false)
                    ImportDestinationMode.ALL_WISHLIST -> game.copy(isWishlist = true)
                }
            }
        }
    }

    val duplicateCount by remember(parseResult, existingKeys) {
        derivedStateOf {
            parseResult?.games?.count { game ->
                val key = "${game.title.trim().lowercase()}_${game.platform.trim().lowercase()}"
                existingKeys.contains(key)
            } ?: 0
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("import_collection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
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
                            .background(EmeraldMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = EmeraldMint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Importer une collection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Google Sheets, ODS/ODT, XLS/XLSX, VCS ou CSV",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("import_dialog_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val activeResult = parseResult
            if (activeResult == null || activeResult.games.isEmpty()) {
                // STEP 1: Choose File or Paste CSV Text
                Text(
                    text = "Sélectionnez votre source de données",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Error message banner if previous attempt failed
                if (lastErrorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = lastErrorMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Card A: Pick file from Device or Drive
                Card(
                    onClick = {
                        if (!isLoadingFile) {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "text/comma-separated-values",
                                    "text/csv",
                                    "text/plain",
                                    "text/calendar",
                                    "text/x-vcalendar",
                                    "application/csv",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.oasis.opendocument.spreadsheet",
                                    "application/vnd.oasis.opendocument.text",
                                    "*/*"
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BrandPrimary.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_pick_file_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choisir un fichier tableur ou calendrier",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Formats : .CSV, .XLS, .XLSX, .ODS, .ODT, .VCS (Local & Drive)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card B: Google Sheets via shared URL
                OutlinedCard(
                    onClick = { isGoogleSheetsActive = !isGoogleSheetsActive },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isGoogleSheetsActive) EmeraldMint.copy(alpha = 0.05f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_toggle_google_sheets_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = EmeraldMint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Importer depuis Google Sheets (URL)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Collez le lien partagé de votre feuille de calcul Google.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isGoogleSheetsActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = googleSheetUrlInput,
                            onValueChange = { googleSheetUrlInput = it },
                            label = { Text("Lien Google Sheets") },
                            placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = EmeraldMint)
                            },
                            trailingIcon = {
                                if (googleSheetUrlInput.isNotBlank()) {
                                    IconButton(onClick = { googleSheetUrlInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Effacer")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_google_sheet_url_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        googleSheetUrlInput = clipText.trim()
                                    } else {
                                        Toast.makeText(context, "Presse-papier vide.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Coller le lien", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (googleSheetUrlInput.isBlank()) {
                                        Toast.makeText(context, "Veuillez coller un lien Google Sheets.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isLoadingGoogleSheet = true
                                        lastErrorMessage = null
                                        coroutineScope.launch {
                                            try {
                                                val result = MultiFormatImportUtil.fetchAndParseGoogleSheet(googleSheetUrlInput)
                                                result.onSuccess { res ->
                                                    if (res.games.isEmpty()) {
                                                        lastErrorMessage = res.errorMessages.firstOrNull() ?: "Aucun jeu détecté dans la feuille."
                                                    } else {
                                                        parseResult = res
                                                        lastErrorMessage = null
                                                    }
                                                }.onFailure { err ->
                                                    lastErrorMessage = err.localizedMessage ?: "Erreur d'importation Google Sheets"
                                                    Toast.makeText(context, lastErrorMessage, Toast.LENGTH_LONG).show()
                                                }
                                            } finally {
                                                isLoadingGoogleSheet = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoadingGoogleSheet && googleSheetUrlInput.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_load_google_sheet_button")
                            ) {
                                if (isLoadingGoogleSheet) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chargement...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Charger", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Note : La feuille Google Sheet doit être accessible en lecture (Fichier > Partager > Accès général : Tous les utilisateurs disposant du lien).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card B: Paste CSV text
                OutlinedCard(
                    onClick = { isPastingText = !isPastingText },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_toggle_paste_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Coller du texte CSV brut",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Copiez un tableau depuis Excel, Sheets ou Notes et collez-le ici.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isPastingText) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pastedCsvText = """
                                        Titre,Plateforme,Genre,Année,Éditeur,Prix d'achat,Valeur estimée,Statut Collection
                                        Super Mario 64,Nintendo 64,Plateforme,1996,Nintendo,35.00,85.00,Collection
                                        The Legend of Zelda: Ocarina of Time,Nintendo 64,Action-Aventure,1998,Nintendo,45.00,95.00,Collection
                                        Pokémon Version Rouge,Game Boy,RPG,1996,Nintendo,25.00,70.00,Collection
                                        Castlevania: Symphony of the Night,PlayStation,Action-RPG,1997,Konami,50.00,160.00,Wishlist
                                    """.trimIndent()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("import_insert_sample_button")
                            ) {
                                Text("Insérer exemple", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val template = "Titre,Plateforme,Genre,Année,Éditeur,Format,Région,Complétude,Statut,Note (/5),Prix d'achat (€),Valeur estimée (€),Statut Collection,Notes & Remarques"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(template))
                                    Toast.makeText(context, "Modèle d'en-têtes copié !", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("import_copy_template_button")
                            ) {
                                Text("Copier modèle", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pastedCsvText,
                            onValueChange = { pastedCsvText = it },
                            placeholder = {
                                Text(
                                    "Titre,Plateforme,Année,Prix d'achat\nSuper Mario World,Super Nintendo,1990,45.00\nZelda Ocarina,Nintendo 64,1998,50.00",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("import_paste_textfield"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (pastedCsvText.isNotBlank() && !isParsingPastedText) {
                                    isParsingPastedText = true
                                    val textToParse = pastedCsvText
                                    coroutineScope.launch {
                                        try {
                                            val result = withContext(Dispatchers.IO) {
                                                CsvImportUtil.parseCsvString(textToParse)
                                            }
                                            parseResult = result
                                            if (result.games.isEmpty()) {
                                                val err = result.errorMessages.firstOrNull() ?: "Aucun jeu détecté."
                                                lastErrorMessage = err
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            } else {
                                                lastErrorMessage = null
                                            }
                                        } finally {
                                            isParsingPastedText = false
                                        }
                                    }
                                }
                            },
                            enabled = pastedCsvText.isNotBlank() && !isParsingPastedText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_parse_pasted_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text(if (isParsingPastedText) "Analyse..." else "Analyser le texte CSV")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // STEP 2: Preview & Configuration
                val result = activeResult

                // Summary banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldMint.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = EmeraldMint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = result.fileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val formatLabel = when {
                                result.fileName.endsWith(".ods", true) || result.fileName.endsWith(".odt", true) -> "OpenDocument"
                                result.fileName.endsWith(".xls", true) || result.fileName.endsWith(".xlsx", true) -> "Excel"
                                result.fileName.endsWith(".vcs", true) || result.fileName.endsWith(".ics", true) -> "vCalendar / VCS"
                                result.fileName.contains("google_sheet", true) -> "Google Sheets"
                                else -> "Séparateur: '${result.detectedDelimiter}'"
                            }
                            Text(
                                text = formatLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${result.games.size} jeux détectés",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldMint,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Prêts à importer : ${finalGamesToImport.size}",
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                fontSize = 14.sp
                            )
                        }

                        val totalEstimatedBatch = remember(finalGamesToImport) {
                            finalGamesToImport.sumOf { it.estimatedValue }
                        }
                        if (totalEstimatedBatch > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.FRANCE, "Valeur estimée du lot : %.2f €", totalEstimatedBatch),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldMint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination options
                Text(
                    text = "Destination dans GameDex",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImportDestinationMode.entries.forEach { mode ->
                        val isSelected = destinationMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { destinationMode = mode },
                            label = { Text(mode.label, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("import_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duplicate management
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ignorer les doublons déjà présents",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (duplicateCount > 0) {
                            Text(
                                text = "$duplicateCount jeu(x) déjà dans votre collection",
                                fontSize = 11.sp,
                                color = AmberGold
                            )
                        }
                    }
                    Switch(
                        checked = skipExistingDuplicates,
                        onCheckedChange = { skipExistingDuplicates = it },
                        modifier = Modifier.testTag("import_skip_duplicates_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val duplicateFlags = remember(result, existingKeys) {
                    result.games.map { game ->
                        existingKeys.contains("${game.title.trim().lowercase()}_${game.platform.trim().lowercase()}")
                    }
                }
                val allPreviewSelected by remember(result, selectionMap) {
                    derivedStateOf {
                        result.games.indices.all { selectionMap[it] ?: true }
                    }
                }

                // Games Preview List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Aperçu des jeux (${finalGamesToImport.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (allPreviewSelected) "Tout désélectionner" else "Tout sélectionner",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        modifier = Modifier
                            .clickable {
                                val nextSelected = !allPreviewSelected
                                result.games.indices.forEach { selectionMap[it] = nextSelected }
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(
                        count = result.games.size,
                        key = { index -> "${index}_${result.games[index].title}_${result.games[index].platform}" }
                    ) { index ->
                        val game = result.games[index]
                        val isSelected = selectionMap[index] ?: true
                        val isDuplicate = duplicateFlags[index]

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDuplicate && skipExistingDuplicates)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected && (!isDuplicate || !skipExistingDuplicates),
                                    onCheckedChange = { selectionMap[index] = it },
                                    enabled = !isDuplicate || !skipExistingDuplicates,
                                    colors = CheckboxDefaults.colors(checkedColor = BrandPrimary),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = game.platform,
                                            fontSize = 11.sp,
                                            color = BrandPrimary
                                        )
                                        if (game.releaseYear > 0) {
                                            Text(
                                                text = " • ${game.releaseYear}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (game.estimatedValue > 0) {
                                            Text(
                                                text = String.format(Locale.FRANCE, " • %.2f €", game.estimatedValue),
                                                fontSize = 11.sp,
                                                color = EmeraldMint,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (isDuplicate) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AmberGold.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Doublon",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Import vs Change File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { parseResult = null },
                        enabled = !isImporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_change_file_button")
                    ) {
                        Text("Changer")
                    }

                    Button(
                        onClick = {
                            if (finalGamesToImport.isNotEmpty()) {
                                isImporting = true
                                onImportConfirmed(finalGamesToImport) { success ->
                                    isImporting = false
                                    if (!success) {
                                        lastErrorMessage = "L'importation a échoué. Veuillez réessayer."
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Aucun jeu sélectionné pour l'import.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isImporting && finalGamesToImport.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("import_confirm_button")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importation...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importer (${finalGamesToImport.size})")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
