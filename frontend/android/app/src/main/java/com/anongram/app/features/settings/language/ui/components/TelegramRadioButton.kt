package com.anongram.app.features.settings.language.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Кастомная радиокнопка в стиле Telegram.
 *
 * Неактивное состояние: приглушённый круглый контур.
 * Активное состояние: акцентный контур + плавно масштабируемая акцентная точка в центре.
 */
@Composable
fun TelegramRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    activeColor: Color = LocalAnonGramScheme.current.accent,
    inactiveColor: Color = LocalAnonGramScheme.current.textMuted
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "radio_dot_scale"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val radius = (this.size.minDimension - strokeWidth) / 2f
        val ringColor = if (selected) activeColor else inactiveColor

        // Внешнее кольцо
        drawCircle(
            color = ringColor,
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // Внутренняя залитая точка (масштабируется при выборе)
        if (scaleAnim > 0f) {
            val innerDotRadius = (radius - 3.5.dp.toPx()) * scaleAnim
            if (innerDotRadius > 0f) {
                drawCircle(
                    color = activeColor,
                    radius = innerDotRadius
                )
            }
        }
    }
}
