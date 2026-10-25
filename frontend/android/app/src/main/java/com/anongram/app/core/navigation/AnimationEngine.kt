package com.anongram.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.anongram.app.core.navigation.transitions.TransitionStyleRegistry

/**
 * Фасад движка переходов. Не содержит логики анимаций: делегирует трансформации
 * стратегиям ITransitionStyle через TransitionStyleRegistry (один класс на стиль).
 */
fun Modifier.applyAnonGramTransition(
    progress: Float,
    isExit: Boolean,
    config: NavigationTransitionConfig,
    density: Float
): Modifier {
    if (config.transitionType == TransitionType.INSTANT) return this
    return graphicsLayer {
        with(TransitionStyleRegistry.resolve(config.transitionType)) {
            transform(progress, isExit, config, density)
        }
    }
}

/**
 * Контейнер перехода: оборачивает экран, применяя активную стратегию,
 * а для PARTICLE_DISSOLVE добавляет оверлей частиц поверх контента.
 */
@Composable
fun AnonGramTransitionScreen(
    progress: Float,
    isExit: Boolean,
    config: NavigationTransitionConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .fillMaxSize()
            .applyAnonGramTransition(progress, isExit, config, density)
    ) {
        content()

        if (config.transitionType == TransitionType.PARTICLE_DISSOLVE) {
            ParticleOverlay(
                progress = progress,
                isExit = isExit,
                style = config.particleStyle
            )
        }
    }
}
