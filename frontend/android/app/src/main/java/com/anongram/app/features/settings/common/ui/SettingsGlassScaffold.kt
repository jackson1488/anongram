package com.anongram.app.features.settings.common.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Применяет эффект матового стекла (Liquid Glass) к парящим кнопкам и островкам в шапке.
 * Автоматически адаптируется к Ч/Б, Б/Ч, цветным палитрам и настройке `glassBlurEnabled`.
 */
@Composable
fun Modifier.settingsGlassPill(
    hazeState: HazeState,
    shape: Shape = CircleShape,
    glassBlurEnabled: Boolean = AnimationsSettingsStore.state.collectAsState().value.glassBlurEnabled
): Modifier {
    val scheme = LocalAnonGramScheme.current
    val pillBorderBrush = com.anongram.app.core.theme.rememberRainbowBorderBrush(scheme)
    val pillModifier = if (glassBlurEnabled) {
        Modifier
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = scheme.islandBg,
                    tint = HazeTint(
                        color = scheme.islandBg.copy(alpha = if (scheme.isDark) 0.45f else 0.55f)
                    ),
                    blurRadius = 16.dp,
                    noiseFactor = 0.02f
                )
            )
            .border(0.8.dp, pillBorderBrush, shape)
    } else {
        Modifier
            .clip(shape)
            .background(scheme.islandBg.copy(alpha = 0.90f))
            .border(0.8.dp, pillBorderBrush, shape)
    }
    return this.then(pillModifier)
}

/**
 * Круглая стеклянная кнопка «Назад» для парящей шапки подразделов настроек.
 */
@Composable
fun SettingsGlassBackButton(
    hazeState: HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Назад",
    size: Dp = 40.dp
) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = modifier
            .size(size)
            .settingsGlassPill(hazeState = hazeState, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = scheme.textPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Универсальный каркас экрана настроек с бесшовным прогрессивным блюром шапки (Telegram Liquid Glass):
 * - Контент прокручивается под шапку на весь экран;
 * - Шапка плавно растворяет блюр и тонировку к нижнему краю (`HazeProgressive.verticalGradient`), без видимых границ;
 * - Кнопка «Назад» и опциональные правые кнопки парят в прозрачном стеклянном баре.
 */
@Composable
fun SettingsGlassScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String = "Назад",
    scrollState: ScrollState = rememberScrollState(),
    contentHorizontalPadding: Dp = 14.dp,
    contentBottomPadding: Dp = 32.dp,
    trailingAction: (@Composable (HazeState) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val animState by AnimationsSettingsStore.state.collectAsState()
    val hazeState = remember { HazeState() }

    val headerScrollFraction by remember {
        derivedStateOf { (scrollState.value / 160f).coerceIn(0f, 1f) }
    }
    val animatedHeaderAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        label = "settingsSubHeaderAlpha"
    )

    val glassTintAlpha = if (scheme.isDark) {
        0.30f + 0.14f * animatedHeaderAlpha
    } else {
        0.40f + 0.16f * animatedHeaderAlpha
    }

    val headerBackgroundModifier = if (animState.glassBlurEnabled) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = scheme.background,
                tint = HazeTint(color = scheme.surface.copy(alpha = glassTintAlpha)),
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
                    scheme.surface.copy(alpha = 0.90f + 0.08f * animatedHeaderAlpha),
                    scheme.background.copy(alpha = 0.60f + 0.25f * animatedHeaderAlpha),
                    Color.Transparent
                )
            )
        )
    }

    val scaffoldBgBrush = com.anongram.app.core.theme.rememberRainbowBackgroundBrush(scheme)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scaffoldBgBrush)
    ) {
        // 1. Прокручиваемый контент на весь экран (проходит под парящей шапкой)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(scaffoldBgBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .safeStatusBarPadding()
                    .safeNavigationBarPadding()
                    .padding(top = 60.dp, bottom = contentBottomPadding)
                    .padding(horizontal = contentHorizontalPadding),
                content = content
            )
        }

        // 2. Парящая прозрачная шапка с бесшовным прогрессивным блюром и стеклянными кнопками
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .then(headerBackgroundModifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .height(48.dp)
            ) {
                SettingsGlassBackButton(
                    hazeState = hazeState,
                    onClick = onBack,
                    contentDescription = backContentDescription,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp)
                )

                if (trailingAction != null) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        trailingAction(hazeState)
                    }
                }
            }

            // Плавный хвост прогрессивного блюра — растворяется в ноль без шва
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
