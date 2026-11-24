package com.anongram.app.core.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Триггер эффекта «капли» (Circular Reveal как в Telegram) при смене темы.
 * Передаётся вниз через [LocalThemeRevealTrigger].
 */
@Immutable
class ThemeRevealTrigger(
    private val onLaunchReveal: (origin: Offset, applyTheme: () -> Unit) -> Unit
) {
    fun launch(origin: Offset, applyTheme: () -> Unit) {
        onLaunchReveal(origin, applyTheme)
    }
}

val LocalThemeRevealTrigger = compositionLocalOf<ThemeRevealTrigger?> { null }

@Composable
fun rememberThemeRevealLauncher(): (origin: Offset, applyTheme: () -> Unit) -> Unit {
    val trigger = LocalThemeRevealTrigger.current
    return remember(trigger) {
        { origin, applyTheme ->
            if (trigger != null) trigger.launch(origin, applyTheme)
            else applyTheme()
        }
    }
}

/**
 * Корневой хост анимации «капли» при переключении тем:
 * 1. Снимает пиксельный снимок старой темы через [rememberGraphicsLayer];
 * 2. Мгновенно переключает тему под снимком;
 * 3. Вырезает расширяющийся круг из точки нажатия [origin] до дальнего угла экрана за 440 мс.
 */
@Composable
fun ThemeRevealHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var oldSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var revealOrigin by remember { mutableStateOf(Offset.Zero) }
    val revealProgress = remember { Animatable(0f) }

    val trigger = remember(scope, graphicsLayer) {
        ThemeRevealTrigger { origin, applyTheme ->
            scope.launch {
                val captured = runCatching { graphicsLayer.toImageBitmap() }.getOrNull()
                if (captured == null) {
                    applyTheme()
                    return@launch
                }
                oldSnapshot = captured
                revealOrigin = origin
                revealProgress.snapTo(0f)
                applyTheme()
                revealProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                )
                oldSnapshot = null
            }
        }
    }

    CompositionLocalProvider(LocalThemeRevealTrigger provides trigger) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
            ) {
                content()
            }

            val snapshot = oldSnapshot
            if (snapshot != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val origin = if (revealOrigin == Offset.Zero) Offset(w / 2f, h / 2f) else revealOrigin
                    val maxRadius = maxOf(
                        hypot(origin.x, origin.y),
                        hypot(w - origin.x, origin.y),
                        hypot(origin.x, h - origin.y),
                        hypot(w - origin.x, h - origin.y)
                    ).coerceAtLeast(1f)
                    val progress = revealProgress.value.coerceIn(0f, 1f)
                    val currentRadius = maxRadius * progress

                    val outerPath = Path().apply {
                        addRect(Rect(0f, 0f, w, h))
                    }
                    val holePath = Path().apply {
                        addOval(
                            Rect(
                                left = origin.x - currentRadius,
                                top = origin.y - currentRadius,
                                right = origin.x + currentRadius,
                                bottom = origin.y + currentRadius
                            )
                        )
                    }
                    val maskedOldPath = Path.combine(PathOperation.Difference, outerPath, holePath)
                    clipPath(maskedOldPath) {
                        drawImage(image = snapshot)
                    }

                    if (progress in 0.01f..0.98f) {
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - progress) * 0.28f),
                            radius = currentRadius,
                            center = origin,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
