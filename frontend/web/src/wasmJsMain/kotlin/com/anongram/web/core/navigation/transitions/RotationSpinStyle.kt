package com.anongram.web.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

class RotationSpinStyle : ITransitionStyle {
    override val type: TransitionType = TransitionType.ROTATION_SPIN

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        rotationZ = progress * 90f
        scaleX = 1f - progress * 0.3f
        scaleY = 1f - progress * 0.3f
        alpha = 1f - progress
    }
}
