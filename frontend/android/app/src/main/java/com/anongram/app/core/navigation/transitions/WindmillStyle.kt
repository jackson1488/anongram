package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №7 Ветряная мельница: вращение экрана вокруг верхнего левого угла. */
class WindmillStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.WINDMILL

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        transformOrigin = TransformOrigin(0f, 0f)
        rotationZ = progress * 90f
        alpha = 1f - progress
    }
}
