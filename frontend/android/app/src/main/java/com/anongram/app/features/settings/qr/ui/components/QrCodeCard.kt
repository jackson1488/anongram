package com.anongram.app.features.settings.qr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Белая скруглённая карточка QR-кода профиля AnonGram (Скриншот 3).
 *
 * Особенности:
 * - Реальная генерация QR-кода через ZXing с высоким уровнем коррекции ошибок (ErrorCorrectionLevel.H);
 * - Стилизованные скруглённые модули и угловые маркеры;
 * - В центре — фирменная Стелс-иконка приложения (Cryptographic Shield + Stealth Origami Plane);
 * - Под QR-кодом — крупный контрастный хэндл пользователя (@HANDLE);
 * - Корректная поддержка тёмной темы (чистый чёрный цвет) и цветных акцентов.
 *
 * @param handleText Хэндл пользователя (например, "@KNSHVKAI").
 * @param qrPayload Полезная нагрузка (ссылка или URI профиля).
 * @param qrColor Акцентный цвет QR-кода и хэндла.
 */
@Composable
fun QrCodeCard(
    handleText: String,
    qrPayload: String = "anongram://user?handle=${handleText.removePrefix("@").uppercase()}",
    qrColor: Color = Color(0xFF18181B),
    modifier: Modifier = Modifier
) {
    val effectiveColor = if (qrColor == Color(0xFF18181B)) Color.Black else qrColor

    // Реальная генерация матрицы QR-кода через ZXing с коррекцией ошибок H (30% избыточности)
    val bitMatrix = remember(qrPayload) {
        runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 0,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 0, 0, hints)
        }.getOrNull()
    }

    Column(
        modifier = modifier
            .width(330.dp)
            .shadow(24.dp, RoundedCornerShape(36.dp), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(36.dp))
            .background(Color.White)
            .padding(horizontal = 26.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(256.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(256.dp)) {
                val matrix = bitMatrix
                val gridSize = matrix?.width ?: 29
                val cellSize = size.width / gridSize

                // Функция отрисовки углового поискового маркера с мягкими скруглениями
                fun drawFinderPattern(startX: Int, startY: Int) {
                    val x = startX * cellSize
                    val y = startY * cellSize
                    val outerSize = 7 * cellSize

                    // Внешняя скруглённая рамка
                    drawRoundRect(
                        color = effectiveColor,
                        topLeft = Offset(x + cellSize * 0.4f, y + cellSize * 0.4f),
                        size = Size(outerSize - cellSize * 0.8f, outerSize - cellSize * 0.8f),
                        cornerRadius = CornerRadius(cellSize * 1.8f, cellSize * 1.8f),
                        style = Stroke(width = cellSize * 0.95f)
                    )

                    // Внутренний скруглённый квадрат
                    drawRoundRect(
                        color = effectiveColor,
                        topLeft = Offset(x + cellSize * 2f, y + cellSize * 2f),
                        size = Size(cellSize * 3f, cellSize * 3f),
                        cornerRadius = CornerRadius(cellSize * 0.9f, cellSize * 0.9f)
                    )
                }

                // 1. Три угловых поисковых маркера
                drawFinderPattern(0, 0)
                drawFinderPattern(gridSize - 7, 0)
                drawFinderPattern(0, gridSize - 7)

                // 2. Модули данных реального QR-кода
                val centerCol = gridSize / 2f
                val centerRow = gridSize / 2f
                val centerCutoutRadius = gridSize * 0.20f // Радиус выреза под центральную стелс-иконку

                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        // Пропуск угловых зон поисковых маркеров (включая защитные поля 8x8)
                        val inTopLeft = row < 8 && col < 8
                        val inTopRight = row < 8 && col >= gridSize - 8
                        val inBottomLeft = row >= gridSize - 8 && col < 8

                        // Пропуск центральной зоны для стелс-иконки приложения
                        val dx = col + 0.5f - centerCol
                        val dy = row + 0.5f - centerRow
                        val inCenterBadge = (dx * dx + dy * dy) <= (centerCutoutRadius * centerCutoutRadius)

                        if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenterBadge) {
                            val isModule = matrix?.get(col, row) ?: false
                            if (isModule) {
                                drawRoundRect(
                                    color = effectiveColor,
                                    topLeft = Offset(col * cellSize + 0.8f, row * cellSize + 0.8f),
                                    size = Size(cellSize - 1.6f, cellSize - 1.6f),
                                    cornerRadius = CornerRadius(cellSize * 0.35f, cellSize * 0.35f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Фирменная Стелс-иконка приложения в центре QR-кода (Cryptographic Shield + Stealth Plane)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(effectiveColor)
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stat_anongram),
                    contentDescription = "AnonGram Stealth Logo",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Хэндл пользователя крупным жирным шрифтом
        Text(
            text = handleText,
            color = effectiveColor,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}
