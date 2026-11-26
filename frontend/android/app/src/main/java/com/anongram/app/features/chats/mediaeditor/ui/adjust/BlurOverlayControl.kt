package com.anongram.app.features.chats.mediaeditor.ui.adjust

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import com.anongram.app.features.chats.mediaeditor.model.BlurEffectState
import com.anongram.app.features.chats.mediaeditor.model.BlurMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun BlurOverlayControl(
    blur: BlurEffectState,
    onBlurChange: (BlurEffectState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (blur.mode == BlurMode.OFF) return

    val currentBlur by rememberUpdatedState(blur)
    val currentOnBlurChange by rememberUpdatedState(onBlurChange)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(blur.mode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val minDim = minOf(w, h).coerceAtLeast(1f)

                    val startState = currentBlur
                    val cxPx = startState.centerX * w
                    val cyPx = startState.centerY * h
                    val rPx = startState.radius * minDim
                    val distFromCenter = hypot(down.position.x - cxPx, down.position.y - cyPx)

                    // If single finger touches near the ring edge (within 32.dp), dragging adjusts radius; otherwise moves center
                    val ringHitTolerance = 32.dp.toPx()
                    val isDraggingRingEdge = abs(distFromCenter - rPx) <= ringHitTolerance ||
                        abs(distFromCenter - rPx * 1.45f) <= ringHitTolerance

                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        val latest = currentBlur
                        if (pressed.size >= 2) {
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            val nx = (latest.centerX + pan.x / w).coerceIn(0.05f, 0.95f)
                            val ny = (latest.centerY + pan.y / h).coerceIn(0.05f, 0.95f)
                            val nr = (latest.radius * zoom).coerceIn(0.08f, 0.75f)
                            val na = (latest.angleDegrees + rotation) % 360f
                            currentOnBlurChange(
                                latest.copy(
                                    centerX = nx,
                                    centerY = ny,
                                    radius = nr,
                                    angleDegrees = na
                                )
                            )
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            val change = pressed.first()
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                if (isDraggingRingEdge) {
                                    val newDist = hypot(
                                        change.position.x - latest.centerX * w,
                                        change.position.y - latest.centerY * h
                                    )
                                    val nr = (newDist / minDim).coerceIn(0.08f, 0.75f)
                                    currentOnBlurChange(latest.copy(radius = nr))
                                } else {
                                    val nx = (latest.centerX + pan.x / w).coerceIn(0.05f, 0.95f)
                                    val ny = (latest.centerY + pan.y / h).coerceIn(0.05f, 0.95f)
                                    currentOnBlurChange(latest.copy(centerX = nx, centerY = ny))
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val cx = blur.centerX * size.width
        val cy = blur.centerY * size.height
        val minDim = minOf(size.width, size.height)
        val r = blur.radius * minDim
        val ringColor = Color.White.copy(alpha = 0.88f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

        when (blur.mode) {
            BlurMode.RADIAL -> {
                drawCircle(
                    color = ringColor,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx(), pathEffect = dashEffect)
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.48f),
                    radius = r * 1.45f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.3.dp.toPx(), pathEffect = dashEffect)
                )
                drawCircle(
                    color = ringColor,
                    radius = 5.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
            BlurMode.LINEAR -> {
                val rad = Math.toRadians(blur.angleDegrees.toDouble())
                val dx = (cos(rad) * size.maxDimension).toFloat()
                val dy = (sin(rad) * size.maxDimension).toFloat()
                val nx = (-sin(rad) * r).toFloat()
                val ny = (cos(rad) * r).toFloat()
                val nxOuter = (-sin(rad) * r * 1.45f).toFloat()
                val nyOuter = (cos(rad) * r * 1.45f).toFloat()

                drawLine(
                    color = ringColor,
                    start = Offset(cx + nx - dx, cy + ny - dy),
                    end = Offset(cx + nx + dx, cy + ny + dy),
                    strokeWidth = 1.7.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = ringColor,
                    start = Offset(cx - nx - dx, cy - ny - dy),
                    end = Offset(cx - nx + dx, cy - ny + dy),
                    strokeWidth = 1.7.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = ringColor.copy(alpha = 0.45f),
                    start = Offset(cx + nxOuter - dx, cy + nyOuter - dy),
                    end = Offset(cx + nxOuter + dx, cy + nyOuter + dy),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = ringColor.copy(alpha = 0.45f),
                    start = Offset(cx - nxOuter - dx, cy - nyOuter - dy),
                    end = Offset(cx - nxOuter + dx, cy - nyOuter + dy),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawCircle(
                    color = ringColor,
                    radius = 5.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
            BlurMode.OFF -> Unit
        }
    }
}
