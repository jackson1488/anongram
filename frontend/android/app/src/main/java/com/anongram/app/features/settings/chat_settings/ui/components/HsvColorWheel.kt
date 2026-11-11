package com.anongram.app.features.settings.chat_settings.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ThemePaletteEngine
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Нативный Compose Canvas-компонент цветового круга (Hue 0..360° × Saturation 0..1).
 * Поддерживает как одиночный тап, так и плавное перетаскивание пальцем без конфликта со скроллом.
 */
@Composable
fun HsvColorWheel(
    hue: Float,
    saturation: Float,
    onColorChange: (newHue: Float, newSaturation: Float) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 184.dp
) {
    val scheme = LocalAnonGramScheme.current
    val updatedCallback by rememberUpdatedState(onColorChange)

    val rainbowColors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red
    )

    val selectedPreviewColor = ThemePaletteEngine.hslToColor(hue, saturation.coerceIn(0f, 1f), 0.54f)

    Canvas(
        modifier = modifier
            .size(wheelSize)
            .pointerInput(Unit) {
                fun emitPoint(pos: Offset) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxRadius = (minOf(size.width, size.height) / 2f - 4f).coerceAtLeast(1f)
                    val dx = pos.x - cx
                    val dy = pos.y - cy
                    val angle = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                    val dist = hypot(dx, dy)
                    val sat = (dist / maxRadius).coerceIn(0f, 1f)
                    updatedCallback(angle, sat)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    emitPoint(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            change.consume()
                            emitPoint(change.position)
                        }
                    } while (change.pressed)
                }
            }
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Внешнее радужное кольцо (Hue)
        drawCircle(
            brush = Brush.sweepGradient(
                colors = rainbowColors,
                center = center
            ),
            radius = radius,
            center = center
        )

        // 2. Радиальное обесцвечивание к центру (Saturation = 0..1)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Тонкий внешний контур круга (адаптируется под светлую и тёмную тему)
        drawCircle(
            color = scheme.cardBorder,
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 3. Маркер активной точки
        val usableRadius = (radius - 6.dp.toPx()).coerceAtLeast(1f)
        val angleRad = hue * (PI.toFloat() / 180f)
        val thumbDist = saturation.coerceIn(0f, 1f) * usableRadius
        val thumbCenter = Offset(
            x = center.x + cos(angleRad) * thumbDist,
            y = center.y + sin(angleRad) * thumbDist
        )

        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = 12.dp.toPx(),
            center = thumbCenter
        )
        drawCircle(
            color = Color.White,
            radius = 10.5.dp.toPx(),
            center = thumbCenter
        )
        drawCircle(
            color = selectedPreviewColor,
            radius = 7.5.dp.toPx(),
            center = thumbCenter
        )
    }
}
