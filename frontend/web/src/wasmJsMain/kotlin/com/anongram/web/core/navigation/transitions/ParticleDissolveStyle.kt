package com.anongram.web.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType

class ParticleDissolveStyle : ITransitionStyle {
    override val type: TransitionType = TransitionType.PARTICLE_DISSOLVE

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        alpha = (1f - progress * 1.5f).coerceIn(0f, 1f)
    }
}
