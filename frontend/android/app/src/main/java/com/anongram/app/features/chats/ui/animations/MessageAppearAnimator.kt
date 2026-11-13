package com.anongram.app.features.chats.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.anongram.app.features.settings.animations.data.MessageAppearStyle
import com.anongram.app.features.settings.animations.data.TokenStreamSpeed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object AppMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float = 1f
}

private val TOKEN_REGEX = Regex("\\S+\\s*|\\s+")

/**
 * Разбивает текст на аккуратные токены для эффекта AI-стриминга.
 */
fun splitIntoStreamTokens(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val rawTokens = TOKEN_REGEX.findAll(text).map { it.value }.toList()
    if (rawTokens.isEmpty()) return listOf(text)
    val result = ArrayList<String>(rawTokens.size)
    for (token in rawTokens) {
        if (token.length <= 10) {
            result.add(token)
        } else {
            // Длинные строки/ссылки/CJK делим на короткие чанки по 4 символа
            var i = 0
            while (i < token.length) {
                result.add(token.substring(i, minOf(i + 4, token.length)))
                i += 4
            }
        }
    }
    return result
}

/**
 * Возвращает постепенно раскрывающийся текст токен за токеном, если [shouldStream] == true.
 */
@Composable
fun rememberStreamedMessageText(
    fullText: String,
    animateKey: Any,
    shouldStream: Boolean,
    tokenSpeed: TokenStreamSpeed,
    speedMultiplier: Float,
    onStreamFinished: () -> Unit = {}
): String {
    if (!shouldStream || fullText.isEmpty()) {
        return fullText
    }

    var visibleText by remember(animateKey) { mutableStateOf("") }

    LaunchedEffect(animateKey, fullText, shouldStream, tokenSpeed, speedMultiplier) {
        if (!shouldStream) {
            visibleText = fullText
            return@LaunchedEffect
        }
        val tokens = splitIntoStreamTokens(fullText)
        val stepDelay = (tokenSpeed.tokenDelayMs * speedMultiplier).toLong().coerceIn(5L, 220L)
        val sb = StringBuilder(fullText.length)
        visibleText = ""
        for (idx in tokens.indices) {
            sb.append(tokens[idx])
            visibleText = sb.toString()
            if (idx < tokens.lastIndex) {
                delay(stepDelay)
            }
        }
        onStreamFinished()
    }

    return visibleText
}

/**
 * Модульная обёртка анимации появления бабла (используется и в чате, и в предпросмотре настроек).
 */
@Composable
fun MessageAppearWrapper(
    animateKey: Any,
    isIncoming: Boolean,
    shouldAnimate: Boolean,
    style: MessageAppearStyle,
    speedMultiplier: Float,
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val startScale = when (style) {
        MessageAppearStyle.CORNER_POP -> 0.35f
        MessageAppearStyle.SPRING_FLY -> 0.85f
        MessageAppearStyle.SLIDE_FADE -> 0.93f
        MessageAppearStyle.TOKEN_STREAM -> 0.95f
    }
    val startOffsetY = with(density) {
        when (style) {
            MessageAppearStyle.SPRING_FLY -> 56.dp.toPx()
            MessageAppearStyle.SLIDE_FADE -> 44.dp.toPx()
            MessageAppearStyle.CORNER_POP -> 22.dp.toPx()
            MessageAppearStyle.TOKEN_STREAM -> 16.dp.toPx()
        }
    }

    val alpha = remember(animateKey) { Animatable(if (shouldAnimate) 0f else 1f) }
    val scale = remember(animateKey) { Animatable(if (shouldAnimate) startScale else 1f) }
    val offsetY = remember(animateKey) { Animatable(if (shouldAnimate) startOffsetY else 0f) }

    LaunchedEffect(animateKey, shouldAnimate, style, speedMultiplier) {
        if (!shouldAnimate) {
            alpha.snapTo(1f)
            scale.snapTo(1f)
            offsetY.snapTo(0f)
            return@LaunchedEffect
        }

        alpha.snapTo(0f)
        scale.snapTo(startScale)
        offsetY.snapTo(startOffsetY)

        val mult = speedMultiplier.coerceIn(0.4f, 2.0f)
        withContext(AppMotionDurationScale) {
            coroutineScope {
                when (style) {
                    MessageAppearStyle.SPRING_FLY -> {
                        launch { alpha.animateTo(1f, tween((220 * mult).toInt().coerceAtLeast(70))) }
                        launch {
                            scale.animateTo(
                                1f,
                                spring(dampingRatio = 0.58f, stiffness = 260f / mult)
                            )
                        }
                        launch {
                            offsetY.animateTo(
                                0f,
                                spring(dampingRatio = 0.62f, stiffness = 250f / mult)
                            )
                        }
                    }
                    MessageAppearStyle.SLIDE_FADE -> {
                        val dur = (320 * mult).toInt().coerceAtLeast(90)
                        launch { alpha.animateTo(1f, tween(dur, easing = FastOutSlowInEasing)) }
                        launch { scale.animateTo(1f, tween(dur, easing = FastOutSlowInEasing)) }
                        launch { offsetY.animateTo(0f, tween(dur, easing = FastOutSlowInEasing)) }
                    }
                    MessageAppearStyle.CORNER_POP -> {
                        launch { alpha.animateTo(1f, tween((160 * mult).toInt().coerceAtLeast(60))) }
                        launch {
                            scale.animateTo(
                                1f,
                                spring(dampingRatio = 0.50f, stiffness = 340f / mult)
                            )
                        }
                        launch {
                            offsetY.animateTo(
                                0f,
                                spring(dampingRatio = 0.62f, stiffness = 320f / mult)
                            )
                        }
                    }
                    MessageAppearStyle.TOKEN_STREAM -> {
                        val dur = (160 * mult).toInt().coerceAtLeast(60)
                        launch { alpha.animateTo(1f, tween(dur, easing = FastOutSlowInEasing)) }
                        launch { scale.animateTo(1f, tween(dur, easing = FastOutSlowInEasing)) }
                        launch { offsetY.animateTo(0f, tween(dur, easing = FastOutSlowInEasing)) }
                    }
                }
            }
        }
        onAnimationFinished()
    }

    val origin = when (style) {
        MessageAppearStyle.CORNER_POP -> TransformOrigin(if (isIncoming) 0f else 1f, 1f)
        else -> TransformOrigin(if (isIncoming) 0.15f else 0.85f, 0.9f)
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.scaleX = scale.value
            this.scaleY = scale.value
            this.translationY = offsetY.value
            this.transformOrigin = origin
        }
    ) {
        content()
    }
}
