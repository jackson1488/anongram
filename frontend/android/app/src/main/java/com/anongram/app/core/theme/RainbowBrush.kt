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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import kotlinx.coroutines.delay

private val RainbowDarkBgStops = listOf(
    Color(0xFF541034), // Красный
    Color(0xFF5A280B), // Оранжевый
    Color(0xFF473808), // Жёлтый
    Color(0xFF0A4732), // Зелёный
    Color(0xFF093E56), // Голубой
    Color(0xFF172F61), // Синий
    Color(0xFF3A1A69)  // Фиолетовый
)

private val RainbowLightBgStops = listOf(
    Color(0xFFFFA8C0), // Красный
    Color(0xFFFFC294), // Оранжевый
    Color(0xFFFFDF85), // Жёлтый
    Color(0xFF96F0C4), // Зелёный
    Color(0xFF94E2FF), // Голубой
    Color(0xFFA8C4FF), // Синий
    Color(0xFFD0A8FF)  // Фиолетовый
)

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
        label = "rainbowBubblePhase"
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
 * 7-цветный комбо-фон для экранов (все 7 цветов видны одновременно).
 * В живом режиме весь 7-цветный спектр плавно течёт по экрану в реальном времени.
 */
@Composable
fun rememberRainbowBackgroundBrush(scheme: AnonGramScheme): Brush {
    if (!scheme.isRainbow) {
        return Brush.linearGradient(listOf(scheme.background, scheme.background))
    }
    val stops = if (scheme.isDark) RainbowDarkBgStops else RainbowLightBgStops
    if (!scheme.isRainbowLive) {
        return Brush.linearGradient(
            colors = stops,
            start = Offset(0f, 0f),
            end = Offset(1100f, 1900f)
        )
    }

    val transition = rememberInfiniteTransition(label = "liveRainbowBg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowBgPhase"
    )
    val span = 1800f
    val shift = phase * span
    return Brush.linearGradient(
        colors = stops + stops.first(),
        start = Offset(-shift * 0.55f, -shift),
        end = Offset(span * 0.55f - shift * 0.55f, span - shift),
        tileMode = TileMode.Repeated
    )
}

/**
 * 7-цветная радужная рамка для карточек, островков и кнопок.
 */
@Composable
fun rememberRainbowBorderBrush(scheme: AnonGramScheme): Brush {
    if (!scheme.isRainbow) {
        return Brush.linearGradient(listOf(scheme.cardBorder, scheme.cardBorder))
    }
    val alpha = if (scheme.isDark) 0.58f else 0.48f
    val borderColors = remember(alpha) { AnonGramThemes.rainbowColors.map { it.copy(alpha = alpha) } }
    if (!scheme.isRainbowLive) {
        return Brush.linearGradient(borderColors)
    }

    val transition = rememberInfiniteTransition(label = "liveRainbowBorder")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowBorderPhase"
    )
    val span = 1000f
    val shift = phase * span
    return Brush.linearGradient(
        colors = borderColors + borderColors.first(),
        start = Offset(-shift, 0f),
        end = Offset(span - shift, 360f),
        tileMode = TileMode.Repeated
    )
}

/**
 * Разноцветное комбо токенов (accent, accentDim, selectedGlow и др. одновременно берут разные
 * цвета из 7-цветного спектра), а в живом режиме их комбинация плавно переливается.
 */
@Composable
fun rememberLiveRainbowScheme(baseScheme: AnonGramScheme): AnonGramScheme {
    if (!baseScheme.isRainbow || !baseScheme.isRainbowLive) return baseScheme

    var step by remember(baseScheme.id) { mutableIntStateOf(0) }
    LaunchedEffect(baseScheme.id) {
        while (true) {
            delay(900L)
            step = (step + 1) % AnonGramThemes.rainbowColors.size
        }
    }

    return remember(baseScheme.id, step) {
        val palette = AnonGramThemes.rainbowColors
        val primaryAccent = palette[(step + 4) % palette.size]
        val secondaryAccent = palette[(step + 6) % palette.size]
        val glowAccent = palette[(step + 1) % palette.size]
        baseScheme.copy(
            accent = primaryAccent,
            accentDim = secondaryAccent,
            accentSoft = primaryAccent.copy(alpha = if (baseScheme.isDark) 0.22f else 0.16f),
            selectedGlow = glowAccent,
            outgoingBubbleBorder = glowAccent.copy(alpha = 0.65f)
        )
    }
}
