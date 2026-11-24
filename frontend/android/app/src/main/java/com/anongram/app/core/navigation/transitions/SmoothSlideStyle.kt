package com.anongram.app.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/**
 * Плавный уезд экрана до самого края (как в Telegram):
 * следует за пальцем без скачков и плавно уходит за левый/правый край с мягким скруглением.
 */
class SmoothSlideStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.SMOOTH_SLIDE

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        val edgeDir = if (config.swipeEdge == 1) -1f else 1f
        val yOffsetFactor = (config.touchYFraction - 0.5f).coerceIn(-0.5f, 0.5f)
        val scale = (1f - progress * 0.12f).coerceIn(0.86f, 1f)
        scaleX = scale
        scaleY = scale
        shape = RoundedCornerShape((progress * 24f).dp)
        clip = progress > 0.01f
        shadowElevation = progress * 14.dp.toPx()
        translationY = yOffsetFactor * progress * size.height * 0.04f
        translationX = edgeDir * progress * size.width
        alpha = 1f
    }
}
