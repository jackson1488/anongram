package com.anongram.app.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/**
 * №1 Флагманский живой жест 1:1: сжатие уходящего экрана в карточку
 * (scale 0.90, скругление до 30dp) с пружинным овершутом приземления.
 */
class SpringSwipeStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.SPRING_SWIPE

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        if (config.enablePredictiveBackCombo && isExit) {
            val cardScale = 1f - progress * 0.18f
            val cornerRadius = progress * 30f
            translationX = if (progress <= 0.65f) {
                progress * size.width * 0.42f
            } else {
                val tail = (progress - 0.65f) / 0.35f
                size.width * (0.273f + tail * 0.727f)
            }
            scaleX = cardScale
            scaleY = cardScale
            shape = RoundedCornerShape(cornerRadius.dp)
            clip = cornerRadius > 0.5f
            shadowElevation = progress * 16.dp.toPx()
            alpha = if (progress > 0.85f) 1f - ((progress - 0.85f) / 0.15f) else 1f
        } else {
            translationX = progress * size.width
            scaleX = 1f
            scaleY = 1f
            clip = false
            shadowElevation = if (isExit) progress * 16.dp.toPx() else 0f
        }
    }
}
