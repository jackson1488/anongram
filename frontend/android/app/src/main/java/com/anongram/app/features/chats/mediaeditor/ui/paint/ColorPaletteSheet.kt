package com.anongram.app.features.chats.mediaeditor.ui.paint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.EditorPalette

@Composable
fun ColorPaletteBar(
    selectedColorArgb: Long,
    onSelectColor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    var showCustomPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom color (+) button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(scheme.card)
                .border(1.5.dp, scheme.islandBorder, CircleShape)
                .clickable { showCustomPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = strings.customColor,
                tint = scheme.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        EditorPalette.quickColors.forEach { argb ->
            val selected = argb == selectedColorArgb
            Box(
                modifier = Modifier
                    .size(if (selected) 30.dp else 26.dp)
                    .clip(CircleShape)
                    .background(EditorPalette.colorFromArgb(argb))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) scheme.accent else Color.White.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(argb) }
            )
        }
    }

    if (showCustomPicker) {
        CustomColorDialog(
            initialArgb = selectedColorArgb,
            onConfirm = { newArgb ->
                onSelectColor(newArgb)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

@Composable
private fun CustomColorDialog(
    initialArgb: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val initColor = android.graphics.Color.valueOf(initialArgb.toInt())
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb.toInt(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1].coerceAtLeast(0.2f)) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.4f)) }

    val previewInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val previewLong = previewInt.toLong() and 0xFFFFFFFFL

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.card,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(strings.customColor, color = scheme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(previewInt))
                        .border(2.dp, scheme.islandBorder, CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = scheme.accent, activeTrackColor = scheme.accent)
                )
                Slider(
                    value = sat,
                    onValueChange = { sat = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = scheme.accent, activeTrackColor = scheme.accent)
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = scheme.accent, activeTrackColor = scheme.accent)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(previewLong) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.accent)
            ) {
                Text(strings.done, color = scheme.onAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = scheme.textSecondary)
            }
        }
    )
}
