package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import kotlin.random.Random

/**
 * Интерактивный анимированный спойлер с мерцающими вращающимися частицами (пыль/звёздочки).
 * По тапу плавно открывает текст, по повторному тапу — скрывает обратно.
 */
@Composable
fun BubbleSpoilerText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    var isRevealed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "spoilerSparkle")
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparklePhase"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { isRevealed = !isRevealed }
    ) {
        // Текст сообщения: если спойлер активен, альфа 0 (или размыт), но занимает реальный размер
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .alpha(if (isRevealed) 1f else 0.05f)
                .padding(horizontal = 2.dp, vertical = 1.dp)
        )

        // Завеса анимированных мерцающих частиц
        if (!isRevealed) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val numDots = ((width * height) / 120f).toInt().coerceIn(30, 250)

                // Детерминированный генератор для стабильного расположения частиц
                val rnd = Random(42)
                for (i in 0 until numDots) {
                    val x = rnd.nextFloat() * width
                    val y = rnd.nextFloat() * height
                    val radius = rnd.nextFloat() * 1.8f + 0.8f
                    val speed = rnd.nextFloat() * 0.8f + 0.2f
                    val dotAlpha = ((kotlin.math.sin((sparklePhase * 2 * Math.PI * speed) + i).toFloat() + 1f) / 2f)
                        .coerceIn(0.2f, 0.95f)

                    val color = if (i % 3 == 0) {
                        Color(0xFFC084FC).copy(alpha = dotAlpha) // фиолетовая искорка
                    } else if (i % 5 == 0) {
                        Color.White.copy(alpha = dotAlpha)
                    } else {
                        Color(0xFF8B5CF6).copy(alpha = dotAlpha * 0.8f)
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
