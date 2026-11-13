package com.anongram.web.core.navigation.transitions

import com.anongram.web.core.navigation.TransitionType

/**
 * Реестр стратегий переходов.
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
