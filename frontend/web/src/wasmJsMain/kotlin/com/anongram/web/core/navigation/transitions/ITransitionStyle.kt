package com.anongram.web.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

/**
 * Стратегия анимации перехода (Паттерн «Стратегия»).
 */
interface ITransitionStyle {
    val type: TransitionType

    fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    )
}
