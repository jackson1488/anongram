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
        cameraDistance = 10 * density
        if (isExit) {
            transformOrigin = TransformOrigin(1f, 0.5f)
            rotationY = -progress * 85f
            translationX = progress * size.width * 0.25f
        } else {
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = progress * 85f
            translationX = progress * size.width * 0.25f
        }
        alpha = (1f - progress * 0.25f).coerceIn(0f, 1f)
    }
}
