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
        val towardEdgeDir = if (config.swipeEdge == 1) 1f else -1f
        val inwardDir = -towardEdgeDir
        val yOffsetFactor = (config.touchYFraction - 0.5f).coerceIn(-0.5f, 0.5f)
        val scale = (1f - progress * 0.18f).coerceIn(0.80f, 1f)
        scaleX = scale
        scaleY = scale
        shape = RoundedCornerShape((progress * 28f).dp)
        clip = progress > 0.01f
        shadowElevation = progress * 18.dp.toPx()
        translationY = yOffsetFactor * progress * size.height * 0.06f

        val holdFraction = 0.22f
        if (config.isPredictiveHolding) {
            translationX = inwardDir * progress * size.width * holdFraction
        } else {
            val releaseP = config.predictiveReleaseProgress.coerceIn(0f, 0.9f)
            val exitT = if (releaseP > 0.001f) {
                ((progress - releaseP) / (1f - releaseP)).coerceIn(0f, 1f)
            } else {
                progress.coerceIn(0f, 1f)
            }
            val startX = inwardDir * releaseP * size.width * holdFraction
            val endX = towardEdgeDir * size.width * 1.05f
            translationX = startX + (endX - startX) * exitT
        }
        alpha = 1f
    }
}
