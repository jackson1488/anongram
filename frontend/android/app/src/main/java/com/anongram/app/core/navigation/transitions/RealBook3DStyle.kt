package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/** №3 3D-перелистывание страницы книги: поворот вокруг корешка слева. */
class RealBook3DStyle : ITransitionStyle {

    override val type: TransitionType = TransitionType.REAL_BOOK_3D

    override fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    ) {
        transformOrigin = TransformOrigin(0f, 0.5f)
        cameraDistance = 12 * density
        rotationY = if (isExit) -progress * 85f else -progress * 85f
        alpha = (1f - progress * 0.18f).coerceIn(0f, 1f)
    }
}
