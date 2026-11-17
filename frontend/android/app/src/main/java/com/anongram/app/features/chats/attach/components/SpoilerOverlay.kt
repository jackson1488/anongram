package com.anongram.app.features.chats.attach.components

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlin.random.Random

private data class SpoilerParticle(
    val xNorm: Float,
    val yNorm: Float,
    val radius: Float,
    val phaseOffset: Float,
    val speed: Float
)

/**
 * Анимированный эффект мерцающего спойлера в стиле Telegram с адаптацией под тему
 * и поддержкой отключения движения частиц (экономия батареи).
 */
@Composable
fun SpoilerOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 160
) {
    val scheme = LocalAnonGramScheme.current
    val motionEnabled by ChatTextSettingsStore.spoilerParticleMotionEnabled.collectAsState()
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

    val progress = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "spoilerTransition")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spoilerProgress"
        )
        animated
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.overlayScrim.copy(alpha = 0.45f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        scheme.accent.copy(alpha = 0.20f),
                        scheme.textPrimary.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.40f),
                    radius = minOf(w, h) * 0.85f
                ),
                radius = minOf(w, h) * 0.85f,
                center = Offset(w * 0.50f, h * 0.40f)
            )

            particles.forEach { p ->
                val wave = kotlin.math.sin(progress * p.speed + p.phaseOffset).toFloat()
                val alpha = (0.25f + 0.75f * ((wave + 1f) / 2f)).coerceIn(0.15f, 0.95f)
                val driftX = if (motionEnabled) wave * 3.5f else 0f
                val driftY = if (motionEnabled) kotlin.math.cos(progress * p.speed + p.phaseOffset).toFloat() * 3.5f else 0f
                val x = (p.xNorm * w + driftX).coerceIn(0f, w)
                val y = (p.yNorm * h + driftY).coerceIn(0f, h)

                drawCircle(
                    color = scheme.textPrimary.copy(alpha = alpha),
                    radius = p.radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
