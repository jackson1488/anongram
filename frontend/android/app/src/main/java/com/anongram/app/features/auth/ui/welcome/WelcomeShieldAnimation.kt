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
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Анимированный щит AnonGram с оригами-самолётиком.
 *
 * Отрисовывает на Canvas фирменный герб протокола в цветах активной темы:
 * - Пульсирующий внешний щит с мягким ореолом (`scheme.accent` / `scheme.accentDim`);
 * - Самолётик оригами (символ анонимной пересылки сообщений);
 * - Плавная циклическая пульсация свечения и масштаба.
 *
 * @param size Размер компонента в dp.
 * @param modifier Модификатор макета.
 */
@Composable
fun WelcomeShieldAnimation(
    size: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val accentPrimary = scheme.accent
    val accentSecondary = scheme.accentDim
    val shieldTopBg = scheme.card
    val shieldBottomBg = scheme.surface
    val planeHighlight = scheme.textPrimary

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
                    width * 0.82f, height * 0.08f,
                    width * 0.88f, height * 0.22f,
                    width * 0.88f, height * 0.44f
                )
                cubicTo(
                    width * 0.88f, height * 0.70f,
                    width * 0.58f, height * 0.87f,
                    width * 0.5f, height * 0.95f
                )
                cubicTo(
                    width * 0.42f, height * 0.87f,
                    width * 0.12f, height * 0.70f,
                    width * 0.12f, height * 0.44f
                )
                cubicTo(
                    width * 0.12f, height * 0.22f,
                    width * 0.18f, height * 0.08f,
                    width * 0.5f, height * 0.08f
                )
                close()
            }

            // Фоновое неоновое свечение (Aura Glow)
            drawPath(
                path = shieldPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = glowAlpha * 0.45f),
                        accentSecondary.copy(alpha = glowAlpha * 0.22f),
                        Color.Transparent
                    ),
                    radius = width * 0.65f
                )
            )

            // Основная заливка щита из активной темы
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        shieldTopBg,
                        shieldBottomBg
                    )
                )
            )

            // Внутренний тонкий светящийся контур
            val innerShieldPath = Path().apply {
                moveTo(width * 0.5f, height * 0.13f)
                cubicTo(
                    width * 0.77f, height * 0.13f,
                    width * 0.82f, height * 0.24f,
                    width * 0.82f, height * 0.43f
                )
                cubicTo(
                    width * 0.82f, height * 0.66f,
                    width * 0.56f, height * 0.82f,
                    width * 0.5f, height * 0.89f
                )
                cubicTo(
                    width * 0.44f, height * 0.82f,
                    width * 0.18f, height * 0.66f,
                    width * 0.18f, height * 0.43f
                )
                cubicTo(
                    width * 0.18f, height * 0.24f,
                    width * 0.23f, height * 0.13f,
                    width * 0.5f, height * 0.13f
                )
                close()
            }
            drawPath(
                path = innerShieldPath,
                color = accentPrimary.copy(alpha = 0.35f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Внешний контур с акцентным градиентом
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(accentPrimary, accentSecondary, accentPrimary)
                ),
                style = Stroke(
                    width = 4.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // -----------------------------------------------------------------
            // Самолётик оригами (направлен вправо-вверх)
            // -----------------------------------------------------------------
            val noseX = width * 0.68f
            val noseY = height * 0.31f
            val leftWingX = width * 0.32f
            val leftWingY = height * 0.52f
            val rightWingX = width * 0.54f
            val rightWingY = height * 0.70f
            val notchX = width * 0.49f
            val notchY = height * 0.51f

            // Левое крыло (в полутени)
            val leftWingPath = Path().apply {
                moveTo(noseX, noseY)
                lineTo(leftWingX, leftWingY)
                lineTo(notchX, notchY)
                close()
            }
            drawPath(
                path = leftWingPath,
                color = accentSecondary
            )

            // Правое крыло (основное, контрастное)
            val rightWingPath = Path().apply {
                moveTo(noseX, noseY)
                lineTo(width * 0.71f, height * 0.34f)
                lineTo(rightWingX, rightWingY)
                lineTo(notchX, notchY)
                close()
            }
            drawPath(
                path = rightWingPath,
                brush = Brush.linearGradient(
                    colors = listOf(planeHighlight, accentPrimary)
                )
            )

            // Нижний клапан / складка
            val bottomFoldPath = Path().apply {
                moveTo(notchX, notchY)
                lineTo(rightWingX, rightWingY)
                lineTo(width * 0.44f, height * 0.56f)
                close()
            }
            drawPath(
                path = bottomFoldPath,
                color = accentSecondary.copy(alpha = 0.75f)
            )

            // Контур самолётика для чёткости
            val planeOutline = Path().apply {
                moveTo(leftWingX, leftWingY)
                lineTo(noseX, noseY)
                lineTo(rightWingX, rightWingY)
                lineTo(notchX, notchY)
                close()
            }
            drawPath(
                path = planeOutline,
                color = planeHighlight.copy(alpha = 0.85f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
