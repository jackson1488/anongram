package com.anongram.web.features.settings.animations.data

import com.anongram.web.platform.WebStorage
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
    private const val PREFIX = "anongram_anim_"
    private var initialized = false

    private val _state = MutableStateFlow(AnimationsSettingsState())
    val state: StateFlow<AnimationsSettingsState> = _state.asStateFlow()

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        _state.value = AnimationsSettingsState(
            screenTransitionsEnabled = WebStorage.getString("${PREFIX}screen_transitions")?.toBooleanStrictOrNull() ?: true,
            glassBlurEnabled = WebStorage.getString("${PREFIX}glass_blur")?.toBooleanStrictOrNull() ?: true,
            stickersLoopEnabled = WebStorage.getString("${PREFIX}stickers_loop")?.toBooleanStrictOrNull() ?: true,
            reactionsEffectsEnabled = WebStorage.getString("${PREFIX}reactions_effects")?.toBooleanStrictOrNull() ?: true,
            animatedBackgroundsEnabled = WebStorage.getString("${PREFIX}animated_bg")?.toBooleanStrictOrNull() ?: true,
            reduceMotionEnabled = WebStorage.getString("${PREFIX}reduce_motion")?.toBooleanStrictOrNull() ?: false,
            animationSpeedOption = WebStorage.getString("${PREFIX}speed") ?: "1.0x",
            messageAppearEnabled = WebStorage.getString("${PREFIX}msg_appear")?.toBooleanStrictOrNull() ?: true,
            messageAppearStyle = MessageAppearStyle.fromId(WebStorage.getString("${PREFIX}msg_appear_style")),
            tokenStreamSpeed = TokenStreamSpeed.fromId(WebStorage.getString("${PREFIX}token_speed")),
            messageDeleteStyle = MessageDeleteStyle.fromId(WebStorage.getString("${PREFIX}msg_delete_style")),
            listItemShiftEnabled = WebStorage.getString("${PREFIX}list_shift")?.toBooleanStrictOrNull() ?: true,
            statusCheckAnimEnabled = WebStorage.getString("${PREFIX}status_check")?.toBooleanStrictOrNull() ?: true
        )
    }

    fun update(transform: (AnimationsSettingsState) -> AnimationsSettingsState) {
        _state.update(transform)
        val s = _state.value
        WebStorage.putString("${PREFIX}screen_transitions", s.screenTransitionsEnabled.toString())
        WebStorage.putString("${PREFIX}glass_blur", s.glassBlurEnabled.toString())
        WebStorage.putString("${PREFIX}stickers_loop", s.stickersLoopEnabled.toString())
        WebStorage.putString("${PREFIX}reactions_effects", s.reactionsEffectsEnabled.toString())
        WebStorage.putString("${PREFIX}animated_bg", s.animatedBackgroundsEnabled.toString())
        WebStorage.putString("${PREFIX}reduce_motion", s.reduceMotionEnabled.toString())
        WebStorage.putString("${PREFIX}speed", s.animationSpeedOption)
        WebStorage.putString("${PREFIX}msg_appear", s.messageAppearEnabled.toString())
        WebStorage.putString("${PREFIX}msg_appear_style", s.messageAppearStyle.id)
        WebStorage.putString("${PREFIX}token_speed", s.tokenStreamSpeed.id)
        WebStorage.putString("${PREFIX}msg_delete_style", s.messageDeleteStyle.id)
        WebStorage.putString("${PREFIX}list_shift", s.listItemShiftEnabled.toString())
        WebStorage.putString("${PREFIX}status_check", s.statusCheckAnimEnabled.toString())
    }
}
