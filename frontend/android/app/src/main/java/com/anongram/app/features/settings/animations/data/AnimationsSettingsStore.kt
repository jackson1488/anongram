package com.anongram.app.features.settings.animations.data

import android.content.Context
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.ParticleStyle
import com.anongram.app.core.navigation.TransitionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MessageAppearStyle(val id: String) {
    SPRING_FLY("spring_fly"),
    SLIDE_FADE("slide_fade"),
    CORNER_POP("corner_pop"),
    TOKEN_STREAM("token_stream");

    companion object {
        fun fromId(id: String?): MessageAppearStyle =
            entries.firstOrNull { it.id == id } ?: TOKEN_STREAM
    }
}

enum class TokenStreamSpeed(val id: String, val tokenDelayMs: Long) {
    SLOW("slow", 70L),
    MEDIUM("medium", 40L),
    FAST("fast", 22L),
    TURBO("turbo", 10L);

    companion object {
        fun fromId(id: String?): TokenStreamSpeed =
            entries.firstOrNull { it.id == id } ?: FAST
    }
}

enum class MessageDeleteStyle(val id: String) {
    PARTICLES("particles"),
    PUZZLE("puzzle"),
    SLIDE_OUT("slide_out"),
    NONE("none");

    companion object {
        fun fromId(id: String?): MessageDeleteStyle =
            entries.firstOrNull { it.id == id } ?: PARTICLES
    }
}

data class AnimationsSettingsState(
    val screenTransitionsEnabled: Boolean = true,
    val glassBlurEnabled: Boolean = true,
    val stickersLoopEnabled: Boolean = true,
    val reactionsEffectsEnabled: Boolean = true,
    val animatedBackgroundsEnabled: Boolean = true,
    val reduceMotionEnabled: Boolean = false,
    val animationSpeedOption: String = "1.0x",
    val messageAppearEnabled: Boolean = true,
    val messageAppearStyle: MessageAppearStyle = MessageAppearStyle.TOKEN_STREAM,
    val tokenStreamSpeed: TokenStreamSpeed = TokenStreamSpeed.FAST,
    val messageDeleteStyle: MessageDeleteStyle = MessageDeleteStyle.PARTICLES,
    val listItemShiftEnabled: Boolean = true,
    val statusCheckAnimEnabled: Boolean = true,
    val screenTransitionType: TransitionType = TransitionType.SPRING_SWIPE,
    val screenParticleStyle: ParticleStyle = ParticleStyle.VAPOR_DUST,
    val screenTransitionSpringDamping: Float = 0.76f,
    val enableGesturePullCombo: Boolean = true,
    val enablePredictiveBackCombo: Boolean = true
) {
    val speedMultiplier: Float
        get() = when (animationSpeedOption) {
            "0.5x" -> 1.8f
            "1.5x" -> 0.7f
            "2.0x" -> 0.5f
            else -> 1.0f
        }

    /** Скорость для NavigationTransitionConfig: выше 1.0x = быстрее переход. */
    val transitionSpeedMultiplier: Float
        get() = when (animationSpeedOption) {
            "0.5x" -> 0.55f
            "1.5x" -> 1.5f
            "2.0x" -> 2.0f
            else -> 1.0f
        }

    fun toNavigationTransitionConfig(): NavigationTransitionConfig {
        val effectiveType = if (!screenTransitionsEnabled || reduceMotionEnabled) {
            TransitionType.INSTANT
        } else {
            screenTransitionType
        }
        return NavigationTransitionConfig(
            transitionType = effectiveType,
            speedMultiplier = transitionSpeedMultiplier,
            springDamping = screenTransitionSpringDamping.coerceIn(0.5f, 1.0f),
            particleStyle = screenParticleStyle,
            enableGesturePullCombo = enableGesturePullCombo,
            enablePredictiveBackCombo = enablePredictiveBackCombo
        )
    }
}

object AnimationsSettingsStore {
    private const val PREFS_NAME = "anongram_animations_settings"
    private const val KEY_SCREEN_TRANSITIONS = "key_screen_transitions"
    private const val KEY_GLASS_BLUR = "key_glass_blur"
    private const val KEY_STICKERS_LOOP = "key_stickers_loop"
    private const val KEY_REACTIONS_EFFECTS = "key_reactions_effects"
    private const val KEY_ANIMATED_BACKGROUNDS = "key_animated_backgrounds"
    private const val KEY_REDUCE_MOTION = "key_reduce_motion"
    private const val KEY_ANIMATION_SPEED = "key_animation_speed"
    private const val KEY_MESSAGE_APPEAR_ENABLED = "key_message_appear_enabled"
    private const val KEY_MESSAGE_APPEAR_STYLE = "key_message_appear_style"
    private const val KEY_TOKEN_STREAM_SPEED = "key_token_stream_speed"
    private const val KEY_MESSAGE_DELETE_STYLE = "key_message_delete_style"
    private const val KEY_LIST_SHIFT_ENABLED = "key_list_shift_enabled"
    private const val KEY_STATUS_CHECK_ANIM = "key_status_check_anim"
    private const val KEY_SCREEN_TRANSITION_TYPE = "key_screen_transition_type"
    private const val KEY_SCREEN_PARTICLE_STYLE = "key_screen_particle_style"
    private const val KEY_SCREEN_SPRING_DAMPING = "key_screen_spring_damping"
    private const val KEY_COMBO_GESTURE_PULL = "key_combo_gesture_pull"
    private const val KEY_COMBO_PREDICTIVE_BACK = "key_combo_predictive_back"

    @Volatile
    private var initialized = false

    private val _state = MutableStateFlow(AnimationsSettingsState())
    val state: StateFlow<AnimationsSettingsState> = _state.asStateFlow()

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val transitionName = prefs.getString(KEY_SCREEN_TRANSITION_TYPE, TransitionType.SPRING_SWIPE.name)
            val particleName = prefs.getString(KEY_SCREEN_PARTICLE_STYLE, ParticleStyle.VAPOR_DUST.name)
            _state.value = AnimationsSettingsState(
                screenTransitionsEnabled = prefs.getBoolean(KEY_SCREEN_TRANSITIONS, true),
                glassBlurEnabled = prefs.getBoolean(KEY_GLASS_BLUR, true),
                stickersLoopEnabled = prefs.getBoolean(KEY_STICKERS_LOOP, true),
                reactionsEffectsEnabled = prefs.getBoolean(KEY_REACTIONS_EFFECTS, true),
                animatedBackgroundsEnabled = prefs.getBoolean(KEY_ANIMATED_BACKGROUNDS, true),
                reduceMotionEnabled = prefs.getBoolean(KEY_REDUCE_MOTION, false),
                animationSpeedOption = prefs.getString(KEY_ANIMATION_SPEED, "1.0x") ?: "1.0x",
                messageAppearEnabled = prefs.getBoolean(KEY_MESSAGE_APPEAR_ENABLED, true),
                messageAppearStyle = MessageAppearStyle.fromId(
                    prefs.getString(KEY_MESSAGE_APPEAR_STYLE, MessageAppearStyle.TOKEN_STREAM.id)
                ),
                tokenStreamSpeed = TokenStreamSpeed.fromId(
                    prefs.getString(KEY_TOKEN_STREAM_SPEED, TokenStreamSpeed.FAST.id)
                ),
                messageDeleteStyle = MessageDeleteStyle.fromId(
                    prefs.getString(KEY_MESSAGE_DELETE_STYLE, MessageDeleteStyle.PARTICLES.id)
                ),
                listItemShiftEnabled = prefs.getBoolean(KEY_LIST_SHIFT_ENABLED, true),
                statusCheckAnimEnabled = prefs.getBoolean(KEY_STATUS_CHECK_ANIM, true),
                screenTransitionType = TransitionType.entries.firstOrNull { it.name == transitionName }
                    ?: TransitionType.SPRING_SWIPE,
                screenParticleStyle = ParticleStyle.entries.firstOrNull { it.name == particleName }
                    ?: ParticleStyle.VAPOR_DUST,
                screenTransitionSpringDamping = prefs.getFloat(KEY_SCREEN_SPRING_DAMPING, 0.76f)
                    .coerceIn(0.5f, 1.0f),
                enableGesturePullCombo = prefs.getBoolean(KEY_COMBO_GESTURE_PULL, true),
                enablePredictiveBackCombo = prefs.getBoolean(KEY_COMBO_PREDICTIVE_BACK, true)
            )
            initialized = true
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setScreenTransitions(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_SCREEN_TRANSITIONS, enabled).apply()
        _state.update { it.copy(screenTransitionsEnabled = enabled) }
    }

    fun setGlassBlur(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_GLASS_BLUR, enabled).apply()
        _state.update { it.copy(glassBlurEnabled = enabled) }
    }

    fun setStickersLoop(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_STICKERS_LOOP, enabled).apply()
        _state.update { it.copy(stickersLoopEnabled = enabled) }
    }

    fun setReactionsEffects(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_REACTIONS_EFFECTS, enabled).apply()
        _state.update { it.copy(reactionsEffectsEnabled = enabled) }
    }

    fun setAnimatedBackgrounds(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_ANIMATED_BACKGROUNDS, enabled).apply()
        _state.update { it.copy(animatedBackgroundsEnabled = enabled) }
    }

    fun setReduceMotion(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()
        _state.update { it.copy(reduceMotionEnabled = enabled) }
    }

    fun setAnimationSpeedOption(context: Context, option: String) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_ANIMATION_SPEED, option).apply()
        _state.update { it.copy(animationSpeedOption = option) }
    }

    fun setMessageAppearEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_MESSAGE_APPEAR_ENABLED, enabled).apply()
        _state.update { it.copy(messageAppearEnabled = enabled) }
    }

    fun setMessageAppearStyle(context: Context, style: MessageAppearStyle) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_MESSAGE_APPEAR_STYLE, style.id).apply()
        _state.update { it.copy(messageAppearStyle = style) }
    }

    fun setTokenStreamSpeed(context: Context, speed: TokenStreamSpeed) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_TOKEN_STREAM_SPEED, speed.id).apply()
        _state.update { it.copy(tokenStreamSpeed = speed) }
    }

    fun setMessageDeleteStyle(context: Context, style: MessageDeleteStyle) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_MESSAGE_DELETE_STYLE, style.id).apply()
        _state.update { it.copy(messageDeleteStyle = style) }
    }

    fun setListItemShiftEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_LIST_SHIFT_ENABLED, enabled).apply()
        _state.update { it.copy(listItemShiftEnabled = enabled) }
    }

    fun setStatusCheckAnimEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_STATUS_CHECK_ANIM, enabled).apply()
        _state.update { it.copy(statusCheckAnimEnabled = enabled) }
    }

    fun setScreenTransitionType(context: Context, type: TransitionType) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_SCREEN_TRANSITION_TYPE, type.name).apply()
        _state.update { it.copy(screenTransitionType = type) }
    }

    fun setScreenParticleStyle(context: Context, style: ParticleStyle) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_SCREEN_PARTICLE_STYLE, style.name).apply()
        _state.update { it.copy(screenParticleStyle = style) }
    }

    fun setScreenTransitionSpringDamping(context: Context, damping: Float) {
        val clamped = damping.coerceIn(0.5f, 1.0f)
        ensureInitialized(context)
        prefs(context).edit().putFloat(KEY_SCREEN_SPRING_DAMPING, clamped).apply()
        _state.update { it.copy(screenTransitionSpringDamping = clamped) }
    }

    fun setEnableGesturePullCombo(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_COMBO_GESTURE_PULL, enabled).apply()
        _state.update { it.copy(enableGesturePullCombo = enabled) }
    }

    fun setEnablePredictiveBackCombo(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_COMBO_PREDICTIVE_BACK, enabled).apply()
        _state.update { it.copy(enablePredictiveBackCombo = enabled) }
    }
}
