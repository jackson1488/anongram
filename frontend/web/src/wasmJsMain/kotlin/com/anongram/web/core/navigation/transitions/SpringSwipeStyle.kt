package com.anongram.web.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

class SpringSwipeStyle : ITransitionStyle {
    override val type: TransitionType = TransitionType.SPRING_SWIPE

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        val cardScale = if (config.enablePredictiveBackCombo && isExit) 1f - progress * 0.10f else 1f
        val cornerRadius = if (config.enablePredictiveBackCombo && isExit) progress * 30f else 0f
        translationX = progress * size.width
        scaleX = cardScale
        scaleY = cardScale
        shape = RoundedCornerShape(cornerRadius.dp)
        clip = cornerRadius > 0f
        shadowElevation = if (isExit) progress * 16.dp.toPx() else 0f
    }
}
