package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №4 Вращение куба на 90°: поворот вокруг правого ребра в глубоком космосе. */
class CubeRotation3DStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.CUBE_ROTATION_3D

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        rotationY = -progress * 90f
        transformOrigin = TransformOrigin(1f, 0.5f)
        cameraDistance = 12 * density
        alpha = 1f - progress * 0.3f
    }
}
