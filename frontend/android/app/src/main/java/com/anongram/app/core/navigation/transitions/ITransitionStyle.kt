package com.anongram.app.core.navigation.transitions

import androidx.compose.ui.graphics.GraphicsLayerScope
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.TransitionType

/**
 * Стратегия анимации перехода (Паттерн «Стратегия»).
 *
 * Каждая из 11 анимаций AnonGram — изолированный трансформер, подключаемый
 * через [TransitionStyleRegistry] без изменения экранов: экран знает только прогресс 0..1,
 * а вся физика движения живёт внутри конкретного стиля.
 */
interface ITransitionStyle {

    /** Тип перехода, за который отвечает эта стратегия. */
    val type: TransitionType

    /** Применяет трансформации графического слоя к экрану. */
    fun GraphicsLayerScope.transform(
        progress: Float,
        isExit: Boolean,
        config: NavigationTransitionConfig,
        density: Float
    )
}
