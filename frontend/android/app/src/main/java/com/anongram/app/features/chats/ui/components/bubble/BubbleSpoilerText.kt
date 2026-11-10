package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import kotlin.random.Random

/**
 * Интерактивный анимированный спойлер в стиле Telegram (макет media_1794316086489.png).
 * Деликатная фиолетовая вуаль с мягкими мерцающими искорками.
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

    val infiniteTransition = rememberInfiniteTransition(label = "spoilerSparkle")
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparklePhase"
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

        // Мягкая фиолетовая вуаль с деликатными мерцающими искорками как в Telegram
        if (veilAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(veilAlpha)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3F2B66).copy(alpha = 0.88f))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height
                    // Умеренное число искорок: от 16 до 34 точек, никакой каши статического шума
                    val numDots = ((width * height) / 450f).toInt().coerceIn(16, 34)

                    val rnd = Random(42)
                    for (i in 0 until numDots) {
                        val x = rnd.nextFloat() * width
                        val y = rnd.nextFloat() * height
                        val radius = rnd.nextFloat() * 1.0f + 0.6f
                        val speed = rnd.nextFloat() * 0.7f + 0.3f
                        val wave = kotlin.math.sin(sparklePhase * speed + i.toFloat()).toFloat()
                        val dotAlpha = ((wave + 1f) / 2f).coerceIn(0.2f, 0.95f)

                        val color = when (i % 4) {
                            0 -> Color(0xFFE9D5FF).copy(alpha = dotAlpha) // лавандовый
                            1 -> Color(0xFFC084FC).copy(alpha = dotAlpha) // фиолетовый
                            2 -> Color.White.copy(alpha = dotAlpha * 0.85f) // белый
                            else -> Color(0xFFA855F7).copy(alpha = dotAlpha * 0.75f) // аметистовый
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
