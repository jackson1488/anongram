package com.anongram.app.core.navigation.transitions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №2 Параллакс Cupertino: нижний экран уезжает, верхний скользит с глубиной 33% и скруглением углов. */
class CupertinoParallaxStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.CUPERTINO_PARALLAX

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        val safeProgress = progress.coerceAtLeast(0f)
        translationX = progress * size.width
        val cornerRadius = (safeProgress * 84f).coerceIn(0f, 32f)
        shape = RoundedCornerShape(cornerRadius.dp)
        clip = cornerRadius > 0.5f
        shadowElevation = if (isExit) safeProgress * 16.dp.toPx() else 0f
    }
}
