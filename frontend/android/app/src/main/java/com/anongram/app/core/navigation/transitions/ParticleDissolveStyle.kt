package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/**
 * №8 Испарение экрана на частицы. Графический слой только тает;
 * сами частицы (4 стиля) рисует ParticleOverlay поверх контента.
 */
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
