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
import androidx.compose.foundation.layout.imePadding
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
    var strokeWidthNorm by remember { mutableFloatStateOf(0.018f) }

    val strokes = remember(initialStrokes) {
        mutableStateListOf<BrushStroke>().apply { addAll(initialStrokes) }
    }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val textLayers = remember(initialTextLayers) {
        mutableStateListOf<TextLayerItem>().apply { addAll(initialTextLayers) }
    }
    var selectedTextId by remember { mutableStateOf<String?>(textLayers.lastOrNull()?.id) }

    // Precompute strongly blurred bitmap (2-pass radius 20) for real live Blur Brush preview
    val blurredBitmap = remember(previewBitmap) {
        previewBitmap?.let { img ->
            runCatching {
                val bmp = img.asAndroidBitmap()
                val w = bmp.width
                val h = bmp.height
                val px = IntArray(w * h)
                bmp.getPixels(px, 0, w, 0, 0, w, h)
                val pass1 = PhotoFilterEngine.fastBoxBlur(px, w, h, radius = 20)
                val pass2 = PhotoFilterEngine.fastBoxBlur(pass1, w, h, radius = 20)
                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                out.setPixels(pass2, 0, w, 0, 0, w, h)
                out
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top bar: Undo & Clear all (or Clear & Done in Text tab as in Screenshot 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeTab == PaintModeTab.TEXT) {
                Text(
                    text = strings.clearAll,
                    color = scheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            textLayers.clear()
                            val fresh = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                            textLayers.add(fresh)
                            selectedTextId = fresh.id
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )

                Text(
                    text = strings.confirm,
                    color = scheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            val cleanTexts = textLayers.filter { it.text.isNotBlank() }
                            onApply(strokes.toList(), cleanTexts)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            } else {
                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes.removeAt(strokes.lastIndex)
                        }
                    },
                    modifier = Modifier.size(38.dp)
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
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { strokes.clear() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Media + Drawing + Text Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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

                val allStrokesToRender = remember(strokes.toList(), currentStrokePreview) {
                    if (currentStrokePreview != null) strokes + currentStrokePreview else strokes.toList()
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

                // Inline Text Layers Overlay (no dialog window!)
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
                        selectedTextId = layer.id
                    }
                )
            }
        }

        // Bottom Controls Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(top = 6.dp, bottom = 6.dp)
        ) {
            if (activeTab == PaintModeTab.DRAWING) {
                BrushControlsBar(
                    selectedBrush = selectedBrush,
                    onSelectBrush = { selectedBrush = it },
                    selectedColorArgb = selectedColorArgb,
                    onSelectColor = { selectedColorArgb = it },
                    strokeWidthNorm = strokeWidthNorm,
                    onStrokeWidthChange = { strokeWidthNorm = it }
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
                    },
                    onUpdateLayer = { updated ->
                        val idx = textLayers.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) textLayers[idx] = updated
                    },
                    onOpenDownloadFonts = onOpenDownloadFonts
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row: ✕ | РИСУНОК | ТЕКСТ | ✓
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
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
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
                                    if (tab == PaintModeTab.TEXT && textLayers.isEmpty()) {
                                        val added = TextLayerItem(text = "", colorArgb = selectedColorArgb)
                                        textLayers.add(added)
                                        selectedTextId = added.id
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
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

        // Prepare BitmapShader mapped to ContentScale.Fit bounds of the photo on screen
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
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = strokePx * 2.2f
                            if (blurredShader != null) {
                                shader = blurredShader
                            } else {
                                color = 0x9994A3B8.toInt()
                            }
                        }
                        native.drawPath(path, paint)
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
