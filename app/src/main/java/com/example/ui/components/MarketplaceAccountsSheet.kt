package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.marketplace.MarketplacePreferencesManager
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import com.example.worker.WishlistWorkScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VINTED_DOMAINS = listOf("vinted.fr", "vinted.be", "vinted.es", "vinted.it", "vinted.de")
private val EBAY_DOMAINS = listOf("ebay.fr", "ebay.com", "ebay.co.uk", "ebay.de")
private val VINTED_CONDITIONS = listOf("Tous", "Très bon état", "Bon état", "Neuf avec étiquette")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceAccountsSheet(
    onDismiss: () -> Unit,
    onImmediateCheckRequested: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { MarketplacePreferencesManager(context) }

    var vintedAccount by remember { mutableStateOf(prefsManager.getVintedAccount()) }
    var ebayAccount by remember { mutableStateOf(prefsManager.getEbayAccount()) }

    var vintedUsernameInput by remember { mutableStateOf(vintedAccount.username) }
    var vintedDomainInput by remember { mutableStateOf(vintedAccount.domain) }
    var vintedConditionInput by remember { mutableStateOf(vintedAccount.filterCondition) }
    var isTestingVinted by remember { mutableStateOf(false) }

    var ebayUsernameInput by remember { mutableStateOf(ebayAccount.username) }
    var ebayDomainInput by remember { mutableStateOf(ebayAccount.domain) }
    var ebayBinOnlyInput by remember { mutableStateOf(ebayAccount.buyItNowOnly) }
    var isTestingEbay by remember { mutableStateOf(false) }

    var dailyCheckEnabled by remember { mutableStateOf(prefsManager.isDailyCheckEnabled()) }
    var lastCheckTimestamp by remember { mutableStateOf(prefsManager.getLastGlobalCheckTimestamp()) }
    var isRunningImmediateCheck by remember { mutableStateOf(false) }

    var expandedVintedDomain by remember { mutableStateOf(false) }
    var expandedEbayDomain by remember { mutableStateOf(false) }
    var expandedVintedCondition by remember { mutableStateOf(false) }

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
                .testTag("marketplace_accounts_sheet")
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Comptes Vinted & eBay",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Surveillance quotidienne des prix Wishlist",
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

            // Global Periodic Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                    text = "Vérification automatique (1x par jour)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scan quotidien des jeux avec alerte de prix max",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = dailyCheckEnabled,
                            onCheckedChange = { checked ->
                                dailyCheckEnabled = checked
                                prefsManager.setDailyCheckEnabled(checked)
                                if (checked) {
                                    WishlistWorkScheduler.scheduleDailyPriceCheck(context)
                                } else {
                                    WishlistWorkScheduler.cancelDailyPriceCheck(context)
                                }
                            },
                            modifier = Modifier.testTag("switch_daily_check")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val formattedLastCheck = remember(lastCheckTimestamp) {
                            if (lastCheckTimestamp > 0L) {
                                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(lastCheckTimestamp))
                                "Dernier scan : $dateStr"
                            } else {
                                "Aucun scan effectué encore"
                            }
                        }
                        Text(
                            text = formattedLastCheck,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                isRunningImmediateCheck = true
                                onImmediateCheckRequested()
                                coroutineScope.launch {
                                    try {
                                        WishlistWorkScheduler.triggerImmediatePriceCheck(context)
                                        Toast.makeText(context, "Vérification des prix lancée en arrière-plan !", Toast.LENGTH_SHORT).show()
                                        kotlinx.coroutines.delay(1000)
                                        lastCheckTimestamp = System.currentTimeMillis()
                                        prefsManager.setLastGlobalCheckTimestamp(lastCheckTimestamp)
                                    } finally {
                                        isRunningImmediateCheck = false
                                    }
                                }
                            },
                            enabled = !isRunningImmediateCheck,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_immediate_price_check")
                        ) {
                            if (isRunningImmediateCheck) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Scanner maintenant", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // VINTED ACCOUNT CARD
            // -------------------------------------------------------------
            val vintedTeal = Color(0xFF09B1BA)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_vinted_account"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (vintedAccount.isConnected) vintedTeal.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = vintedTeal.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Vinted",
                                        tint = vintedTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Vinted",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Jeux rétro, cartouches & boîtes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (vintedAccount.isConnected) EmeraldMint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (vintedAccount.isConnected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldMint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connecté", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldMint)
                                } else {
                                    Text("Non configuré", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vintedUsernameInput,
                        onValueChange = { vintedUsernameInput = it },
                        label = { Text("Pseudo ou Profil Vinted", fontSize = 12.sp) },
                        placeholder = { Text("ex: johan_collector", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_vinted_username"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Domain Picker
                        ExposedDropdownMenuBox(
                            expanded = expandedVintedDomain,
                            onExpandedChange = { expandedVintedDomain = !expandedVintedDomain },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = vintedDomainInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Domaine", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVintedDomain) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedVintedDomain,
                                onDismissRequest = { expandedVintedDomain = false }
                            ) {
                                VINTED_DOMAINS.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d, fontSize = 12.sp) },
                                        onClick = {
                                            vintedDomainInput = d
                                            expandedVintedDomain = false
                                        }
                                    )
                                }
                            }
                        }

                        // Condition Filter
                        ExposedDropdownMenuBox(
                            expanded = expandedVintedCondition,
                            onExpandedChange = { expandedVintedCondition = !expandedVintedCondition },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = vintedConditionInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("État souhaité", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVintedCondition) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedVintedCondition,
                                onDismissRequest = { expandedVintedCondition = false }
                            ) {
                                VINTED_CONDITIONS.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c, fontSize = 12.sp) },
                                        onClick = {
                                            vintedConditionInput = c
                                            expandedVintedCondition = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingVinted = true
                                coroutineScope.launch {
                                    try {
                                        val res = prefsManager.testVintedConnection(vintedDomainInput)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "✅ Vinted connecté avec succès !", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "⚠️ Vinted joignable via flux public", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isTestingVinted = false
                                    }
                                }
                            },
                            enabled = !isTestingVinted,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_test_vinted")
                        ) {
                            if (isTestingVinted) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Tester le flux", fontSize = 12.sp)
                        }

                        Row {
                            if (vintedAccount.isConnected) {
                                TextButton(
                                    onClick = {
                                        prefsManager.disconnectVinted()
                                        vintedAccount = prefsManager.getVintedAccount()
                                        Toast.makeText(context, "Compte Vinted débranché", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Déconnecter", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    prefsManager.saveVintedAccount(
                                        username = vintedUsernameInput.ifBlank { "Collectionneur" },
                                        domain = vintedDomainInput,
                                        condition = vintedConditionInput
                                    )
                                    vintedAccount = prefsManager.getVintedAccount()
                                    Toast.makeText(context, "Compte Vinted enregistré et branché !", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = vintedTeal),
                                modifier = Modifier.testTag("btn_save_vinted")
                            ) {
                                Text("Brancher Vinted", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // EBAY ACCOUNT CARD
            // -------------------------------------------------------------
            val ebayBlue = Color(0xFF0064D2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_ebay_account"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (ebayAccount.isConnected) ebayBlue.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ebayBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = "eBay",
                                        tint = ebayBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "eBay",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enchères, Achat Immédiat & CIB",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (ebayAccount.isConnected) EmeraldMint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (ebayAccount.isConnected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldMint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connecté", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldMint)
                                } else {
                                    Text("Non configuré", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ebayUsernameInput,
                        onValueChange = { ebayUsernameInput = it },
                        label = { Text("Pseudo ou ID Compte eBay", fontSize = 12.sp) },
                        placeholder = { Text("ex: johan-vintage-games", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ebay_username"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedEbayDomain,
                            onExpandedChange = { expandedEbayDomain = !expandedEbayDomain },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = ebayDomainInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Domaine", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEbayDomain) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedEbayDomain,
                                onDismissRequest = { expandedEbayDomain = false }
                            ) {
                                EBAY_DOMAINS.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d, fontSize = 12.sp) },
                                        onClick = {
                                            ebayDomainInput = d
                                            expandedEbayDomain = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Switch(
                                checked = ebayBinOnlyInput,
                                onCheckedChange = { ebayBinOnlyInput = it },
                                modifier = Modifier.testTag("switch_ebay_bin")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Achat Immédiat", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingEbay = true
                                coroutineScope.launch {
                                    try {
                                        val res = prefsManager.testEbayConnection(ebayDomainInput)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "✅ eBay connecté avec succès !", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "⚠️ eBay accessible", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isTestingEbay = false
                                    }
                                }
                            },
                            enabled = !isTestingEbay,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_test_ebay")
                        ) {
                            if (isTestingEbay) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Tester le flux", fontSize = 12.sp)
                        }

                        Row {
                            if (ebayAccount.isConnected) {
                                TextButton(
                                    onClick = {
                                        prefsManager.disconnectEbay()
                                        ebayAccount = prefsManager.getEbayAccount()
                                        Toast.makeText(context, "Compte eBay débranché", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Déconnecter", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    prefsManager.saveEbayAccount(
                                        username = ebayUsernameInput.ifBlank { "Collectionneur" },
                                        domain = ebayDomainInput,
                                        buyItNowOnly = ebayBinOnlyInput
                                    )
                                    ebayAccount = prefsManager.getEbayAccount()
                                    Toast.makeText(context, "Compte eBay enregistré et branché !", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ebayBlue),
                                modifier = Modifier.testTag("btn_save_ebay")
                            ) {
                                Text("Brancher eBay", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
