package com.anongram.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Фоновый слой бесшовного матового стекла для плавающих шапок AnonGram.
 *
 * Использует стандартный `hazeEffect` (без `HazeProgressive`, который на тёмных темах
 * давал чёрную полосу сверху) и плавно растворяет нижний край через `BlendMode.DstIn`.
 */
@Composable
fun AnonGramGlassHeaderBackdrop(
    hazeState: HazeState,
    glassBlurEnabled: Boolean,
    scrollFraction: Float,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val fraction = scrollFraction.coerceIn(0f, 1f)

    val tintColor = if (scheme.isDark) {
        scheme.surface.copy(alpha = 0.28f + 0.16f * fraction)
    } else {
        scheme.surface.copy(alpha = 0.40f + 0.16f * fraction)
    }

    val backdropModifier = if (glassBlurEnabled) {
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = scheme.background,
                    tint = HazeTint(color = tintColor),
                    blurRadius = 18.dp,
                    noiseFactor = 0.02f
                )
            )
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Black,
                        0.68f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    } else {
        modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    scheme.surface.copy(alpha = 0.90f + 0.08f * fraction),
                    scheme.background.copy(alpha = 0.60f + 0.25f * fraction),
                    Color.Transparent
                )
            )
        )
    }

    Box(modifier = backdropModifier)
}

/**
 * Контейнер шапки с бесшовным матовым фоном [AnonGramGlassHeaderBackdrop] позади контента.
 */
@Composable
fun AnonGramGlassHeaderColumn(
    hazeState: HazeState,
    glassBlurEnabled: Boolean,
    scrollFraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        AnonGramGlassHeaderBackdrop(
            hazeState = hazeState,
            glassBlurEnabled = glassBlurEnabled,
            scrollFraction = scrollFraction,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
