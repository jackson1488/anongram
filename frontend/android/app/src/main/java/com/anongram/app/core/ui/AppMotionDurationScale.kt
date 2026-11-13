package com.anongram.app.core.ui

import androidx.compose.ui.MotionDurationScale
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore

/**
 * Единый [MotionDurationScale] для анимаций AnonGram, который зависит от внутренней
 * настройки «Уменьшение движения» ([AnimationsSettingsStore]) и не ломается,
 * когда в системе Android выставлено `animator_duration_scale = 0.0`.
 */
object AppMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = if (AnimationsSettingsStore.state.value.reduceMotionEnabled) 0f else 1f
}
