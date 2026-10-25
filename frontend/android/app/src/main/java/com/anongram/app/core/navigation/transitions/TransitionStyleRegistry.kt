package com.anongram.app.core.navigation.transitions

import com.anongram.app.core.navigation.TransitionType

/**
 * Реестр стратегий переходов. Добавление новой анимации = один новый класс
 * [ITransitionStyle] + одна строка здесь; экраны и фасад не меняются.
 */
object TransitionStyleRegistry {

    private val styles: Map<TransitionType, ITransitionStyle> = listOf(
        SpringSwipeStyle(),
        CupertinoParallaxStyle(),
        RealBook3DStyle(),
        CubeRotation3DStyle(),
        RotationSpinStyle(),
        CascadeStyle(),
        WindmillStyle(),
        ParticleDissolveStyle(),
        CardStackPeekStyle(),
        PredictiveBackStackStyle(),
        InstantStyle()
    ).associateBy { it.type }

    fun resolve(type: TransitionType): ITransitionStyle =
        styles[type] ?: error("Не зарегистрирован ITransitionStyle для типа $type")
}
