package com.anongram.app.core.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Spring physics parameters and easing curves for AnonGram navigation
 */
object PhysicsSprings {
    // Flagship overshoot curve: Cubic(0.22, 1.25, 0.36, 1.0)
    val OvershootEasing = CubicBezierEasing(0.22f, 1.25f, 0.36f, 1.0f)
    
    // Natural decelerate curve
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // Jetpack Compose SpringSpec matching stiffness = 320, damping = 0.76
    fun <T> screenSpringSpec(
        damping: Float = 0.76f,
        stiffness: Float = 320f
    ) = spring<T>(
        dampingRatio = damping,
        stiffness = stiffness
    )
}
