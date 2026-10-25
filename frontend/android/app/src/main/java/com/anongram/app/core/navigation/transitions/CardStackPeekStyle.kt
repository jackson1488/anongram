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
        scaleX = 1f - progress * 0.12f
        scaleY = 1f - progress * 0.12f
        shape = RoundedCornerShape((progress * 30f).dp)
        clip = true
        shadowElevation = progress * 20.dp.toPx()
    }
}
