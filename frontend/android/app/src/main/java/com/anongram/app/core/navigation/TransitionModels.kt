package com.anongram.app.core.navigation

/**
 * All 11 Screen Transition Animation Styles preserved from AnonGram specification
 */
enum class TransitionType {
    SPRING_SWIPE,        // 1:1 finger tracking swipe with spring overshoot landing
    CUPERTINO_PARALLAX,  // iOS Cupertino 33% depth parallax + edge shadow
    REAL_BOOK_3D,        // 3D book page curl with spine darkening shadow
    CUBE_ROTATION_3D,    // 3D cube rotation in deep black space
    ROTATION_SPIN,       // Spin around center with scale shrink
    CASCADE,             // Vertical cascade / accordion collapse downward
    WINDMILL,            // Rotation around top-left corner
    PARTICLE_DISSOLVE,   // Screen dissolves/reassembles from particles
    CARD_STACK_PEEK,     // Shrinks into floating card with 30dp radius
    PREDICTIVE_BACK,     // Android 14+ Predictive Back gesture exit
    INSTANT              // 0 ms turbo mode for maximum speed and stealth
}

/**
 * 4 Styles of particles for screen evaporation
 */
enum class ParticleStyle {
    VAPOR_DUST,    // Glowing cyan cyber dust
    PUZZLE_PIECES, // Rotating outlined cyan puzzle pieces
    DIGITAL_VOXELS,// Matrix green square voxels
    ASH_EMBERS     // Incinerating flame orange embers
}

/**
 * Configuration for navigation transitions, speed multiplier and physics parameters
 */
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
