package com.anongram.app.features.chats.attach.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Фоновый блюр/скрим сзади плавающей шапки вложений:
 * Мягкий градиент от scheme.surface к прозрачности,
 * чтобы контент элегантно растворялся под шапкой во всех темах.
 */
@Composable
fun AttachHeaderScrim(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    includeStatusBar: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    val statusBarTop = if (includeStatusBar) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else 0.dp
    val scrimHeight = statusBarTop + 68.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(scrimHeight)
            .background(
                Brush.verticalGradient(
                    0.0f to scheme.background.copy(alpha = 0.58f * alpha),
                    0.65f to scheme.background.copy(alpha = 0.25f * alpha),
                    1.0f to Color.Transparent
                )
            )
    )
}
