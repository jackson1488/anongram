package com.anongram.app.features.chats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack

private val WallpaperDot = Color(0xFF18202E)

/**
 * Узорные обои чата: 3 стиля (переключаются по «Изменить обои» в меню):
 * 0: Точечная сетка + крипто-mesh акцентом темы (по умолчанию)
 * 1: Созвездие узлов и связей
 * 2: Минималистичные вертикальные шифрованные линии
 */
@Composable
fun ChatWallpaper(
    style: Int = 0,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = ObsidianBlack)

        when (style % 3) {
            0 -> {
                // Стиль 0: Точечная сетка + диагональная крипто-сетка
                val gridStep = 26.dp.toPx()
                val rowStep = gridStep * 0.86f
                val dotRadius = 1.1.dp.toPx()

                var row = 0
                var y = 0f
                while (y <= size.height + rowStep) {
                    val offset = if (row % 2 == 0) 0f else gridStep / 2f
                    var x = offset
                    while (x <= size.width + gridStep) {
                        drawCircle(color = WallpaperDot, radius = dotRadius, center = Offset(x, y))
                        x += gridStep
                    }
                    y += rowStep
                    row += 1
                }

                val meshColor = scheme.accent.copy(alpha = 0.05f)
                val meshStroke = 1.dp.toPx()
                val meshStep = 104.dp.toPx()
                val span = size.width + size.height

                var d = -size.height
                while (d <= size.width) {
                    drawLine(
                        color = meshColor,
                        start = Offset(d, 0f),
                        end = Offset(d + size.height, size.height),
                        strokeWidth = meshStroke
                    )
                    d += meshStep
                }

                var u = 0f
                while (u <= span) {
                    drawLine(
                        color = meshColor,
                        start = Offset(u, 0f),
                        end = Offset(u - size.height, size.height),
                        strokeWidth = meshStroke
                    )
                    u += meshStep
                }
            }
            1 -> {
                // Стиль 1: Созвездие гексагональных узлов
                val step = 48.dp.toPx()
                val nodeColor = scheme.accent.copy(alpha = 0.08f)
                val lineColor = scheme.accent.copy(alpha = 0.03f)

                var y = 0f
                var row = 0
                while (y <= size.height + step) {
                    val offsetX = if (row % 2 == 0) 0f else step / 2f
                    var x = offsetX
                    while (x <= size.width + step) {
                        drawCircle(color = nodeColor, radius = 2.dp.toPx(), center = Offset(x, y))
                        if (x + step <= size.width + step) {
                            drawLine(
                                color = lineColor,
                                start = Offset(x, y),
                                end = Offset(x + step, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        x += step
                    }
                    y += step * 0.866f
                    row++
                }
            }
            else -> {
                // Стиль 2: Вертикальные микро-полосы потока
                val colStep = 32.dp.toPx()
                val stripeColor = scheme.accent.copy(alpha = 0.04f)
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += colStep
                }
            }
        }
    }
}
