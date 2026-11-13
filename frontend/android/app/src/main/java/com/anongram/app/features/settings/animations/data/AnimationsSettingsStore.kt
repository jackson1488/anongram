package com.anongram.app.features.settings.animations.data

import android.content.Context
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
    val statusCheckAnimEnabled: Boolean = true
) {
    val speedMultiplier: Float
        get() = when (animationSpeedOption) {
            "0.5x" -> 1.8f
            "1.5x" -> 0.7f
            "2.0x" -> 0.5f
            else -> 1.0f
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

    @Volatile
    private var initialized = false

    private val _state = MutableStateFlow(AnimationsSettingsState())
    val state: StateFlow<AnimationsSettingsState> = _state.asStateFlow()

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
                statusCheckAnimEnabled = prefs.getBoolean(KEY_STATUS_CHECK_ANIM, true)
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
}
