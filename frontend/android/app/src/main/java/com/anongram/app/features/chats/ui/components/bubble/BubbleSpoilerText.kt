package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Интерактивный анимированный спойлер с живыми плавающими частицами (Telegram Stardust Spoiler).
 * Миллионы плавающих искорок дрейфуют по поверхности фиолетовой вуали.
 * По тапу плавно растворяется и открывает скрытый текст, по повторному — скрывает обратно.
 */
@Composable
fun BubbleSpoilerText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    var isRevealed by remember { mutableStateOf(false) }

    val veilAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "veilAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spoilerFloatingParticles")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 31.4159f, // 10 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(8000),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTime"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { isRevealed = !isRevealed }
    ) {
        // Текст сообщения
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .alpha(if (isRevealed) 1f else 0.08f)
                .padding(horizontal = 3.dp, vertical = 1.dp)
        )

        // Живая вуаль с дрейфующими плавающими частицами Telegram
        if (veilAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(veilAlpha)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3F2B66).copy(alpha = 0.90f))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height
                    // Плотное облако плавающих частиц
                    val numDots = ((width * height) / 220f).toInt().coerceIn(28, 70)

                    for (i in 0 until numDots) {
                        // Начальные распределенные координаты
                        val seedX = ((i * 0.173f + 0.07f) % 1.0f)
                        val seedY = ((i * 0.281f + 0.13f) % 1.0f)

                        // Скорости дрейфа по осям X и Y
                        val driftSpeedX = 0.035f + (i % 5) * 0.012f
                        val driftSpeedY = 0.025f + (i % 7) * 0.009f

                        // Плавный дрейф с циклическим бесшовным переносом (wrap-around)
                        val curXNorm = ((seedX + animTime * driftSpeedX + sin(animTime * 0.8f + i) * 0.02f) % 1.0f + 1.0f) % 1.0f
                        val curYNorm = ((seedY + animTime * driftSpeedY + sin(animTime * 0.6f + i * 2) * 0.025f) % 1.0f + 1.0f) % 1.0f

                        val x = curXNorm * width
                        val y = curYNorm * height

                        // Радиус частицы и мерцание
                        val radius = (0.7f + (i % 3) * 0.45f).dp.toPx()
                        val wave = sin(animTime * 2.5f + i * 1.7f)
                        val dotAlpha = ((wave + 1f) / 2f).coerceIn(0.25f, 0.95f)

                        val color = when (i % 5) {
                            0 -> Color(0xFFE9D5FF).copy(alpha = dotAlpha)        // Нежный лавандовый
                            1 -> Color(0xFFC084FC).copy(alpha = dotAlpha)        // Насыщенный сиреневый
                            2 -> Color.White.copy(alpha = dotAlpha * 0.90f)      // Чистая белая искра
                            3 -> Color(0xFFA855F7).copy(alpha = dotAlpha * 0.80f)// Аметистовый
                            else -> Color(0xFFE0E7FF).copy(alpha = dotAlpha)     // Ледяной кристалл
                        }

                        drawCircle(
                            color = color,
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
