package com.anongram.web.core.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring

object PhysicsSprings {
    val OvershootEasing = CubicBezierEasing(0.22f, 1.25f, 0.36f, 1.0f)
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    fun <T> screenSpringSpec(
        damping: Float = 0.76f,
        stiffness: Float = 320f
    ) = spring<T>(
        dampingRatio = damping,
        stiffness = stiffness
    )
}
