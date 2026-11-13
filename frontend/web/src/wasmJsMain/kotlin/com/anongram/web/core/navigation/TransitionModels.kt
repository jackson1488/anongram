package com.anongram.web.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TransitionType(val displayName: String) {
    SPRING_SWIPE("Spring Swipe"),
    CUPERTINO_PARALLAX("Cupertino"),
    REAL_BOOK_3D("Book 3D"),
    CUBE_ROTATION_3D("Cube 3D"),
    ROTATION_SPIN("Spin"),
    CASCADE("Cascade"),
    WINDMILL("Windmill"),
    PARTICLE_DISSOLVE("Particles"),
    CARD_STACK_PEEK("Card Peek"),
    PREDICTIVE_BACK("Predictive"),
    INSTANT("Instant")
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

    companion object {
        var currentType: TransitionType by mutableStateOf(TransitionType.SPRING_SWIPE)
        fun current(): NavigationTransitionConfig = NavigationTransitionConfig(transitionType = currentType)
    }
}
