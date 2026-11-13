package com.anongram.web.core.navigation

enum class TransitionType {
    SPRING_SWIPE,
    CUPERTINO_PARALLAX,
    REAL_BOOK_3D,
    CUBE_ROTATION_3D,
    ROTATION_SPIN,
    CASCADE,
    WINDMILL,
    PARTICLE_DISSOLVE,
    CARD_STACK_PEEK,
    PREDICTIVE_BACK,
    INSTANT
}

enum class ParticleStyle {
    VAPOR_DUST,
    PUZZLE_PIECES,
    DIGITAL_VOXELS,
    ASH_EMBERS
}

data class NavigationTransitionConfig(
    val transitionType: TransitionType = TransitionType.SPRING_SWIPE,
    val speedMultiplier: Float = 1.0f,
    val baseDurationMs: Int = 280,
    val springStiffness: Float = 320.0f,
    val springDamping: Float = 0.76f,
    val particleStyle: ParticleStyle = ParticleStyle.VAPOR_DUST,
    val enableGesturePullCombo: Boolean = true,
    val enablePredictiveBackCombo: Boolean = true,
    val allowFullWidthSwipe: Boolean = true
) {
    val effectiveDurationMs: Int
        get() = if (transitionType == TransitionType.INSTANT) 0
        else (baseDurationMs / speedMultiplier).toInt().coerceIn(40, 1000)
}
