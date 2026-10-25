package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №2 Параллакс Cupertino: нижний экран уезжает, верхний скользит с глубиной 33%. */
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
