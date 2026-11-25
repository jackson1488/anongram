package com.anongram.app.features.chats.mediaeditor.ui.adjust

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.anongram.app.features.chats.mediaeditor.model.BlurEffectState
import com.anongram.app.features.chats.mediaeditor.model.BlurMode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BlurOverlayControl(
    blur: BlurEffectState,
    onBlurChange: (BlurEffectState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (blur.mode == BlurMode.OFF) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(blur.mode) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val nx = (blur.centerX + pan.x / w).coerceIn(0.05f, 0.95f)
                    val ny = (blur.centerY + pan.y / h).coerceIn(0.05f, 0.95f)
                    val nr = (blur.radius * zoom).coerceIn(0.10f, 0.75f)
                    val na = (blur.angleDegrees + rotation) % 360f
                    onBlurChange(
                        blur.copy(
                            centerX = nx,
                            centerY = ny,
                            radius = nr,
                            angleDegrees = na
                        )
                    )
                }
            }
    ) {
        val cx = blur.centerX * size.width
        val cy = blur.centerY * size.height
        val minDim = minOf(size.width, size.height)
        val r = blur.radius * minDim
        val ringColor = Color.White.copy(alpha = 0.85f)

        when (blur.mode) {
            BlurMode.RADIAL -> {
                drawCircle(
                    color = ringColor,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.45f),
                    radius = r * 1.45f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = ringColor,
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
            BlurMode.LINEAR -> {
                val rad = Math.toRadians(blur.angleDegrees.toDouble())
                val dx = (cos(rad) * size.maxDimension).toFloat()
                val dy = (sin(rad) * size.maxDimension).toFloat()
                val nx = (-sin(rad) * r).toFloat()
                val ny = (cos(rad) * r).toFloat()

                drawLine(
                    color = ringColor,
                    start = Offset(cx + nx - dx, cy + ny - dy),
                    end = Offset(cx + nx + dx, cy + ny + dy),
                    strokeWidth = 1.6.dp.toPx()
                )
                drawLine(
                    color = ringColor,
                    start = Offset(cx - nx - dx, cy - ny - dy),
                    end = Offset(cx - nx + dx, cy - ny + dy),
                    strokeWidth = 1.6.dp.toPx()
                )
                drawCircle(
                    color = ringColor,
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
            BlurMode.OFF -> Unit
        }
    }
}
