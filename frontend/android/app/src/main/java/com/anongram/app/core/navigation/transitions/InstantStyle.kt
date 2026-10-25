package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №11 Турбо-режим 0 мс: максимальная скорость и стелс, трансформаций нет. */
class InstantStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.INSTANT

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) = Unit
}
