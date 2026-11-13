package com.anongram.web.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

class PredictiveBackStackStyle : ITransitionStyle {
    override val type: TransitionType = TransitionType.PREDICTIVE_BACK

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        translationX = progress * size.width * 0.8f
        scaleX = 1f - progress * 0.10f
        scaleY = 1f - progress * 0.10f
        shape = RoundedCornerShape((progress * 30f).dp)
        clip = true
        shadowElevation = progress * 18.dp.toPx()
    }
}
