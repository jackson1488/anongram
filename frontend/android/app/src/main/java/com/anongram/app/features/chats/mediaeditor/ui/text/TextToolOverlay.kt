package com.anongram.app.features.chats.mediaeditor.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.engine.EditorFontResolver
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.EditorFontSpec
import com.anongram.app.features.chats.mediaeditor.model.EditorPalette
import com.anongram.app.features.chats.mediaeditor.model.TextAlignOption
import com.anongram.app.features.chats.mediaeditor.model.TextBackgroundStyle
import com.anongram.app.features.chats.mediaeditor.model.TextLayerItem
import kotlin.math.roundToInt

@Composable
fun TextLayersCanvasOverlay(
    layers: List<TextLayerItem>,
    selectedLayerId: String?,
    interactive: Boolean,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayer: (TextLayerItem) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onEditLayerText: (TextLayerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val boxH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        layers.forEach { layer ->
            val isSelected = interactive && layer.id == selectedLayerId
            TextLayerItemView(
                layer = layer,
                isSelected = isSelected,
                interactive = interactive,
                boxW = boxW,
                boxH = boxH,
                onTap = {
                    if (isSelected) {
                        onEditLayerText(layer)
                    } else {
                        onSelectLayer(layer.id)
                    }
                },
                onTransform = { dxNorm, dyNorm, zoom, rot ->
                    onUpdateLayer(
                        layer.copy(
                            normX = (layer.normX + dxNorm).coerceIn(0.05f, 0.95f),
                            normY = (layer.normY + dyNorm).coerceIn(0.05f, 0.95f),
                            scale = (layer.scale * zoom).coerceIn(0.4f, 3.5f),
                            rotationDegrees = (layer.rotationDegrees + rot) % 360f
                        )
                    )
                },
                onDelete = { onDeleteLayer(layer.id) }
            )
        }
    }
}

@Composable
private fun TextLayerItemView(
    layer: TextLayerItem,
    isSelected: Boolean,
    interactive: Boolean,
    boxW: Float,
    boxH: Float,
    onTap: () -> Unit,
    onTransform: (dxNorm: Float, dyNorm: Float, zoom: Float, rot: Float) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val resolvedFont = EditorFontResolver.resolveCompose(context, layer.fontSpec)
    val layerColor = EditorPalette.colorFromArgb(layer.colorArgb)

    val textColor = when (layer.bgStyle) {
        TextBackgroundStyle.FILLED -> if (layer.colorArgb == 0xFF000000L) Color.Black else Color.Black
        else -> layerColor
    }
    val bgColor = when (layer.bgStyle) {
        TextBackgroundStyle.FILLED -> if (layer.colorArgb == 0xFF000000L) Color.White else layerColor
        TextBackgroundStyle.SEMI -> Color.Black.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val textAlign = when (layer.align) {
        TextAlignOption.LEFT -> TextAlign.Start
        TextAlignOption.CENTER -> TextAlign.Center
        TextAlignOption.RIGHT -> TextAlign.End
    }

    val centerX = (layer.normX * boxW).roundToInt()
    val centerY = (layer.normY * boxH).roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(centerX, centerY) }
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height / 2f
                scaleX = layer.scale
                scaleY = layer.scale
                rotationZ = layer.rotationDegrees
            }
            .then(
                if (interactive) {
                    Modifier
                        .pointerInput(layer.id) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                onTransform(pan.x / boxW, pan.y / boxH, zoom, rotation)
                            }
                        }
                        .clickable { onTap() }
                } else Modifier
            )
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Color.White,
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            )
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = layer.text,
                color = textColor,
                fontSize = 22.sp,
                fontFamily = resolvedFont.fontFamily,
                fontWeight = resolvedFont.fontWeight,
                fontStyle = resolvedFont.fontStyle,
                textAlign = textAlign
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun TextControlsBar(
    selectedLayer: TextLayerItem?,
    onAddNewText: () -> Unit,
    onUpdateLayer: (TextLayerItem) -> Unit,
    onOpenDownloadFonts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    var showFontMenu by remember { mutableStateOf(false) }

    val activeFont = selectedLayer?.fontSpec ?: EditorFontSpec.defaultBasic
    val activeAlign = selectedLayer?.align ?: TextAlignOption.CENTER
    val activeBgStyle = selectedLayer?.bgStyle ?: TextBackgroundStyle.NONE

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Font button
            Box {
                Text(
                    text = EditorFontResolver.labelFor(activeFont, strings),
                    color = scheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                        .clickable { showFontMenu = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                EditorFontPickerMenu(
                    expanded = showFontMenu,
                    selectedFont = activeFont,
                    onSelectFont = { spec ->
                        selectedLayer?.let { onUpdateLayer(it.copy(fontSpec = spec)) }
                    },
                    onOpenDownloadFonts = onOpenDownloadFonts,
                    onDismiss = { showFontMenu = false }
                )
            }

            // Alignment button
            IconButton(
                onClick = {
                    val nextAlign = when (activeAlign) {
                        TextAlignOption.LEFT -> TextAlignOption.CENTER
                        TextAlignOption.CENTER -> TextAlignOption.RIGHT
                        TextAlignOption.RIGHT -> TextAlignOption.LEFT
                    }
                    selectedLayer?.let { onUpdateLayer(it.copy(align = nextAlign)) }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(scheme.card)
            ) {
                Icon(
                    imageVector = when (activeAlign) {
                        TextAlignOption.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        TextAlignOption.CENTER -> Icons.Default.FormatAlignCenter
                        TextAlignOption.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                    },
                    contentDescription = null,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Style [A] button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (activeBgStyle != TextBackgroundStyle.NONE) scheme.accent else scheme.card)
                    .clickable {
                        val nextStyle = when (activeBgStyle) {
                            TextBackgroundStyle.NONE -> TextBackgroundStyle.FILLED
                            TextBackgroundStyle.FILLED -> TextBackgroundStyle.SEMI
                            TextBackgroundStyle.SEMI -> TextBackgroundStyle.OUTLINE
                            TextBackgroundStyle.OUTLINE -> TextBackgroundStyle.NONE
                        }
                        selectedLayer?.let { onUpdateLayer(it.copy(bgStyle = nextStyle)) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = if (activeBgStyle != TextBackgroundStyle.NONE) scheme.onAccent else scheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Add text pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.accent)
                    .clickable { onAddNewText() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = null,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.addText,
                    color = scheme.onAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TextInputEditorDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    var text by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.card,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(strings.editText, color = scheme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(strings.textPlaceholder, color = scheme.textMuted) },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = scheme.inputBg,
                    unfocusedContainerColor = scheme.inputBg,
                    focusedBorderColor = scheme.accent,
                    unfocusedBorderColor = scheme.inputBorder,
                    focusedTextColor = scheme.textPrimary,
                    unfocusedTextColor = scheme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
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
