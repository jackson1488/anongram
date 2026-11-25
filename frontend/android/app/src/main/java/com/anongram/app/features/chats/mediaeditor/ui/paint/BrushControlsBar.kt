package com.anongram.app.features.chats.mediaeditor.ui.paint

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.BrushType
import com.anongram.app.features.chats.mediaeditor.model.EditorPalette

@Composable
fun BrushControlsBar(
    selectedBrush: BrushType,
    onSelectBrush: (BrushType) -> Unit,
    selectedColorArgb: Long,
    onSelectColor: (Long) -> Unit,
    onOpenFullColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current

    var showColorGrid by remember { mutableStateOf(false) }

    val brushes: List<Triple<BrushType, String, ImageVector>> = listOf(
        Triple(BrushType.PEN, strings.brushPen, Icons.Rounded.Create),
        Triple(BrushType.ARROW, strings.brushArrow, Icons.AutoMirrored.Rounded.TrendingFlat),
        Triple(BrushType.MARKER, strings.brushMarker, Icons.Rounded.Highlight),
        Triple(BrushType.NEON, strings.brushNeon, Icons.Rounded.AutoFixHigh),
        Triple(BrushType.BLUR, strings.brushBlur, Icons.Rounded.BlurOn),
        Triple(BrushType.ERASER, strings.brushEraser, Icons.Rounded.CleaningServices)
    )

    AnimatedContent(
        targetState = showColorGrid,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "brushOrPaletteCapsule",
        modifier = modifier.fillMaxWidth()
    ) { isGrid ->
        if (isGrid) {
            ColorPaletteBar(
                selectedColorArgb = selectedColorArgb,
                onSelectColor = { onSelectColor(it) },
                onBackToBrushes = { showColorGrid = false },
                onOpenFullPicker = onOpenFullColorPicker
            )
        } else {
            // Single compact capsule: [Color circle] [Pen] [Arrow] [Marker] [Neon] [Blur] [Eraser] [+]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.islandBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active color circle button -> animates into 2x7 palette grid
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EditorPalette.colorFromArgb(selectedColorArgb))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { showColorGrid = true }
                )

                brushes.forEach { (type, label, icon) ->
                    val selected = type == selectedBrush
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (selected) scheme.card else Color.Transparent)
                            .clickable { onSelectBrush(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) Color.White else scheme.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // (+) opens full 3-tab color picker sheet
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onOpenFullColorPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.customColor,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
