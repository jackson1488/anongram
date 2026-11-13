package com.anongram.web.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.web.core.theme.LocalAnonGramScheme

@Composable
fun AnonGramScreenSurface(
    modifier: Modifier = Modifier,
    applySystemBarsPadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val baseModifier = modifier
        .fillMaxSize()
        .background(scheme.background)
        .let { m ->
            if (applySystemBarsPadding) {
                m.safeStatusBarPadding().safeNavigationBarPadding()
            } else {
                m
            }
        }
    CompositionLocalProvider(LocalContentColor provides scheme.textPrimary) {
        Box(modifier = baseModifier, content = content)
    }
}

@Composable
fun AnonGramIslandCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Surface(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, scheme.cardBorder), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = shape,
        color = scheme.card,
        contentColor = scheme.textPrimary
    ) {
        Column(content = content)
    }
}
