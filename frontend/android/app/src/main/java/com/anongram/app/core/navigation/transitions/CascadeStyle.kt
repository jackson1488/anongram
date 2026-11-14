package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №6 Вертикальный каскад: экран складывается гармошкой вниз. */
class CascadeStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.CASCADE

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        if (isExit) {
            transformOrigin = TransformOrigin(0.5f, 1f)
            scaleY = (1f - progress).coerceAtLeast(0.01f)
            translationY = progress * 72.dp.toPx()
            alpha = 1f - progress
        } else {
            transformOrigin = TransformOrigin(0.5f, 0f)
            scaleY = (1f - progress * 0.78f).coerceAtLeast(0.15f)
            translationY = -progress * 28.dp.toPx()
            alpha = 1f - progress * 0.15f
        }
    }
}
