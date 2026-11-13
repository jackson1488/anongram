package com.anongram.web.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

class CupertinoParallaxStyle : ITransitionStyle {
    override val type: TransitionType = TransitionType.CUPERTINO_PARALLAX

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        translationX = progress * size.width
    }
}
