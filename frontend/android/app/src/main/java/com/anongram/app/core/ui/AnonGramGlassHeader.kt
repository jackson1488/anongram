package com.anongram.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ThemeTintMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Единый бесшовный прогрессивный блюр для плавающих шапок AnonGram.
 *
 * В светлой теме использует молочный тон `scheme.surface`.
 * В тёмной теме (включая кастомные и Ч/Б темы) осветляет тон матового стекла
 * от `scheme.card` + лёгкий акцент темы, чтобы блюр не превращался в глухую чёрную полосу.
 */
@Composable
fun Modifier.anonGramProgressiveHeaderBlur(
    hazeState: HazeState,
    glassBlurEnabled: Boolean,
    scrollFraction: Float
): Modifier {
    val scheme = LocalAnonGramScheme.current
    val fraction = scrollFraction.coerceIn(0f, 1f)
    val isMono = scheme.spec.tintMode == ThemeTintMode.MONOCHROME

    val glassTint = if (scheme.isDark) {
        val base = lerp(scheme.card, scheme.accent, if (isMono) 0f else 0.10f)
        val lifted = lerp(base, Color.White, 0.08f)
        lifted.copy(alpha = 0.16f + 0.10f * fraction)
    } else {
        scheme.surface.copy(alpha = 0.38f + 0.14f * fraction)
    }

    val blurModifier = if (glassBlurEnabled) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Color.Transparent,
                tint = HazeTint(color = glassTint),
                blurRadius = 22.dp,
                noiseFactor = 0.02f
            )
        ) {
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f
            )
        }
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    scheme.surface.copy(alpha = 0.90f + 0.08f * fraction),
                    scheme.background.copy(alpha = 0.60f + 0.25f * fraction),
                    Color.Transparent
                )
            )
        )
    }
    return this.then(blurModifier)
}
