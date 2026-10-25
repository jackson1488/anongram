package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №5 Вращение вокруг центра с усадкой и растворением. */
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
