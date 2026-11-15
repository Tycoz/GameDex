package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EmeraldMint
import com.example.util.GameCoverCropUtil
import kotlin.math.roundToInt

enum class CoverAspectRatioPreset(val label: String, val ratio: Float, val subtitle: String) {
    FREE("Libre", 0f, "Manuel"),
    RATIO_3_4("3:4", 0.75f, "Switch / PS / Xbox"),
    RATIO_4_3("4:3", 1.33f, "SNES / GBA rétro"),
    RATIO_1_1("1:1", 1.0f, "Game Boy / PS1 CD")
}

/**
 * Interactive Dialog for cropping, rotating, flipping, and framing a video game cover photo.
 */
@Composable
fun CoverPhotoEditorDialog(
    initialBitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    // Current working bitmap state
    var currentWorkingBitmap by remember(initialBitmap) { mutableStateOf(initialBitmap) }

    // History for reset
    val originalSourceBitmap = remember(initialBitmap) { initialBitmap }

    // Selected ratio preset
    var selectedPreset by remember { mutableStateOf(CoverAspectRatioPreset.FREE) }

    // Manual margin adjustments (0% to 25%)
    var horizontalMarginPercent by remember { mutableFloatStateOf(0f) }
    var verticalMarginPercent by remember { mutableFloatStateOf(0f) }

    // Track total rotation for UI badge
    var currentRotationDegrees by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("cover_photo_editor_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Recadrer & Réorienter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ajustez la jaquette avant import",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_crop_editor")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Image Preview Frame
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("editor_image_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.05f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewImageBitmap = remember(currentWorkingBitmap) {
                            currentWorkingBitmap.asImageBitmap()
                        }
                        Image(
                            bitmap = previewImageBitmap,
                            contentDescription = "Aperçu de la jaquette",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )

                        // Dimension and orientation badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${currentWorkingBitmap.width} × ${currentWorkingBitmap.height} px (${currentRotationDegrees}°)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Réorienter (Orientation & Rotation)
                Text(
                    text = "1. Réorienter l'image",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentWorkingBitmap = GameCoverCropUtil.rotateBitmap(currentWorkingBitmap, -90f)
                            currentRotationDegrees = (currentRotationDegrees - 90 + 360) % 360
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_rotate_left"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-90°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            currentWorkingBitmap = GameCoverCropUtil.rotateBitmap(currentWorkingBitmap, 90f)
                            currentRotationDegrees = (currentRotationDegrees + 90) % 360
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_rotate_right"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+90°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            currentWorkingBitmap = GameCoverCropUtil.flipBitmapHorizontal(currentWorkingBitmap)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_flip_horizontal"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Miroir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Recadrer (Aspect ratio & Smart Auto-crop)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Format & Recadrage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Quick Smart Auto-Crop Button
                    Surface(
                        onClick = {
                            currentWorkingBitmap = GameCoverCropUtil.autoCropGameCover(currentWorkingBitmap)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldMint.copy(alpha = 0.15f),
                        modifier = Modifier.testTag("btn_auto_crop_smart")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldMint,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-recadrage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldMint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Aspect Ratio Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CoverAspectRatioPreset.entries.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                if (preset.ratio > 0f) {
                                    currentWorkingBitmap = GameCoverCropUtil.cropToTargetRatio(currentWorkingBitmap, preset.ratio)
                                }
                            },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = preset.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_ratio_${preset.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Fine Adjustment Sliders (Rogner les bordures)
                Text(
                    text = "Ajustement fin des marges :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Horizontal crop slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Côtés : ${(horizontalMarginPercent * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        modifier = Modifier.width(68.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = horizontalMarginPercent,
                        onValueChange = { horizontalMarginPercent = it },
                        valueRange = 0f..0.20f,
                        steps = 7,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("slider_crop_horizontal"),
                        colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                    )
                }

                // Vertical crop slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Haut/Bas : ${(verticalMarginPercent * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        modifier = Modifier.width(68.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = verticalMarginPercent,
                        onValueChange = { verticalMarginPercent = it },
                        valueRange = 0f..0.20f,
                        steps = 7,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("slider_crop_vertical"),
                        colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                    )
                }

                if (horizontalMarginPercent > 0.005f || verticalMarginPercent > 0.005f) {
                    OutlinedButton(
                        onClick = {
                            currentWorkingBitmap = GameCoverCropUtil.cropByPercent(
                                currentWorkingBitmap,
                                leftPercent = horizontalMarginPercent,
                                topPercent = verticalMarginPercent,
                                rightPercent = horizontalMarginPercent,
                                bottomPercent = verticalMarginPercent
                            )
                            horizontalMarginPercent = 0f
                            verticalMarginPercent = 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("btn_apply_margin_crop"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Appliquer ce rognage des marges", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            currentWorkingBitmap = originalSourceBitmap
                            currentRotationDegrees = 0
                            selectedPreset = CoverAspectRatioPreset.FREE
                            horizontalMarginPercent = 0f
                            verticalMarginPercent = 0f
                        },
                        modifier = Modifier.testTag("btn_reset_editor"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Réinitialiser la photo", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom confirmation actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_cancel_crop_editor"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Annuler", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            // If margins were set, apply them to final output
                            val finalBitmap = if (horizontalMarginPercent > 0.005f || verticalMarginPercent > 0.005f) {
                                GameCoverCropUtil.cropByPercent(
                                    currentWorkingBitmap,
                                    leftPercent = horizontalMarginPercent,
                                    topPercent = verticalMarginPercent,
                                    rightPercent = horizontalMarginPercent,
                                    bottomPercent = verticalMarginPercent
                                )
                            } else {
                                currentWorkingBitmap
                            }
                            onConfirm(finalBitmap)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_confirm_crop_editor"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Valider la jaquette", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
