package com.anongram.app.core.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.transitions.TransitionStyleRegistry
import kotlin.math.abs

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
    if (isExit && !config.enableExitAnimation) return this
    if (!isExit && (!config.transitionType.isBidirectional || !config.enableEnterAnimation)) {
        if (!config.enableBaseSmoothEnter) return this
        return graphicsLayer {
            translationX = progress * size.width
        }
    }
    return graphicsLayer {
        with(TransitionStyleRegistry.resolve(config.transitionType)) {
            transform(progress, isExit, config, density)
        }
        val hasCardCombo = config.enableMiniCardCombo || config.enablePredictiveBackCombo
        if (
            hasCardCombo &&
            isExit &&
            config.transitionType != TransitionType.SPRING_SWIPE &&
            config.transitionType != TransitionType.CARD_STACK_PEEK &&
            config.transitionType != TransitionType.PREDICTIVE_BACK &&
            config.transitionType != TransitionType.PARTICLE_DISSOLVE
        ) {
            val cardScale = (1f - progress * 0.14f).coerceIn(0.82f, 1f)
            scaleX *= cardScale
            scaleY *= cardScale
            shape = androidx.compose.foundation.shape.RoundedCornerShape((progress * 28f).dp)
            clip = progress > 0.01f
            shadowElevation = progress * 16.dp.toPx()
        }
    }
}

/**
 * Жест «назад» 1:1 свайпом слева направо из любой точки экрана.
 * Не перехватывает вертикальный скролл и свайпы справа налево (например, ответ на сообщение).
 */
fun Modifier.swipeBackGesture(
    enabled: Boolean,
    onDragStart: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onDragEnd: (shouldClose: Boolean) -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(enabled) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalDx = 0f
            var totalDy = 0f
            var tracking = false
            var rejected = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (tracking) {
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        onDragEnd((totalDx / width) > 0.25f)
                    }
                    break
                }

                val delta = change.positionChange()
                totalDx += delta.x
                totalDy += delta.y

                if (!tracking && !rejected) {
                    if (change.isConsumed) {
                        rejected = true
                    } else if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx)) {
                        rejected = true
                    } else if (totalDx < -touchSlop) {
                        rejected = true
                    } else if (totalDx > touchSlop * 0.8f && totalDx > abs(totalDy) * 1.15f) {
                        tracking = true
                        onDragStart()
                    }
                }

                if (tracking) {
                    change.consume()
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    onDragProgress((totalDx / width).coerceIn(0f, 1f))
                }
            }
        }
    }
}

/**
 * Контейнер перехода: оборачивает экран, применяя активную стратегию,
 * а для PARTICLE_DISSOLVE захватывает реальный снимок экрана для пазлов/частиц в обе стороны.
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
    val isParticleActiveForDir = config.transitionType == TransitionType.PARTICLE_DISSOLVE &&
        ((isExit && config.enableExitAnimation) || (!isExit && config.enableEnterAnimation))
    val isTransitionActive = progress > 0.005f

    if (!isParticleActiveForDir) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .applyAnonGramTransition(progress, isExit, config, density)
        ) {
            content()
        }
        return
    }

    val captureLayer = rememberGraphicsLayer()
    var hasRecordedFrame by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(isTransitionActive, hasRecordedFrame, config.particleStyle) {
        if (!isTransitionActive) {
            capturedBitmap = null
            hasRecordedFrame = false
        } else if (hasRecordedFrame && capturedBitmap == null) {
            withFrameNanos { }
            if (captureLayer.size.width > 0 && captureLayer.size.height > 0) {
                capturedBitmap = runCatching { captureLayer.toImageBitmap() }.getOrNull()
            }
        }
    }

    val showCapturedParticles = isTransitionActive && capturedBitmap != null

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (isTransitionActive && capturedBitmap == null) {
                        captureLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        if (isExit) {
                            drawLayer(captureLayer)
                        }
                        if (!hasRecordedFrame && size.width > 1f && size.height > 1f) {
                            hasRecordedFrame = true
                        }
                    } else if (!showCapturedParticles) {
                        drawContent()
                    }
                }
        ) {
            content()
        }

        if (showCapturedParticles) {
            ParticleOverlay(
                progress = progress,
                isExit = isExit,
                style = config.particleStyle,
                screenBitmap = capturedBitmap,
                denseParticles = config.denseParticles,
                slideCombo = config.particleSlideCombo
            )
        }
    }
}
