package com.anongram.app.features.chats.mediaeditor.ui.text

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
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
import com.anongram.app.features.chats.mediaeditor.ui.paint.ColorPaletteBar
import kotlin.math.hypot
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLayers by rememberUpdatedState(layers)
    val currentSelectedId by rememberUpdatedState(selectedLayerId)
    val currentOnUpdate by rememberUpdatedState(onUpdateLayer)
    val currentOnSelect by rememberUpdatedState(onSelectLayer)

    // requestFocusVersion increments whenever user taps on a text layer to edit its text again
    var focusTrigger by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (interactive) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val hit = currentLayers.lastOrNull { l ->
                                    hypot(tapOffset.x - l.normX * w, tapOffset.y - l.normY * h) < 115.dp.toPx()
                                }
                                if (hit != null) {
                                    currentOnSelect(hit.id)
                                    focusTrigger += 1
                                    keyboardController?.show()
                                } else {
                                    // Tap outside unfocuses keyboard so user can freely pinch/scale/drag
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val active = currentLayers.firstOrNull { it.id == currentSelectedId }
                                    ?: currentLayers.lastOrNull()
                                    ?: return@detectTransformGestures
                                currentOnUpdate(
                                    active.copy(
                                        normX = (active.normX + pan.x / w).coerceIn(0.08f, 0.92f),
                                        normY = (active.normY + pan.y / h).coerceIn(0.08f, 0.92f),
                                        scale = (active.scale * zoom).coerceIn(0.45f, 4.0f),
                                        rotationDegrees = (active.rotationDegrees + rotation) % 360f
                                    )
                                )
                            }
                        }
                } else Modifier
            )
    ) {
        val boxW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val boxH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        layers.forEach { layer ->
            val isSelected = interactive && layer.id == selectedLayerId
            TextLayerItemView(
                layer = layer,
                isSelected = isSelected,
                interactive = interactive,
                focusTrigger = focusTrigger,
                boxW = boxW,
                boxH = boxH,
                onSelectAndFocus = {
                    onSelectLayer(layer.id)
                    focusTrigger += 1
                    keyboardController?.show()
                },
                onScaleDelta = { deltaScale ->
                    onUpdateLayer(layer.copy(scale = (layer.scale + deltaScale).coerceIn(0.45f, 4.0f)))
                },
                onTextChange = { newText -> onUpdateLayer(layer.copy(text = newText)) }
            )
        }
    }
}

@Composable
private fun TextLayerItemView(
    layer: TextLayerItem,
    isSelected: Boolean,
    interactive: Boolean,
    focusTrigger: Int,
    boxW: Float,
    boxH: Float,
    onSelectAndFocus: () -> Unit,
    onScaleDelta: (Float) -> Unit,
    onTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = MediaEditorI18n.current
    val resolvedFont = EditorFontResolver.resolveCompose(context, layer.fontSpec)
    val layerColor = EditorPalette.colorFromArgb(layer.colorArgb)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSelected, focusTrigger) {
        if (isSelected && interactive) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val textColor = when (layer.bgStyle) {
        TextBackgroundStyle.FILLED -> if (layer.colorArgb == 0xFF000000L) Color.White else Color.Black
        else -> layerColor
    }
    val bgColor = when (layer.bgStyle) {
        TextBackgroundStyle.FILLED -> if (layer.colorArgb == 0xFF000000L) Color.Black else layerColor
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
                if (isSelected) {
                    Modifier.drawBehind {
                        val cr = 12.dp.toPx()
                        drawRoundRect(
                            color = Color.White,
                            cornerRadius = CornerRadius(cr, cr),
                            style = Stroke(
                                width = 1.8.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                        val handleR = 5.5.dp.toPx()
                        val midY = size.height / 2f
                        drawCircle(Color.White, radius = handleR + 1.8.dp.toPx(), center = Offset(0f, midY))
                        drawCircle(Color(0xFF0A84FF), radius = handleR, center = Offset(0f, midY))
                        drawCircle(Color.White, radius = handleR + 1.8.dp.toPx(), center = Offset(size.width, midY))
                        drawCircle(Color(0xFF0A84FF), radius = handleR, center = Offset(size.width, midY))
                    }
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 280.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .clickable(enabled = interactive) { onSelectAndFocus() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val style = TextStyle(
                color = textColor,
                fontSize = 24.sp,
                fontFamily = resolvedFont.fontFamily,
                fontWeight = resolvedFont.fontWeight,
                fontStyle = resolvedFont.fontStyle,
                textAlign = textAlign
            )

            if (interactive && isSelected) {
                BasicTextField(
                    value = layer.text,
                    onValueChange = onTextChange,
                    textStyle = style,
                    cursorBrush = SolidColor(Color(0xFFFF3B30)),
                    modifier = Modifier.focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (layer.text.isEmpty()) {
                                Text(
                                    text = strings.textPlaceholder,
                                    style = style.copy(color = Color.White.copy(alpha = 0.55f))
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else if (layer.text.isNotBlank()) {
                Text(
                    text = layer.text,
                    style = style
                )
            }
        }
    }
}

@Composable
fun TextControlsBar(
    selectedLayer: TextLayerItem?,
    selectedColorArgb: Long,
    onSelectColor: (Long) -> Unit,
    onAddNewText: () -> Unit,
    onUpdateLayer: (TextLayerItem) -> Unit,
    onOpenDownloadFonts: () -> Unit,
    onOpenFullColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    var showFontMenu by remember { mutableStateOf(false) }
    var showColorGrid by remember { mutableStateOf(false) }

    val activeFont = selectedLayer?.fontSpec ?: EditorFontSpec.defaultBasic
    val activeAlign = selectedLayer?.align ?: TextAlignOption.CENTER
    val activeBgStyle = selectedLayer?.bgStyle ?: TextBackgroundStyle.NONE
    val activeColor = selectedLayer?.colorArgb ?: selectedColorArgb

    AnimatedContent(
        targetState = showColorGrid,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "textOrPaletteCapsule",
        modifier = modifier.fillMaxWidth()
    ) { isGrid ->
        if (isGrid) {
            ColorPaletteBar(
                selectedColorArgb = activeColor,
                onSelectColor = { onSelectColor(it) },
                onBackToBrushes = { showColorGrid = false },
                onOpenFullPicker = onOpenFullColorPicker
            )
        } else {
            // Single compact capsule above keyboard (matching Telegram screenshot 3)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color circle -> animates into 2x7 palette grid
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EditorPalette.colorFromArgb(activeColor))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showColorGrid = true }
                    )

                    // Alignment icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                val nextAlign = when (activeAlign) {
                                    TextAlignOption.LEFT -> TextAlignOption.CENTER
                                    TextAlignOption.CENTER -> TextAlignOption.RIGHT
                                    TextAlignOption.RIGHT -> TextAlignOption.LEFT
                                }
                                selectedLayer?.let { onUpdateLayer(it.copy(align = nextAlign)) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (activeAlign) {
                                TextAlignOption.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                TextAlignOption.CENTER -> Icons.Default.FormatAlignCenter
                                TextAlignOption.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                            },
                            contentDescription = null,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Style [A]
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (activeBgStyle != TextBackgroundStyle.NONE) scheme.accent else Color.Transparent)
                            .border(1.5.dp, Color.White, RoundedCornerShape(7.dp))
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
                            color = if (activeBgStyle != TextBackgroundStyle.NONE) scheme.onAccent else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // (+) button to add another text layer on the same photo
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(scheme.card)
                            .clickable { onAddNewText() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.addText,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Font selector pill on the right: [Roboto ⌄]
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                            .clickable { showFontMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = EditorFontResolver.labelFor(activeFont, strings),
                            color = scheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Rounded.UnfoldMore,
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

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
            }
        }
    }
}
