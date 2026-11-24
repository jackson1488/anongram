package com.anongram.app.core.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import kotlinx.coroutines.delay

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

/**
 * Живое переливание цветов для всего интерфейса ОС (фоны, карточки, островки, акценты, рамки).
 * При активной живой радуге плавно перебирает 360° спектра в реальном времени (~18 кадров/с).
 */
@Composable
fun rememberLiveRainbowScheme(baseScheme: AnonGramScheme): AnonGramScheme {
    if (!baseScheme.isRainbow || !baseScheme.isRainbowLive) return baseScheme

    var hue by remember(baseScheme.id) { mutableFloatStateOf(baseScheme.spec.hue) }
    LaunchedEffect(baseScheme.id) {
        while (true) {
            delay(55L)
            hue = (hue + 2.5f) % 360f
        }
    }

    return remember(baseScheme.id, hue) {
        val dynamic = ThemePaletteEngine.buildScheme(
            id = baseScheme.id,
            displayName = baseScheme.displayName,
            spec = baseScheme.spec.copy(
                tintMode = ThemeTintMode.FULL_OS,
                bubbleStyle = BubbleColorStyle.GRADIENT,
                hue = hue,
                saturation = if (baseScheme.isDark) 0.88f else 0.82f
            )
        )
        val idx = ((hue / 51.4f).toInt()).coerceIn(0, AnonGramThemes.rainbowColors.lastIndex)
        dynamic.copy(
            isRainbow = true,
            isRainbowLive = true,
            outgoingBubbleStart = AnonGramThemes.rainbowColors[idx],
            outgoingBubbleEnd = AnonGramThemes.rainbowColors[(idx + 3) % AnonGramThemes.rainbowColors.size],
            outgoingBubbleBorder = if (baseScheme.isDark) Color(0x55FFFFFF) else Color(0x33000000),
            outgoingText = Color.White,
            selectedGlow = Color(0xFF38BDF8)
        )
    }
}
