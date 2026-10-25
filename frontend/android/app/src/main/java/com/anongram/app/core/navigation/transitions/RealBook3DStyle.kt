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
        rotationY = -progress * 90f
        transformOrigin = TransformOrigin(0f, 0.5f)
        cameraDistance = 8 * density
    }
}
