package com.anongram.app.features.attach.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class SpoilerParticle(
    val xNorm: Float,
    val yNorm: Float,
    val radius: Float,
    val phaseOffset: Float,
    val speed: Float
)

/**
 * Анимированный эффект мерцающего спойлера в стиле Telegram.
 */
@Composable
fun SpoilerOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 160
) {
    val particles = remember(particleCount) {
        val rand = Random(42)
        List(particleCount) {
            SpoilerParticle(
                xNorm = rand.nextFloat(),
                yNorm = rand.nextFloat(),
                radius = 0.8f + rand.nextFloat() * 2.2f,
                phaseOffset = rand.nextFloat() * 6.28f,
                speed = 0.6f + rand.nextFloat() * 1.6f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "spoilerTransition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spoilerProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x66000000))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Мягкое свечение для эффекта звёздного сияния как в Telegram
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color(0x33FFFFFF), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.40f),
                    radius = minOf(w, h) * 0.85f
                ),
                radius = minOf(w, h) * 0.85f,
                center = Offset(w * 0.50f, h * 0.40f)
            )

            particles.forEach { p ->
                val wave = kotlin.math.sin(progress * p.speed + p.phaseOffset).toFloat()
                val alpha = (0.25f + 0.75f * ((wave + 1f) / 2f)).coerceIn(0.12f, 1f)
                val x = (p.xNorm * w + wave * 3.5f).coerceIn(0f, w)
                val y = (p.yNorm * h + kotlin.math.cos(progress * p.speed + p.phaseOffset).toFloat() * 3.5f).coerceIn(0f, h)

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = p.radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
