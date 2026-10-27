package com.anongram.app.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val MAX_TOP_INSET = 56.dp
private val MAX_BOTTOM_INSET = 28.dp

/**
 * Ограничители системных инсетов. Учитывает statusBars, displayCutout и captionBar
 * (для Waydroid и desktop/freeform режимов), чтобы топбар не перекрывался системными заголовками окон.
 */
@Composable
fun Modifier.safeStatusBarPadding(): Modifier {
    val density = LocalDensity.current
    val maxTopPx = with(density) { MAX_TOP_INSET.toPx() }.toInt()
    val topPx = min(
        WindowInsets.statusBars
            .union(WindowInsets.displayCutout)
            .union(WindowInsets.captionBar)
            .getTop(density),
        maxTopPx
    )
    return padding(top = with(density) { topPx.toDp() })
}

@Composable
fun Modifier.safeNavigationBarPadding(): Modifier {
    val density = LocalDensity.current
    val maxBottomPx = with(density) { MAX_BOTTOM_INSET.toPx() }.toInt()
    val bottomPx = min(WindowInsets.navigationBars.getBottom(density), maxBottomPx)
    return padding(bottom = with(density) { bottomPx.toDp() })
}
