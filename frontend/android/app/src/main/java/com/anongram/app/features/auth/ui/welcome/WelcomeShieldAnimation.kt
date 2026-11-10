package com.anongram.app.features.auth.ui.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Анимированный неоновый щит AnonGram.
 *
 * Отрисовывает на Canvas фирменный герб протокола с пульсирующим фиолетовым свечением,
 * внутренними геометрическими узлами и неоновым градиентом #8B5CF6 -> #C084FC.
 *
 * @param size Размер компонента в dp.
 * @param modifier Модификатор макета.
 */
@Composable
fun WelcomeShieldAnimation(
    size: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val neonPurple = Color(0xFF8B5CF6)
    val neonLilac = Color(0xFFC084FC)
    val darkPurple = Color(0xFF3B1866)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * pulseScale)) {
            val width = size.toPx()
            val height = size.toPx()

            // Внешний контур щита
            val shieldPath = Path().apply {
                moveTo(width * 0.5f, height * 0.08f)
                cubicTo(
                    width * 0.78f, height * 0.08f,
                    width * 0.90f, height * 0.22f,
                    width * 0.90f, height * 0.42f
                )
                cubicTo(
                    width * 0.90f, height * 0.72f,
                    width * 0.58f, height * 0.88f,
                    width * 0.5f, height * 0.96f
                )
                cubicTo(
                    width * 0.42f, height * 0.88f,
                    width * 0.10f, height * 0.72f,
                    width * 0.10f, height * 0.42f
                )
                cubicTo(
                    width * 0.10f, height * 0.22f,
                    width * 0.22f, height * 0.08f,
                    width * 0.5f, height * 0.08f
                )
                close()
            }

            // Фоновое неоновое свечение (Aura Glow)
            drawPath(
                path = shieldPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        neonPurple.copy(alpha = glowAlpha * 0.45f),
                        darkPurple.copy(alpha = glowAlpha * 0.20f),
                        Color.Transparent
                    ),
                    radius = width * 0.65f
                )
            )

            // Основная заливка щита
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1033),
                        Color(0xFF0F0A1C)
                    )
                )
            )

            // Внешний контур с неоновым градиентом
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(neonLilac, neonPurple, neonLilac)
                ),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Внутренняя эмблема: буква "A" с защитным острием
            val innerPath = Path().apply {
                moveTo(width * 0.5f, height * 0.26f)
                lineTo(width * 0.68f, height * 0.66f)
                lineTo(width * 0.59f, height * 0.66f)
                lineTo(width * 0.54f, height * 0.53f)
                lineTo(width * 0.46f, height * 0.53f)
                lineTo(width * 0.41f, height * 0.66f)
                lineTo(width * 0.32f, height * 0.66f)
                close()

                moveTo(width * 0.5f, height * 0.38f)
                lineTo(width * 0.475f, height * 0.47f)
                lineTo(width * 0.525f, height * 0.47f)
                close()
            }

            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, neonLilac)
                )
            )
        }
    }
}
