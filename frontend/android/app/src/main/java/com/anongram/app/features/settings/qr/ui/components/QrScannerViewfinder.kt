package com.anongram.app.features.settings.qr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Рамка видоискателя сканера QR-кодов (Макет 4):
 * отрисовывает квадратную область сканирования с четырьмя белыми закруглёнными уголками
 * и полупрозрачным внутренним свечением.
 */
@Composable
fun QrScannerViewfinder(
    frameSize: Dp = 260.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(frameSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(frameSize)) {
            val w = size.width
            val h = size.height
            val cornerLen = 36.dp.toPx()
            val radius = 16.dp.toPx()
            val strokeW = 4.dp.toPx()

            // Слегка осветлённый прямоугольник внутри видоискателя
            drawRect(
                color = Color.White.copy(alpha = 0.08f)
            )

            val cornersPath = Path().apply {
                // Верхний левый уголок
                moveTo(0f, cornerLen)
                lineTo(0f, radius)
                quadraticTo(0f, 0f, radius, 0f)
                lineTo(cornerLen, 0f)

                // Верхний правый уголок
                moveTo(w - cornerLen, 0f)
                lineTo(w - radius, 0f)
                quadraticTo(w, 0f, w, radius)
                lineTo(w, cornerLen)

                // Нижний правый уголок
                moveTo(w, h - cornerLen)
                lineTo(w, h - radius)
                quadraticTo(w, h, w - radius, h)
                lineTo(w - cornerLen, h)

                // Нижний левый уголок
                moveTo(cornerLen, h)
                lineTo(radius, h)
                quadraticTo(0f, h, 0f, h - radius)
                lineTo(0f, h - cornerLen)
            }

            drawPath(
                path = cornersPath,
                color = Color.White,
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
