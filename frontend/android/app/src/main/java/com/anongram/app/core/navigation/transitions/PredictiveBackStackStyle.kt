package com.anongram.app.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №10 Предиктивный бэк Android 14/15: сдвиг на 80% ширины + скругление карточки. */
class PredictiveBackStackStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.PREDICTIVE_BACK

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        val scale = (1f - progress * 0.16f).coerceIn(0.80f, 1f)
        val edgeDir = if (config.swipeEdge == 1) -1f else 1f
        val yOffsetFactor = (config.touchYFraction - 0.5f).coerceIn(-0.5f, 0.5f)
        translationX = edgeDir * progress * size.width * 0.10f +
            (if (progress > 0.65f) edgeDir * ((progress - 0.65f) / 0.35f) * size.width * 0.85f else 0f)
        translationY = yOffsetFactor * progress * size.height * 0.08f
        scaleX = scale
        scaleY = scale
        shape = RoundedCornerShape((progress * 28f).dp)
        clip = progress > 0.01f
        shadowElevation = progress * 18.dp.toPx()
        alpha = if (progress > 0.86f) 1f - ((progress - 0.86f) / 0.14f) else 1f
    }
}
