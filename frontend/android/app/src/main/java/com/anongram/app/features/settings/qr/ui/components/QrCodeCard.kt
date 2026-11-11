package com.anongram.app.features.settings.qr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Белая скруглённая карточка QR-кода профиля (Макет 3):
 * - Отрисовывает стилизованный скруглённый QR-код фиолетового цвета;
 * - В центре QR-кода размещён фиолетовый круг с белым оригами-самолётиком;
 * - Под QR-кодом — крупный фиолетовый хэндл пользователя (@KNSHVK).
 *
 * @param handleText Хэндл пользователя (например, "@KNSHVK").
 * @param qrColor Акцентный цвет модулей QR-кода.
 */
@Composable
fun QrCodeCard(
    handleText: String,
    qrColor: Color = Color(0xFF9353D3),
    modifier: Modifier = Modifier
) {
    val effectiveColor = if (qrColor == Color(0xFF18181B)) Color.Black else qrColor

    Column(
        modifier = modifier
            .width(300.dp)
            .shadow(20.dp, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(Color.White)
            .padding(horizontal = 28.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val gridSize = 21
                val cellSize = size.width / gridSize
                val seedHash = handleText.hashCode()

                // Функция отрисовки углового поискового квадрата (Finder Pattern)
                fun drawFinderPattern(startX: Int, startY: Int) {
                    val x = startX * cellSize
                    val y = startY * cellSize
                    val outerSize = 7 * cellSize
                    // Внешняя рамка
                    drawRoundRect(
                        color = effectiveColor,
                        topLeft = Offset(x + cellSize * 0.4f, y + cellSize * 0.4f),
                        size = Size(outerSize - cellSize * 0.8f, outerSize - cellSize * 0.8f),
                        cornerRadius = CornerRadius(cellSize * 1.8f, cellSize * 1.8f),
                        style = Stroke(width = cellSize * 0.95f)
                    )
                    // Внутренний квадрат
                    drawRoundRect(
                        color = effectiveColor,
                        topLeft = Offset(x + cellSize * 2f, y + cellSize * 2f),
                        size = Size(cellSize * 3f, cellSize * 3f),
                        cornerRadius = CornerRadius(cellSize * 0.9f, cellSize * 0.9f)
                    )
                }

                // Три угловых поисковых маркера
                drawFinderPattern(0, 0)
                drawFinderPattern(gridSize - 7, 0)
                drawFinderPattern(0, gridSize - 7)

                // Модули данных QR-кода
                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        val inTopLeft = row < 8 && col < 8
                        val inTopRight = row < 8 && col >= gridSize - 8
                        val inBottomLeft = row >= gridSize - 8 && col < 8
                        val inCenterLogo = row in 8..12 && col in 8..12

                        if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenterLogo) {
                            val hashVal = abs((row * 31 + col * 17 + seedHash) % 11)
                            if (hashVal < 6) {
                                drawRoundRect(
                                    color = effectiveColor,
                                    topLeft = Offset(col * cellSize + 1f, row * cellSize + 1f),
                                    size = Size(cellSize - 2f, cellSize - 2f),
                                    cornerRadius = CornerRadius(cellSize * 0.35f, cellSize * 0.35f)
                                )
                            }
                        }
                    }
                }

                // Центральный круг с белым самолётиком
                val centerRadius = cellSize * 2.1f
                drawCircle(
                    color = effectiveColor,
                    radius = centerRadius,
                    center = center
                )

                val planePath = Path().apply {
                    val cx = center.x
                    val cy = center.y
                    val r = centerRadius * 0.55f
                    moveTo(cx + r * 0.85f, cy - r * 0.75f)
                    lineTo(cx - r * 0.95f, cy + r * 0.1f)
                    lineTo(cx - r * 0.15f, cy + r * 0.25f)
                    lineTo(cx + r * 0.25f, cy + r * 0.95f)
                    close()
                }
                drawPath(path = planePath, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = handleText,
            color = effectiveColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
