package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.core.LinearEasing
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
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlin.math.sin

/**
 * Интерактивный анимированный спойлер с живыми плавающими частицами (Telegram Stardust Spoiler).
 * По тапу плавно растворяется и открывает скрытый текст, по повторному — скрывает обратно.
 */
@Composable
fun BubbleSpoilerText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var isRevealed by remember { mutableStateOf(false) }

    val veilAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "veilAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spoilerFloatingParticles")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.8318f, // 20 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTime"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = !isRevealed) { isRevealed = true }
    ) {
        // Текст сообщения
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .alpha(if (isRevealed) 1f else 0.04f)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        // Живая вуаль с динамическими дрейфующими и мерцающими частицами
        if (veilAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(veilAlpha)
                    .clip(RoundedCornerShape(4.dp))
                    .background(scheme.accentSoft.copy(alpha = 0.92f))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height
                    if (width <= 0 || height <= 0) return@Canvas

                    val numDots = ((width * height) / 45f).toInt().coerceIn(60, 240)

                    for (i in 0 until numDots) {
                        val seedX = ((i * 0.1732f + 0.037f) % 1.0f)
                        val seedY = ((i * 0.2819f + 0.083f) % 1.0f)

                        val driftSpeedX = 0.22f + (i % 7) * 0.08f
                        val driftSpeedY = -0.18f - (i % 5) * 0.07f

                        val curXNorm = ((seedX + animTime * driftSpeedX + sin(animTime * 1.6f + i) * 0.035f) % 1.0f + 1.0f) % 1.0f
                        val curYNorm = ((seedY + animTime * driftSpeedY + sin(animTime * 1.9f + i * 2) * 0.035f) % 1.0f + 1.0f) % 1.0f

                        val px = curXNorm * width
                        val py = curYNorm * height

                        val twinkle = sin(animTime * 9f + i * 19.3f)
                        val dotAlpha = ((twinkle + 1f) * 0.5f).coerceIn(0.20f, 1.0f)
                        val radius = (0.75f + (i % 4) * 0.42f).dp.toPx()

                        val color = when (i % 4) {
                            0 -> scheme.textPrimary.copy(alpha = dotAlpha)
                            1 -> scheme.accent.copy(alpha = dotAlpha)
                            2 -> scheme.textSecondary.copy(alpha = dotAlpha * 0.9f)
                            else -> scheme.accent.copy(alpha = dotAlpha * 0.75f)
                        }

                        drawCircle(
                            color = color,
                            radius = radius,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}
