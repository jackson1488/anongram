package com.anongram.app.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №9 Усадка в плавающую карточку колоды с радиусом 30dp и тенью. */
class CardStackPeekStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.CARD_STACK_PEEK

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        val scale = (1f - progress * 0.36f).coerceIn(0.76f, 1f)
        scaleX = scale
        scaleY = scale
        val corner = (progress * 84f).coerceIn(0f, 32f)
        shape = RoundedCornerShape(corner.dp)
        clip = progress > 0.01f
        shadowElevation = (progress * 24f).coerceAtMost(20f).dp.toPx()
        alpha = if (!config.isPredictiveHolding && progress > 0.72f) {
            (1f - ((progress - 0.72f) / 0.28f)).coerceIn(0f, 1f)
        } else {
            1f
        }
    }
}
