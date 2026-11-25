package com.anongram.app.features.chats.mediaeditor.ui.crop

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
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.CropRect
import com.anongram.app.features.chats.mediaeditor.model.CropTransform
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DragTarget {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE
}

@Composable
fun CropRotatePanel(
    bitmap: ImageBitmap?,
    initialTransform: CropTransform,
    onApply: (CropTransform) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current

    var draft by remember(initialTransform) { mutableStateOf(initialTransform) }
    var isDraggingCrop by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Preview & Interactive Crop Frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .onSizeChanged { boxSize = it },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                val swapped = (draft.baseRotation90 / 90) % 2 != 0
                val displayAspect = if (swapped) 1f / aspect else aspect

                // Compute fitted media rect inside boxSize
                val fittedRect = remember(boxSize, displayAspect) {
                    computeFittedRect(boxSize, displayAspect)
                }

                val rad = Math.toRadians(abs(draft.straightenDegrees).toDouble())
                val autoZoom = (cos(rad) + sin(rad) * maxOf(displayAspect, 1f / displayAspect)).toFloat()
                    .coerceIn(1f, 1.8f)

                Box(
                    modifier = Modifier
                        .size(
                            width = (fittedRect.width / androidx.compose.ui.platform.LocalDensity.current.density).dp,
                            height = (fittedRect.height / androidx.compose.ui.platform.LocalDensity.current.density).dp
                        )
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = draft.totalRotationDegrees
                                scaleX = (if (draft.mirrorHorizontal) -1f else 1f) * autoZoom
                                scaleY = autoZoom
                            }
                    )

                    // Crop overlay canvas
                    var activeHandle by remember { mutableStateOf(DragTarget.NONE) }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val r = draft.cropRect
                                        val l = r.left * w
                                        val t = r.top * h
                                        val ri = r.right * w
                                        val b = r.bottom * h
                                        val hitR = 36.dp.toPx()

                                        activeHandle = when {
                                            (offset - Offset(l, t)).getDistance() <= hitR -> DragTarget.TOP_LEFT
                                            (offset - Offset(ri, t)).getDistance() <= hitR -> DragTarget.TOP_RIGHT
                                            (offset - Offset(l, b)).getDistance() <= hitR -> DragTarget.BOTTOM_LEFT
                                            (offset - Offset(ri, b)).getDistance() <= hitR -> DragTarget.BOTTOM_RIGHT
                                            offset.x in l..ri && offset.y in t..b -> DragTarget.MOVE
                                            else -> DragTarget.NONE
                                        }
                                        isDraggingCrop = activeHandle != DragTarget.NONE
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (activeHandle == DragTarget.NONE) return@detectDragGestures
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val dx = dragAmount.x / w
                                        val dy = dragAmount.y / h
                                        val cur = draft.cropRect
                                        val minSize = 0.15f

                                        val next = when (activeHandle) {
                                            DragTarget.TOP_LEFT -> cur.copy(
                                                left = (cur.left + dx).coerceIn(0f, cur.right - minSize),
                                                top = (cur.top + dy).coerceIn(0f, cur.bottom - minSize)
                                            )
                                            DragTarget.TOP_RIGHT -> cur.copy(
                                                right = (cur.right + dx).coerceIn(cur.left + minSize, 1f),
                                                top = (cur.top + dy).coerceIn(0f, cur.bottom - minSize)
                                            )
                                            DragTarget.BOTTOM_LEFT -> cur.copy(
                                                left = (cur.left + dx).coerceIn(0f, cur.right - minSize),
                                                bottom = (cur.bottom + dy).coerceIn(cur.top + minSize, 1f)
                                            )
                                            DragTarget.BOTTOM_RIGHT -> cur.copy(
                                                right = (cur.right + dx).coerceIn(cur.left + minSize, 1f),
                                                bottom = (cur.bottom + dy).coerceIn(cur.top + minSize, 1f)
                                            )
                                            DragTarget.MOVE -> {
                                                val cw = cur.width
                                                val ch = cur.height
                                                val nl = (cur.left + dx).coerceIn(0f, 1f - cw)
                                                val nt = (cur.top + dy).coerceIn(0f, 1f - ch)
                                                CropRect(nl, nt, nl + cw, nt + ch)
                                            }
                                            DragTarget.NONE -> cur
                                        }
                                        draft = draft.copy(cropRect = next)
                                    },
                                    onDragEnd = {
                                        activeHandle = DragTarget.NONE
                                        isDraggingCrop = false
                                    },
                                    onDragCancel = {
                                        activeHandle = DragTarget.NONE
                                        isDraggingCrop = false
                                    }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val cr = draft.cropRect
                        val l = cr.left * w
                        val t = cr.top * h
                        val r = cr.right * w
                        val b = cr.bottom * h

                        // Dimmed outside regions
                        val dim = Color.Black.copy(alpha = 0.60f)
                        drawRect(dim, Offset.Zero, Size(w, t))
                        drawRect(dim, Offset(0f, b), Size(w, h - b))
                        drawRect(dim, Offset(0f, t), Size(l, b - t))
                        drawRect(dim, Offset(r, t), Size(w - r, b - t))

                        // 3x3 Rule-of-thirds grid while dragging
                        if (isDraggingCrop) {
                            val gridColor = Color.White.copy(alpha = 0.55f)
                            val cw = r - l
                            val ch = b - t
                            for (i in 1..2) {
                                val gx = l + cw * (i / 3f)
                                drawLine(gridColor, Offset(gx, t), Offset(gx, b), strokeWidth = 1.dp.toPx())
                                val gy = t + ch * (i / 3f)
                                drawLine(gridColor, Offset(l, gy), Offset(r, gy), strokeWidth = 1.dp.toPx())
                            }
                        }

                        // White border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(l, t),
                            size = Size(r - l, b - t),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Thick L-shaped corner handles
                        val handleLen = 18.dp.toPx()
                        val handleStroke = 3.5.dp.toPx()
                        val cornerPath = Path().apply {
                            // Top-left
                            moveTo(l, t + handleLen); lineTo(l, t); lineTo(l + handleLen, t)
                            // Top-right
                            moveTo(r - handleLen, t); lineTo(r, t); lineTo(r, t + handleLen)
                            // Bottom-left
                            moveTo(l, b - handleLen); lineTo(l, b); lineTo(l + handleLen, b)
                            // Bottom-right
                            moveTo(r - handleLen, b); lineTo(r, b); lineTo(r, b - handleLen)
                        }
                        drawPath(
                            path = cornerPath,
                            color = Color.White,
                            style = Stroke(width = handleStroke, cap = StrokeCap.Square)
                        )
                    }
                }
            }
        }

        // Mirror (left) + Degree Ruler -45..+45 (center) + Rotate 90 (right)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface.copy(alpha = 0.96f))
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val angleFormatted = String.format("%.1f°", draft.straightenDegrees).replace('.', ',')
            Text(
                text = angleFormatted,
                color = if (abs(draft.straightenDegrees) > 0.05f) scheme.accent else scheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mirror on the LEFT
                IconButton(
                    onClick = { draft = draft.copy(mirrorHorizontal = !draft.mirrorHorizontal) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (draft.mirrorHorizontal) scheme.accent.copy(alpha = 0.18f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = strings.mirror,
                        tint = if (draft.mirrorHorizontal) scheme.accent else scheme.textPrimary
                    )
                }

                // Degree ruler -45°..+45° in the CENTER
                StraightenRuler(
                    degrees = draft.straightenDegrees,
                    onDegreesChange = { draft = draft.copy(straightenDegrees = it) },
                    accentColor = scheme.accent,
                    tickColor = scheme.textMuted,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .padding(horizontal = 8.dp)
                )

                // Rotate 90° on the RIGHT
                IconButton(
                    onClick = {
                        draft = draft.copy(baseRotation90 = (draft.baseRotation90 + 90) % 360)
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = strings.rotate90,
                        tint = if (draft.baseRotation90 != 0) scheme.accent else scheme.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom bar: ОТМЕНА | СБРОС | ГОТОВО
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.cancel,
                    color = scheme.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onCancel() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Text(
                    text = strings.reset,
                    color = if (draft.isModified) scheme.textPrimary else scheme.textMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = draft.isModified) { draft = CropTransform() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Text(
                    text = strings.done,
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onApply(draft) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StraightenRuler(
    degrees: Float,
    onDegreesChange: (Float) -> Unit,
    accentColor: Color,
    tickColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaDeg = -(dragAmount.x / size.width.toFloat().coerceAtLeast(1f)) * 45f
                    val next = (degrees + deltaDeg).coerceIn(-45f, 45f)
                    val snapped = if (abs(next) < 0.6f) 0f else (next * 10f).roundToInt() / 10f
                    onDegreesChange(snapped)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val pxPerDeg = w / 46f

        for (deg in -45..45 step 2) {
            val x = cx + (deg - degrees) * pxPerDeg
            if (x < 4f || x > w - 4f) continue
            val isMajor = deg % 10 == 0
            val isZero = deg == 0
            val lineH = when {
                isZero -> h * 0.62f
                isMajor -> h * 0.45f
                else -> h * 0.25f
            }
            val alpha = (1f - abs(x - cx) / (w * 0.52f)).coerceIn(0.15f, 1f)
            val color = if (isZero) accentColor.copy(alpha = alpha) else tickColor.copy(alpha = alpha)
            drawLine(
                color = color,
                start = Offset(x, (h - lineH) / 2f),
                end = Offset(x, (h + lineH) / 2f),
                strokeWidth = if (isZero || isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Fixed center indicator needle
        drawLine(
            color = accentColor,
            start = Offset(cx, h * 0.12f),
            end = Offset(cx, h * 0.88f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun computeFittedRect(container: IntSize, aspect: Float): Rect {
    if (container.width <= 0 || container.height <= 0) return Rect.Zero
    val cw = container.width.toFloat()
    val ch = container.height.toFloat()
    val containerAspect = cw / ch
    return if (aspect > containerAspect) {
        val h = cw / aspect
        Rect(0f, (ch - h) / 2f, cw, (ch + h) / 2f)
    } else {
        val w = ch * aspect
        Rect((cw - w) / 2f, 0f, (cw + w) / 2f, ch)
    }
}
