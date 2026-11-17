package com.anongram.app.features.chats.attach.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlin.math.sin
import kotlin.random.Random

private class MediaSpoilerParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radiusDp: Float,
    val alpha: Float
)

/**
 * Анимированный эффект мерцающего спойлера для фото/видео в стиле Telegram
 * с адаптацией под тему и поддержкой отключения движения частиц (экономия батареи).
 */
@Composable
fun SpoilerOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 160
) {
    val scheme = LocalAnonGramScheme.current
    val motionEnabled by ChatTextSettingsStore.spoilerParticleMotionEnabled.collectAsState()
    var boxSize by remember { mutableStateOf(Size.Zero) }
    var frameClockMs by remember { mutableLongStateOf(0L) }

    val particles = remember(boxSize, particleCount) {
        if (boxSize.width <= 0f || boxSize.height <= 0f) {
            emptyList()
        } else {
            val rand = Random(42)
            List(particleCount) {
                MediaSpoilerParticle(
                    x = rand.nextFloat() * boxSize.width,
                    y = rand.nextFloat() * boxSize.height,
                    vx = (rand.nextFloat() - 0.5f) * 90f,
                    vy = (rand.nextFloat() - 0.5f) * 90f,
                    radiusDp = 0.75f + rand.nextFloat() * 1.35f,
                    alpha = 0.32f + rand.nextFloat() * 0.65f
                )
            }
        }
    }

    LaunchedEffect(motionEnabled, particles.size) {
        if (!motionEnabled || particles.isEmpty()) return@LaunchedEffect
        var lastMs = 0L
        while (true) {
            withFrameMillis { nowMs ->
                val dt = if (lastMs == 0L) 0.016f else ((nowMs - lastMs) / 1000f).coerceIn(0.001f, 0.05f)
                lastMs = nowMs
                val w = boxSize.width
                val h = boxSize.height
                if (w > 0f && h > 0f) {
                    for (p in particles) {
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        if (p.x < 0f || p.x > w || p.y < 0f || p.y > h) {
                            p.x = Random.nextFloat() * w
                            p.y = Random.nextFloat() * h
                            p.vx = (Random.nextFloat() - 0.5f) * 90f
                            p.vy = (Random.nextFloat() - 0.5f) * 90f
                        }
                    }
                }
                frameClockMs = nowMs
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.overlayScrim.copy(alpha = 0.45f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (boxSize != size) {
                boxSize = size
            }
            val w = size.width
            val h = size.height
            val nowMs = frameClockMs

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

            particles.forEachIndexed { idx, p ->
                val twinkle = if (motionEnabled) {
                    0.65f + 0.35f * sin(nowMs * 0.005f + idx * 0.7f).toFloat()
                } else {
                    1f
                }
                drawCircle(
                    color = scheme.textPrimary.copy(alpha = (p.alpha * twinkle).coerceIn(0.18f, 0.96f)),
                    radius = p.radiusDp * density,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
