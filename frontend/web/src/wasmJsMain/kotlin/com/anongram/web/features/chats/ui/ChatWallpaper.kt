package com.anongram.web.features.chats.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.anongram.web.core.theme.LocalAnonGramScheme

@Composable
fun ChatWallpaper(
    style: Int = 0,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val wallpaperDot = if (scheme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val bgColor = scheme.background
                val mode = style % 3
                val dotPositions = ArrayList<Offset>(256)
                val meshPath = Path()
                var dotRadiusPx = 1.1.dp.toPx()
                var dotColor = wallpaperDot
                var strokeColor = scheme.accent.copy(alpha = 0.05f)
                val strokeWidthPx = 1.dp.toPx()

                when (mode) {
                    0 -> {
                        val gridStep = 26.dp.toPx()
                        val rowStep = gridStep * 0.86f
                        dotRadiusPx = 1.1.dp.toPx()
                        var row = 0
                        var y = 0f
                        while (y <= size.height + rowStep) {
                            val offset = if (row % 2 == 0) 0f else gridStep / 2f
                            var x = offset
                            while (x <= size.width + gridStep) {
                                dotPositions.add(Offset(x, y))
                                x += gridStep
                            }
                            y += rowStep
                            row += 1
                        }
                        val meshStep = 104.dp.toPx()
                        val span = size.width + size.height
                        var d = -size.height
                        while (d <= size.width) {
                            meshPath.moveTo(d, 0f)
                            meshPath.lineTo(d + size.height, size.height)
                            d += meshStep
                        }
                        var u = 0f
                        while (u <= span) {
                            meshPath.moveTo(u, 0f)
                            meshPath.lineTo(u - size.height, size.height)
                            u += meshStep
                        }
                    }
                    1 -> {
                        val step = 48.dp.toPx()
                        dotColor = scheme.accent.copy(alpha = 0.08f)
                        strokeColor = scheme.accent.copy(alpha = 0.03f)
                        dotRadiusPx = 2.dp.toPx()
                        var y = 0f
                        var row = 0
                        while (y <= size.height + step) {
                            val offsetX = if (row % 2 == 0) 0f else step / 2f
                            var x = offsetX
                            while (x <= size.width + step) {
                                dotPositions.add(Offset(x, y))
                                if (x + step <= size.width + step) {
                                    meshPath.moveTo(x, y)
                                    meshPath.lineTo(x + step, y)
                                }
                                x += step
                            }
                            y += step * 0.866f
                            row++
                        }
                    }
                    else -> {
                        val colStep = 32.dp.toPx()
                        strokeColor = scheme.accent.copy(alpha = 0.04f)
                        var x = 0f
                        while (x <= size.width) {
                            meshPath.moveTo(x, 0f)
                            meshPath.lineTo(x, size.height)
                            x += colStep
                        }
                    }
                }

                onDrawBehind {
                    drawRect(bgColor)
                    dotPositions.forEach { pos ->
                        drawCircle(color = dotColor, radius = dotRadiusPx, center = pos)
                    }
                    drawPath(path = meshPath, color = strokeColor, style = Stroke(width = strokeWidthPx))
                }
            }
    )
}
