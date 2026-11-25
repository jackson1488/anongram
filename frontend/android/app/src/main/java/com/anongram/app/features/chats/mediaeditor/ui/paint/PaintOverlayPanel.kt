package com.anongram.app.features.chats.mediaeditor.ui.paint

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.anongram.app.features.chats.mediaeditor.ui.text.TextInputEditorDialog
import com.anongram.app.features.chats.mediaeditor.ui.text.TextLayersCanvasOverlay
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
    onApply: (List<BrushStroke>, List<TextLayerItem>) -> Unit,
    onCancel: () -> Unit,
    onOpenDownloadFonts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current

    var activeTab by remember { mutableStateOf(PaintModeTab.DRAWING) }
    var selectedBrush by remember { mutableStateOf(BrushType.PEN) }
    var selectedColorArgb by remember { mutableLongStateOf(EditorPalette.quickColors[2]) } // Red
    var strokeWidthNorm by remember { mutableFloatStateOf(0.015f) }

    val strokes = remember(initialStrokes) {
        mutableStateListOf<BrushStroke>().apply { addAll(initialStrokes) }
    }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val textLayers = remember(initialTextLayers) {
        mutableStateListOf<TextLayerItem>().apply { addAll(initialTextLayers) }
    }
    var selectedTextId by remember { mutableStateOf<String?>(textLayers.lastOrNull()?.id) }
    var editingTextLayer by remember { mutableStateOf<TextLayerItem?>(null) }
    var isAddingNewText by remember { mutableStateOf(false) }

    // Precompute blurred bitmap for live Blur Brush preview
    val blurredShader = remember(previewBitmap) {
        previewBitmap?.let { img ->
            runCatching {
                val bmp = img.asAndroidBitmap()
                val w = bmp.width
                val h = bmp.height
                val px = IntArray(w * h)
                bmp.getPixels(px, 0, w, 0, 0, w, h)
                val blurredPx = PhotoFilterEngine.fastBoxBlur(px, w, h, radius = 14)
                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                out.setPixels(blurredPx, 0, w, 0, 0, w, h)
                BitmapShader(out, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar: Undo & Clear all
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.islandBg.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = strings.undo,
                    tint = scheme.textPrimary
                )
            }

            Text(
                text = strings.clearAll,
                color = scheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.islandBg.copy(alpha = 0.85f))
                    .clickable {
                        if (activeTab == PaintModeTab.DRAWING) {
                            strokes.clear()
                        } else {
                            textLayers.clear()
                            selectedTextId = null
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Media + Drawing + Text Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val aspect = if (previewBitmap != null && previewBitmap.height > 0) {
                previewBitmap.width.toFloat() / previewBitmap.height.toFloat()
            } else 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Strokes Canvas
                val currentStrokePreview = if (activePoints.isNotEmpty()) {
                    BrushStroke(
                        id = "active",
                        type = selectedBrush,
                        colorArgb = selectedColorArgb,
                        strokeWidthNorm = strokeWidthNorm,
                        points = activePoints
                    )
                } else null

                val allStrokesToRender = remember(strokes.toList(), currentStrokePreview) {
                    if (currentStrokePreview != null) strokes + currentStrokePreview else strokes.toList()
                }

                StrokesPreviewCanvas(
                    strokes = allStrokesToRender,
                    blurredShader = blurredShader,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (activeTab == PaintModeTab.DRAWING) {
                                Modifier.pointerInput(selectedBrush, selectedColorArgb, strokeWidthNorm) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            activePoints = listOf(Offset(offset.x / w, offset.y / h))
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

                // Text Layers Overlay
                TextLayersCanvasOverlay(
                    layers = textLayers,
                    selectedLayerId = selectedTextId,
                    interactive = activeTab == PaintModeTab.TEXT,
                    onSelectLayer = { selectedTextId = it },
                    onUpdateLayer = { updated ->
                        val idx = textLayers.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) textLayers[idx] = updated
                    },
                    onDeleteLayer = { id ->
                        textLayers.removeAll { it.id == id }
                        if (selectedTextId == id) selectedTextId = textLayers.lastOrNull()?.id
                    },
                    onEditLayerText = { layer ->
                        editingTextLayer = layer
                        isAddingNewText = false
                    }
                )
            }
        }

        // Bottom Controls Panel (NO Stickers — only РИСУНОК | ТЕКСТ)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface.copy(alpha = 0.96f))
                .padding(top = 6.dp, bottom = 6.dp)
        ) {
            if (activeTab == PaintModeTab.DRAWING) {
                ColorPaletteBar(
                    selectedColorArgb = selectedColorArgb,
                    onSelectColor = { selectedColorArgb = it }
                )
                BrushControlsBar(
                    selectedBrush = selectedBrush,
                    onSelectBrush = { selectedBrush = it },
                    strokeWidthNorm = strokeWidthNorm,
                    onStrokeWidthChange = { strokeWidthNorm = it }
                )
            } else {
                val selectedLayer = textLayers.firstOrNull { it.id == selectedTextId }
                ColorPaletteBar(
                    selectedColorArgb = selectedLayer?.colorArgb ?: selectedColorArgb,
                    onSelectColor = { argb ->
                        selectedColorArgb = argb
                        selectedLayer?.let { l ->
                            val idx = textLayers.indexOfFirst { it.id == l.id }
                            if (idx >= 0) textLayers[idx] = l.copy(colorArgb = argb)
                        }
                    }
                )
                TextControlsBar(
                    selectedLayer = selectedLayer,
                    onAddNewText = {
                        isAddingNewText = true
                        editingTextLayer = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                    },
                    onUpdateLayer = { updated ->
                        val idx = textLayers.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) textLayers[idx] = updated
                    },
                    onOpenDownloadFonts = onOpenDownloadFonts
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom row: ✕ | [РИСУНОК | ТЕКСТ] | ✓
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        tint = scheme.textPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.card)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        PaintModeTab.DRAWING to strings.tabDrawing,
                        PaintModeTab.TEXT to strings.tabText
                    ).forEach { (tab, title) ->
                        val selected = activeTab == tab
                        Text(
                            text = title,
                            color = if (selected) scheme.onAccent else scheme.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) scheme.accent else Color.Transparent)
                                .clickable {
                                    activeTab = tab
                                    if (tab == PaintModeTab.TEXT && textLayers.isEmpty()) {
                                        isAddingNewText = true
                                        editingTextLayer = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onApply(strokes.toList(), textLayers.toList()) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = strings.done,
                        tint = scheme.accent
                    )
                }
            }
        }
    }

    editingTextLayer?.let { target ->
        TextInputEditorDialog(
            initialText = target.text,
            onConfirm = { newText ->
                if (newText.isNotBlank()) {
                    if (isAddingNewText) {
                        val added = target.copy(text = newText)
                        textLayers.add(added)
                        selectedTextId = added.id
                    } else {
                        val idx = textLayers.indexOfFirst { it.id == target.id }
                        if (idx >= 0) textLayers[idx] = target.copy(text = newText)
                    }
                }
                editingTextLayer = null
            },
            onDismiss = { editingTextLayer = null }
        )
    }
}

@Composable
fun StrokesPreviewCanvas(
    strokes: List<BrushStroke>,
    blurredShader: BitmapShader? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h).coerceAtLeast(1f)

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val saveCount = native.saveLayer(0f, 0f, w, h, null)

            for (stroke in strokes) {
                if (stroke.points.isEmpty()) continue
                val strokePx = (stroke.strokeWidthNorm * minDim).coerceAtLeast(3f)
                val path = buildNativePath(stroke, w, h)

                when (stroke.type) {
                    BrushType.BLUR -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 1.6f
                            if (blurredShader != null) {
                                shader = blurredShader
                            } else {
                                color = 0x8894A3B8.toInt()
                            }
                        }
                        native.drawPath(path, paint)
                    }
                    BrushType.ERASER -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 1.3f
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
                            alpha = 128
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
