package com.anongram.app.features.chats.mediaeditor.ui.paint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.BrushType

@Composable
fun BrushControlsBar(
    selectedBrush: BrushType,
    onSelectBrush: (BrushType) -> Unit,
    strokeWidthNorm: Float,
    onStrokeWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current

    val brushes: List<Triple<BrushType, String, ImageVector>> = listOf(
        Triple(BrushType.PEN, strings.brushPen, Icons.Rounded.Create),
        Triple(BrushType.ARROW, strings.brushArrow, Icons.AutoMirrored.Rounded.TrendingFlat),
        Triple(BrushType.MARKER, strings.brushMarker, Icons.Rounded.Highlight),
        Triple(BrushType.NEON, strings.brushNeon, Icons.Rounded.AutoFixHigh),
        Triple(BrushType.ERASER, strings.brushEraser, Icons.Rounded.CleaningServices),
        Triple(BrushType.BLUR, strings.brushBlur, Icons.Rounded.BlurOn)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Stroke thickness slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.brushSize,
                color = scheme.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
            Slider(
                value = strokeWidthNorm,
                onValueChange = onStrokeWidthChange,
                valueRange = 0.005f..0.055f,
                colors = SliderDefaults.colors(
                    thumbColor = scheme.accent,
                    activeTrackColor = scheme.accent,
                    inactiveTrackColor = scheme.textMuted.copy(alpha = 0.25f)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Brush types row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            brushes.forEach { (type, label, icon) ->
                val selected = type == selectedBrush
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) scheme.accent else scheme.card)
                        .clickable { onSelectBrush(type) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) scheme.onAccent else scheme.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        color = if (selected) scheme.onAccent else scheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
