package com.anongram.app.features.chats.mediaeditor.ui.crop

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.pow
import kotlin.math.sin

private enum class DragTarget {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, RIGHT, TOP, BOTTOM, MOVE
}

@Composable
fun CropRotatePanel(
    bitmap: ImageBitmap?,
    initialTransform: CropTransform,
    isVideo: Boolean = false,
    onLiveChange: ((CropTransform) -> Unit)? = null,
    onApply: (CropTransform) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val density = LocalDensity.current

    var draft by remember(initialTransform) { mutableStateOf(initialTransform) }
    var cumulativeRotation90 by remember(initialTransform) {
        mutableFloatStateOf(initialTransform.baseRotation90.toFloat())
    }
    var isDraggingCrop by remember { mutableStateOf(false) }
    var isDraggingWheel by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    androidx.compose.runtime.LaunchedEffect(draft) {
        onLiveChange?.invoke(draft)
    }

    val animatedBaseRotation by animateFloatAsState(
        targetValue = cumulativeRotation90,
        animationSpec = tween(durationMillis = 240),
        label = "cropBaseRotation90"
    )
    val animatedMirrorScale by animateFloatAsState(
        targetValue = if (draft.mirrorHorizontal) -1f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "cropMirrorScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isVideo) Color.Transparent else Color.Black)
    ) {
        // Fullscreen Crop Workspace with slim edge margin and space for bottom floating controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 108.dp)
                .onSizeChanged { boxSize = it },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null && boxSize.width > 0 && boxSize.height > 0) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                val swapped = (draft.baseRotation90 / 90) % 2 != 0
                val displayAspect = if (swapped) 1f / aspect else aspect

                val fittedRect = remember(boxSize, displayAspect) {
                    computeFittedRect(boxSize, displayAspect)
                }

                val rad = Math.toRadians(abs(draft.straightenDegrees).toDouble())
                val autoZoom = (cos(rad) + sin(rad) * maxOf(displayAspect, 1f / displayAspect)).toFloat()
                    .coerceIn(1f, 1.85f)

                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { fittedRect.width.toDp() },
                            height = with(density) { fittedRect.height.toDp() }
                        )
                ) {
                    if (!isVideo) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = animatedBaseRotation + draft.straightenDegrees
                                    scaleX = animatedMirrorScale * autoZoom
                                    scaleY = autoZoom
                                }
                        )
                    }

                    val currentDraft by rememberUpdatedState(draft)
                    var activeHandle by remember { mutableStateOf(DragTarget.NONE) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val r = currentDraft.cropRect
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
                                            abs(offset.x - l) <= hitR && offset.y in t..b -> DragTarget.LEFT
                                            abs(offset.x - ri) <= hitR && offset.y in t..b -> DragTarget.RIGHT
                                            abs(offset.y - t) <= hitR && offset.x in l..ri -> DragTarget.TOP
                                            abs(offset.y - b) <= hitR && offset.x in l..ri -> DragTarget.BOTTOM
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
                                        val cur = currentDraft.cropRect
                                        val minSize = 0.14f

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
                                            DragTarget.LEFT -> cur.copy(
                                                left = (cur.left + dx).coerceIn(0f, cur.right - minSize)
                                            )
                                            DragTarget.RIGHT -> cur.copy(
                                                right = (cur.right + dx).coerceIn(cur.left + minSize, 1f)
                                            )
                                            DragTarget.TOP -> cur.copy(
                                                top = (cur.top + dy).coerceIn(0f, cur.bottom - minSize)
                                            )
                                            DragTarget.BOTTOM -> cur.copy(
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
                                        draft = currentDraft.copy(cropRect = next)
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
                        val dim = Color.Black.copy(alpha = 0.58f)
                        drawRect(dim, Offset.Zero, Size(w, t))
                        drawRect(dim, Offset(0f, b), Size(w, h - b))
                        drawRect(dim, Offset(0f, t), Size(l, b - t))
                        drawRect(dim, Offset(r, t), Size(w - r, b - t))

                        // 3x3 grid while dragging crop or rotating wheel
                        if (isDraggingCrop || isDraggingWheel) {
                            val gridColor = Color.White.copy(alpha = 0.45f)
                            val cw = r - l
                            val ch = b - t
                            for (i in 1..2) {
                                val gx = l + cw * (i / 3f)
                                drawLine(gridColor, Offset(gx, t), Offset(gx, b), strokeWidth = 1.dp.toPx())
                                val gy = t + ch * (i / 3f)
                                drawLine(gridColor, Offset(l, gy), Offset(r, gy), strokeWidth = 1.dp.toPx())
                            }
                        }

                        // Thin white crop frame border
                        drawRect(
                            color = Color.White.copy(alpha = 0.85f),
                            topLeft = Offset(l, t),
                            size = Size(r - l, b - t),
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // Bold L-shaped white corner handles
                        val handleLen = 20.dp.toPx()
                        val handleStroke = 3.5.dp.toPx()
                        val cornerPath = Path().apply {
                            moveTo(l, t + handleLen); lineTo(l, t); lineTo(l + handleLen, t)
                            moveTo(r - handleLen, t); lineTo(r, t); lineTo(r, t + handleLen)
                            moveTo(l, b - handleLen); lineTo(l, b); lineTo(l + handleLen, b)
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

        // Seamless bottom gradient + floating 3D drum wheel & bottom action bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(top = 14.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val angleFormatted = String.format("%.1f°", draft.straightenDegrees).replace('.', ',')
            Text(
                text = angleFormatted,
                color = if (abs(draft.straightenDegrees) > 0.05f) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mirror on the LEFT
                IconButton(
                    onClick = { draft = draft.copy(mirrorHorizontal = !draft.mirrorHorizontal) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = strings.mirror,
                        tint = if (draft.mirrorHorizontal) scheme.accent else Color.White
                    )
                }

                // 3D Cylindrical Drum Wheel (-45°..+45°) in the CENTER
                StraightenDrumWheel3D(
                    degrees = draft.straightenDegrees,
                    onDegreesChange = { newDeg ->
                        draft = draft.copy(straightenDegrees = newDeg)
                    },
                    onDragStateChange = { isDraggingWheel = it },
                    accentColor = Color(0xFF3897F0),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .padding(horizontal = 6.dp)
                )

                // Rotate 90° on the RIGHT
                IconButton(
                    onClick = {
                        cumulativeRotation90 -= 90f
                        val next90 = ((draft.baseRotation90 - 90) % 360 + 360) % 360
                        draft = draft.copy(baseRotation90 = next90)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Rotate90DegreesCcw,
                        contentDescription = strings.rotate90,
                        tint = if (draft.baseRotation90 != 0) scheme.accent else Color.White
                    )
                }
            }

            // Bottom row: ОТМЕНА | СБРОС | ГОТОВО
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.cancel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onCancel() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )

                if (draft.isModified) {
                    Text(
                        text = strings.reset,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                cumulativeRotation90 = 0f
                                draft = CropTransform()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                Text(
                    text = strings.done,
                    color = scheme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onApply(draft) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 3D цилиндрический барабан градусов (-45°..+45°) в стиле Telegram:
 * Деления проецируются по цилиндру через sin(θ) и cos(θ), сжимаясь и уходя в перспективу к краям.
 */
@Composable
private fun StraightenDrumWheel3D(
    degrees: Float,
    onDegreesChange: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val currentDegrees by rememberUpdatedState(degrees)
    val currentOnChange by rememberUpdatedState(onDegreesChange)
    val currentOnDragState by rememberUpdatedState(onDragStateChange)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentOnDragState(true) },
                    onDragEnd = { currentOnDragState(false) },
                    onDragCancel = { currentOnDragState(false) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val deltaDeg = -(dragAmount.x / w) * 42f
                        val rawNext = (currentDegrees + deltaDeg).coerceIn(-45f, 45f)
                        val next = if (abs(rawNext) < 0.25f && abs(currentDegrees) >= 0.25f) 0f else rawNext
                        currentOnChange(next)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val drumRadius = w * 0.49f
        val visibleSpanDeg = 32f

        for (deg in -45..45 step 1) {
            if (deg % 2 != 0 && deg != 0) continue
            val relDeg = deg - degrees
            if (abs(relDeg) > visibleSpanDeg) continue

            // Cylindrical angle theta in [-PI*0.46 .. +PI*0.46]
            val theta = (relDeg / visibleSpanDeg) * (Math.PI.toFloat() * 0.46f)
            val sinT = sin(theta)
            val cosT = cos(theta).coerceIn(0f, 1f)

            val x = cx + drumRadius * sinT
            val isZero = deg == 0
            val isBetweenZeroAndCurrent = (deg >= minOf(0f, degrees) && deg <= maxOf(0f, degrees))

            // 3D perspective scaling on height, width, and alpha
            val baseH = if (isZero) h * 0.56f else h * 0.36f
            val lineH = baseH * (0.55f + 0.45f * cosT)
            val alpha = cosT.pow(1.45f).coerceIn(0.08f, 1f)

            val tickColor = when {
                isZero || isBetweenZeroAndCurrent -> accentColor.copy(alpha = alpha)
                else -> Color.White.copy(alpha = alpha * 0.78f)
            }

            drawLine(
                color = tickColor,
                start = Offset(x, (h - lineH) / 2f),
                end = Offset(x, (h + lineH) / 2f),
                strokeWidth = (if (isZero) 2.2.dp.toPx() else 1.3.dp.toPx()) * (0.6f + 0.4f * cosT),
                cap = StrokeCap.Round
            )
        }

        // Fixed center indicator bar (current angle needle)
        drawLine(
            color = accentColor,
            start = Offset(cx, h * 0.16f),
            end = Offset(cx, h * 0.84f),
            strokeWidth = 2.6.dp.toPx(),
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
