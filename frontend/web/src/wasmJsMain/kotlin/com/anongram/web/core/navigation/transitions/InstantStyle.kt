package com.anongram.web.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

class InstantStyle : ITransitionStyle {
    override val type: TransitionType = TransitionType.INSTANT

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) = Unit
}
