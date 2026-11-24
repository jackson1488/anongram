package com.anongram.app.core.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode

/**
 * Единая кисть для исходящих пузырей:
 * - обычная тема -> двухцветный градиент `outgoingBubbleStart..End`;
 * - статичная 7-цветная радуга -> плавный 7-цветный градиент;
 * - живая 7-цветная радуга -> непрерывное переливание в реальном времени.
 */
@Composable
fun rememberOutgoingBubbleBrush(scheme: AnonGramScheme): Brush {
    if (!scheme.isRainbow) {
        return Brush.linearGradient(listOf(scheme.outgoingBubbleStart, scheme.outgoingBubbleEnd))
    }
    if (!scheme.isRainbowLive) {
        return Brush.linearGradient(colors = AnonGramThemes.rainbowColors)
    }

    val transition = rememberInfiniteTransition(label = "liveRainbowBubble")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowPhase"
    )
    val span = 960f
    val shift = phase * span
    return Brush.linearGradient(
        colors = AnonGramThemes.rainbowColors + AnonGramThemes.rainbowColors.first(),
        start = Offset(-shift, 0f),
        end = Offset(span - shift, 280f),
        tileMode = TileMode.Repeated
    )
}
