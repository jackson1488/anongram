package com.anongram.app.features.chats.mediaeditor.ui.paint

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.engine.PhotoFilterEngine
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.BrushStroke
import com.anongram.app.features.chats.mediaeditor.model.BrushType
import com.anongram.app.features.chats.mediaeditor.model.EditorPalette
import com.anongram.app.features.chats.mediaeditor.model.TextLayerItem
import com.anongram.app.features.chats.mediaeditor.ui.text.TextControlsBar
import com.anongram.app.features.chats.mediaeditor.ui.text.TextLayersCanvasOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class PaintModeTab {
    DRAWING,
    TEXT
}

@Composable
fun PaintOverlayPanel(
    previewBitmap: ImageBitmap?,
    initialStrokes: List<BrushStroke>,
    initialTextLayers: List<TextLayerItem>,
    initialTab: PaintModeTab = PaintModeTab.DRAWING,
    initialSelectedTextId: String? = null,
    onApply: (List<BrushStroke>, List<TextLayerItem>) -> Unit,
    onCancel: () -> Unit,
    onOpenDownloadFonts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedBrush by remember { mutableStateOf(BrushType.PEN) }
    var selectedColorArgb by remember { mutableLongStateOf(EditorPalette.quickColors[0]) }
    var strokeWidthNorm by remember { mutableFloatStateOf(0.022f) }
    var isAdjustingBrushSize by remember { mutableStateOf(false) }
    var showFullColorPicker by remember { mutableStateOf(false) }

    val strokes = remember(initialStrokes) {
        mutableStateListOf<BrushStroke>().apply { addAll(initialStrokes) }
    }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val textLayers = remember(initialTextLayers) {
        mutableStateListOf<TextLayerItem>().apply {
            addAll(initialTextLayers)
            if (initialTab == PaintModeTab.TEXT && isEmpty()) {
                add(TextLayerItem(text = ""))
            }
        }
    }
    var selectedTextId by remember(initialSelectedTextId) {
        mutableStateOf<String?>(initialSelectedTextId ?: textLayers.lastOrNull()?.id)
    }
    var isTypingText by remember(initialTab) {
        mutableStateOf(initialTab == PaintModeTab.TEXT)
    }

    // Dynamically composite previewBitmap + current strokes so BLUR brush in the same session blurs any drawn lines too!
    var blurredBitmap by remember(previewBitmap) { mutableStateOf<Bitmap?>(null) }
    val committedStrokesSnapshot = strokes.toList()
    LaunchedEffect(previewBitmap, committedStrokesSnapshot.size) {
        val img = previewBitmap ?: return@LaunchedEffect
        blurredBitmap = withContext(Dispatchers.Default) {
            runCatching {
                val base = img.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
                val w = base.width
                val h = base.height
                val minDim = minOf(w, h).toFloat().coerceAtLeast(1f)
                // Draw any non-blur strokes onto base before blurring so BLUR brush blurs them too
                if (committedStrokesSnapshot.isNotEmpty()) {
                    val strokeBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val sc = android.graphics.Canvas(strokeBmp)
                    for (st in committedStrokesSnapshot) {
                        if (st.points.isEmpty() || st.type == BrushType.BLUR) continue
                        val spx = (st.strokeWidthNorm * minDim).coerceAtLeast(3f)
                        val p = buildNativePath(st, w.toFloat(), h.toFloat())
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = if (st.type == BrushType.MARKER) Paint.Cap.SQUARE else Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = if (st.type == BrushType.MARKER) spx * 1.5f else spx
                            if (st.type == BrushType.ERASER) {
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                            } else {
                                color = st.colorArgb.toInt()
                                if (st.type == BrushType.MARKER) alpha = 135
                            }
                        }
                        sc.drawPath(p, paint)
                    }
                    android.graphics.Canvas(base).drawBitmap(strokeBmp, 0f, 0f, null)
                }
                val px = IntArray(w * h)
                base.getPixels(px, 0, w, 0, 0, w, h)
                val pass1 = PhotoFilterEngine.fastBoxBlur(px, w, h, radius = 22)
                val pass2 = PhotoFilterEngine.fastBoxBlur(pass1, w, h, radius = 22)
                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                out.setPixels(pass2, 0, w, 0, 0, w, h)
                out
            }.getOrNull()
        }
    }

    // Smoothly shrink photo into top area when bottom controls or full color picker sheet is open
    val animatedTopPad by animateDpAsState(
        targetValue = 50.dp,
        animationSpec = tween(220),
        label = "paintTopPad"
    )
    val animatedBottomPad by animateDpAsState(
        targetValue = if (showFullColorPicker) 370.dp else 118.dp,
        animationSpec = tween(240),
        label = "paintBottomPad"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Photo + Strokes + Text Canvas (smoothly shrinks into top area above editing controls / color sheet)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = animatedTopPad, bottom = animatedBottomPad, start = 8.dp, end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val photoAspect = remember(previewBitmap) {
                val bmp = previewBitmap
                if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                    (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.2f, 5f)
                } else {
                    1f
                }
            }

            // Strictly bound drawing, blur, eraser, and text to the exact photo aspect rectangle
            Box(
                modifier = Modifier
                    .aspectRatio(photoAspect, matchHeightConstraintsFirst = photoAspect < 1f)
                    .clip(RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val currentStrokePreview = if (activePoints.isNotEmpty()) {
                    BrushStroke(
                        id = "active",
                        type = selectedBrush,
                        colorArgb = selectedColorArgb,
                        strokeWidthNorm = strokeWidthNorm,
                        points = activePoints
                    )
                } else null

                val allStrokesToRender = remember(committedStrokesSnapshot, currentStrokePreview) {
                    if (currentStrokePreview != null) committedStrokesSnapshot + currentStrokePreview else committedStrokesSnapshot
                }

                StrokesPreviewCanvas(
                    strokes = allStrokesToRender,
                    blurredBitmap = blurredBitmap,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (activeTab == PaintModeTab.DRAWING) {
                                Modifier.pointerInput(selectedBrush, selectedColorArgb, strokeWidthNorm) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            activePoints = listOf(
                                                Offset(
                                                    (offset.x / w).coerceIn(0f, 1f),
                                                    (offset.y / h).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            val pt = Offset(
                                                (change.position.x / w).coerceIn(0f, 1f),
                                                (change.position.y / h).coerceIn(0f, 1f)
                                            )
                                            activePoints = activePoints + pt
                                        },
                                        onDragEnd = {
                                            if (activePoints.isNotEmpty()) {
                                                strokes.add(
                                                    BrushStroke(
                                                        id = UUID.randomUUID().toString(),
                                                        type = selectedBrush,
                                                        colorArgb = selectedColorArgb,
                                                        strokeWidthNorm = strokeWidthNorm,
                                                        points = activePoints
                                                    )
                                                )
                                                activePoints = emptyList()
                                            }
                                        },
                                        onDragCancel = { activePoints = emptyList() }
                                    )
                                }
                            } else Modifier
                        )
                )

                // Interactive Text Layers
                TextLayersCanvasOverlay(
                    layers = textLayers,
                    selectedLayerId = selectedTextId,
                    interactive = activeTab == PaintModeTab.TEXT,
                    isTypingText = isTypingText,
                    onTypingTextChange = { isTypingText = it },
                    onSelectLayer = { id ->
                        selectedTextId = id
                        if (id != null && activeTab != PaintModeTab.TEXT) {
                            activeTab = PaintModeTab.TEXT
                        }
                    },
                    onUpdateLayer = { updated ->
                        val idx = textLayers.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) textLayers[idx] = updated
                    },
                    onDeleteLayer = { id ->
                        textLayers.removeAll { it.id == id }
                        if (selectedTextId == id) selectedTextId = textLayers.lastOrNull()?.id
                    },
                    onEditLayerText = { layer ->
                        selectedTextId = layer.id
                        activeTab = PaintModeTab.TEXT
                        isTypingText = true
                    }
                )
            }

            // Center brush diameter preview circle while dragging the left vertical wedge slider
            if (isAdjustingBrushSize) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minDim = minOf(size.width, size.height)
                    val radiusPx = (strokeWidthNorm * minDim * 0.5f).coerceAtLeast(4f)
                    drawCircle(
                        color = Color.White,
                        radius = radiusPx,
                        center = Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                }
            }
        }

        // 2. Top Bar: Undo (↩) & Clear All (or in Text typing mode: Clear & Done to finish typing and enter move/scale mode!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (activeTab == PaintModeTab.DRAWING && strokes.isNotEmpty()) {
                        strokes.removeAt(strokes.lastIndex)
                    } else if (activeTab == PaintModeTab.TEXT && textLayers.isNotEmpty()) {
                        textLayers.removeAt(textLayers.lastIndex)
                        selectedTextId = textLayers.lastOrNull()?.id
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = strings.undo,
                    tint = Color.White
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.clearAll,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            if (activeTab == PaintModeTab.DRAWING) {
                                strokes.clear()
                            } else {
                                textLayers.clear()
                                val fresh = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                                textLayers.add(fresh)
                                selectedTextId = fresh.id
                                isTypingText = true
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )

                // When typing text with keyboard open, top-right "Готово" finishes typing so you can move/scale the text!
                if (activeTab == PaintModeTab.TEXT && isTypingText) {
                    Text(
                        text = strings.confirm,
                        color = scheme.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                isTypingText = false
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Vertical Tapered Wedge Slider on the Left Edge
        if (activeTab == PaintModeTab.DRAWING && !showFullColorPicker) {
            VerticalTaperedBrushSlider(
                strokeWidthNorm = strokeWidthNorm,
                onStrokeWidthChange = { strokeWidthNorm = it },
                onDraggingChange = { isAdjustingBrushSize = it },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .width(28.dp)
                    .height(210.dp)
            )
        }

        // 4. Bottom Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.96f))
                    )
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 8.dp, bottom = 6.dp)
        ) {
            if (activeTab == PaintModeTab.DRAWING) {
                BrushControlsBar(
                    selectedBrush = selectedBrush,
                    onSelectBrush = { selectedBrush = it },
                    selectedColorArgb = selectedColorArgb,
                    onSelectColor = { selectedColorArgb = it },
                    onOpenFullColorPicker = { showFullColorPicker = true }
                )
            } else {
                val selectedLayer = textLayers.firstOrNull { it.id == selectedTextId }
                TextControlsBar(
                    selectedLayer = selectedLayer,
                    selectedColorArgb = selectedLayer?.colorArgb ?: selectedColorArgb,
                    onSelectColor = { argb ->
                        selectedColorArgb = argb
                        selectedLayer?.let { l ->
                            val idx = textLayers.indexOfFirst { it.id == l.id }
                            if (idx >= 0) textLayers[idx] = l.copy(colorArgb = argb)
                        }
                    },
                    onAddNewText = {
                        val added = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                        textLayers.add(added)
                        selectedTextId = added.id
                        isTypingText = true
                        keyboardController?.show()
                    },
                    onUpdateLayer = { updated ->
                        val idx = textLayers.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) textLayers[idx] = updated
                    },
                    onOpenDownloadFonts = onOpenDownloadFonts,
                    onOpenFullColorPicker = { showFullColorPicker = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row: ✕ | РИСУНОК   ТЕКСТ | ✓
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        tint = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        PaintModeTab.DRAWING to strings.tabDrawing,
                        PaintModeTab.TEXT to strings.tabText
                    ).forEach { (tab, title) ->
                        val selected = activeTab == tab
                        Text(
                            text = title,
                            color = if (selected) Color.White else scheme.textMuted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeTab = tab
                                    if (tab == PaintModeTab.DRAWING) {
                                        isTypingText = false
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    } else {
                                        if (textLayers.isEmpty()) {
                                            val added = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                                            textLayers.add(added)
                                            selectedTextId = added.id
                                        }
                                        isTypingText = true
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isTypingText = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        val cleanTexts = textLayers.filter { it.text.isNotBlank() }
                        onApply(strokes.toList(), cleanTexts)
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = strings.done,
                        tint = Color.White
                    )
                }
            }
        }

        // 5. Full 3-Tab Color Picker Bottom Sheet (ПАЛИТРА | СПЕКТР | ШКАЛА)
        if (showFullColorPicker) {
            val activeLayer = textLayers.firstOrNull { it.id == selectedTextId }
            val currentColor = if (activeTab == PaintModeTab.TEXT && activeLayer != null) {
                activeLayer.colorArgb
            } else selectedColorArgb

            FullColorPickerBottomSheet(
                initialArgb = currentColor,
                onConfirm = { newArgb ->
                    selectedColorArgb = newArgb
                    if (activeTab == PaintModeTab.TEXT && activeLayer != null) {
                        val idx = textLayers.indexOfFirst { it.id == activeLayer.id }
                        if (idx >= 0) textLayers[idx] = activeLayer.copy(colorArgb = newArgb)
                    }
                    showFullColorPicker = false
                },
                onDismiss = { showFullColorPicker = false }
            )
        }
    }
}

@Composable
private fun VerticalTaperedBrushSlider(
    strokeWidthNorm: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val minNorm = 0.005f
    val maxNorm = 0.075f
    val frac = ((strokeWidthNorm - minNorm) / (maxNorm - minNorm)).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onDraggingChange(true)
                        val f = 1f - (offset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        onStrokeWidthChange(minNorm + f * (maxNorm - minNorm))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val f = 1f - (change.position.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        onStrokeWidthChange(minNorm + f * (maxNorm - minNorm))
                    },
                    onDragEnd = { onDraggingChange(false) },
                    onDragCancel = { onDraggingChange(false) }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val topR = 9.5.dp.toPx()
        val botR = 2.2.dp.toPx()
        val topY = topR
        val botY = h - botR

        val wedgePath = android.graphics.Path().apply {
            moveTo(cx - topR, topY)
            quadTo(cx, topY - topR, cx + topR, topY)
            lineTo(cx + botR, botY)
            quadTo(cx, botY + botR, cx - botR, botY)
            close()
        }

        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xAA48484A.toInt()
                style = Paint.Style.FILL
            }
            canvas.nativeCanvas.drawPath(wedgePath, paint)
        }

        val thumbY = botY - frac * (botY - topY)
        drawCircle(
            color = Color.White,
            radius = 7.5.dp.toPx(),
            center = Offset(cx, thumbY)
        )
    }
}

@Composable
fun StrokesPreviewCanvas(
    strokes: List<BrushStroke>,
    blurredBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h).coerceAtLeast(1f)

        val blurredShader = blurredBitmap?.let { bmp ->
            val bw = bmp.width.toFloat().coerceAtLeast(1f)
            val bh = bmp.height.toFloat().coerceAtLeast(1f)
            val scale = minOf(w / bw, h / bh)
            val dx = (w - bw * scale) * 0.5f
            val dy = (h - bh * scale) * 0.5f
            val shaderMatrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            }
            BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(shaderMatrix)
            }
        }

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val saveCount = native.saveLayer(0f, 0f, w, h, null)

            for (stroke in strokes) {
                if (stroke.points.isEmpty()) continue
                val strokePx = (stroke.strokeWidthNorm * minDim).coerceAtLeast(3f)
                val path = buildNativePath(stroke, w, h)

                when (stroke.type) {
                    BrushType.BLUR -> {
                        val softBlurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 2.2f
                            if (blurredShader != null) {
                                shader = blurredShader
                            } else {
                                color = 0x9994A3B8.toInt()
                            }
                            maskFilter = BlurMaskFilter((strokePx * 0.45f).coerceAtLeast(4f), BlurMaskFilter.Blur.NORMAL)
                        }
                        native.drawPath(path, softBlurPaint)
                    }
                    BrushType.ERASER -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 1.8f
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        }
                        native.drawPath(path, paint)
                    }
                    BrushType.MARKER -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.SQUARE
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 1.5f
                            color = stroke.colorArgb.toInt()
                            alpha = 135
                        }
                        native.drawPath(path, paint)
                    }
                    BrushType.NEON -> {
                        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 1.9f
                            color = stroke.colorArgb.toInt()
                            maskFilter = BlurMaskFilter(strokePx * 0.8f, BlurMaskFilter.Blur.NORMAL)
                        }
                        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 0.6f
                            color = android.graphics.Color.WHITE
                        }
                        native.drawPath(path, glow)
                        native.drawPath(path, core)
                    }
                    BrushType.PEN -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx
                            color = stroke.colorArgb.toInt()
                        }
                        native.drawPath(path, paint)
                    }
                    BrushType.ARROW -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx
                            color = stroke.colorArgb.toInt()
                        }
                        native.drawPath(path, paint)
                        if (stroke.points.size >= 2) {
                            val end = stroke.points.last()
                            val prev = stroke.points[(stroke.points.size - 5).coerceAtLeast(0)]
                            val ex = end.x * w
                            val ey = end.y * h
                            val angle = atan2((ey - prev.y * h).toDouble(), (ex - prev.x * w).toDouble())
                            val headLen = strokePx * 3.2f
                            val wing = Math.toRadians(28.0)
                            val headPath = Path().apply {
                                moveTo((ex - headLen * cos(angle - wing)).toFloat(), (ey - headLen * sin(angle - wing)).toFloat())
                                lineTo(ex, ey)
                                lineTo((ex - headLen * cos(angle + wing)).toFloat(), (ey - headLen * sin(angle + wing)).toFloat())
                            }
                            native.drawPath(headPath, paint)
                        }
                    }
                }
            }
            native.restoreToCount(saveCount)
        }
    }
}

private fun buildNativePath(stroke: BrushStroke, w: Float, h: Float): Path {
    val path = Path()
    val first = stroke.points.first()
    path.moveTo(first.x * w, first.y * h)
    if (stroke.points.size == 1) {
        path.lineTo(first.x * w + 0.5f, first.y * h + 0.5f)
        return path
    }
    for (i in 1 until stroke.points.size) {
        val prev = stroke.points[i - 1]
        val curr = stroke.points[i]
        path.quadTo(prev.x * w, prev.y * h, (prev.x + curr.x) * 0.5f * w, (prev.y + curr.y) * 0.5f * h)
    }
    val last = stroke.points.last()
    path.lineTo(last.x * w, last.y * h)
    return path
}
