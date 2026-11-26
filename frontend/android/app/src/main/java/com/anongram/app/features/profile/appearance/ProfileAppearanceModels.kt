package com.anongram.app.features.profile.appearance

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Пресет цвета/градиента обложки профиля (8 сплошных + 8 двухцветных градиентов).
 */
data class ProfileColorPreset(
    val id: String,
    val primary: Color,
    val secondary: Color,
    val isGradient: Boolean
)

/**
 * Состояние оформления профиля (обложка, фоновый эмодзи, цвет имени).
 */
data class ProfileAppearanceState(
    val colorPresetId: String? = null,
    val backgroundEmoji: String? = null,
    val nameColorPresetId: String? = null
) {
    val preset: ProfileColorPreset?
        get() = ProfileColorPresets.findById(colorPresetId)

    val namePreset: ProfileColorPreset?
        get() = ProfileColorPresets.findById(nameColorPresetId) ?: preset
}

object ProfileColorPresets {
    val solidPresets: List<ProfileColorPreset> = listOf(
        ProfileColorPreset("solid_blue", Color(0xFF2D7FF9), Color(0xFF1E5EC2), false),
        ProfileColorPreset("solid_green", Color(0xFF39A935), Color(0xFF237A21), false),
        ProfileColorPreset("solid_amber", Color(0xFFD9821E), Color(0xFFA65D0E), false),
        ProfileColorPreset("solid_crimson", Color(0xFFC94A38), Color(0xFF8F2E20), false),
        ProfileColorPreset("solid_violet", Color(0xFF7B4FD6), Color(0xFF5432A8), false),
        ProfileColorPreset("solid_teal", Color(0xFF2AA7B8), Color(0xFF1A7785), false),
        ProfileColorPreset("solid_rose", Color(0xFFC24C84), Color(0xFF8C2D5A), false),
        ProfileColorPreset("solid_slate", Color(0xFF6B7B8C), Color(0xFF475563), false)
    )

    val gradientPresets: List<ProfileColorPreset> = listOf(
        ProfileColorPreset("grad_sky", Color(0xFF62C8FA), Color(0xFF2B6FD9), true),
        ProfileColorPreset("grad_lime", Color(0xFF87CC4B), Color(0xFF318A3A), true),
        ProfileColorPreset("grad_gold", Color(0xFFF5B749), Color(0xFFC26D19), true),
        ProfileColorPreset("grad_coral", Color(0xFFFF8866), Color(0xFFB83E30), true),
        ProfileColorPreset("grad_orchid", Color(0xFFD87BFF), Color(0xFF6A3AC4), true),
        ProfileColorPreset("grad_mint", Color(0xFF6EE2C7), Color(0xFF1F8796), true),
        ProfileColorPreset("grad_berry", Color(0xFFFF7AA8), Color(0xFFA83269), true),
        ProfileColorPreset("grad_silver", Color(0xFFA7B5C4), Color(0xFF576573), true)
    )

    val all: List<ProfileColorPreset> = solidPresets + gradientPresets

    val emojiOptions: List<String> = listOf(
        "🔥", "⚡", "✨", "🛡️", "🎭", "💎", "🌙", "🦊",
        "🐉", "🌸", "🍀", "🎵", "🚀", "❄️", "👑", "🖤"
    )

    fun findById(id: String?): ProfileColorPreset? =
        if (id == null) null else all.firstOrNull { it.id == id }
}

/**
 * Реактивный движок оформления профиля.
 * Управляет стилем собственного профиля и стилями профилей собеседников в чате.
 */
object ProfileAppearanceStore {
    private const val PREFS = "anongram_profile_appearance"
    private const val KEY_COLOR = "my_color_preset"
    private const val KEY_EMOJI = "my_bg_emoji"
    private const val KEY_NAME_COLOR = "my_name_color"

    private val _myAppearance = MutableStateFlow(ProfileAppearanceState())
    val myAppearance: StateFlow<ProfileAppearanceState> = _myAppearance.asStateFlow()

    private val _peerAppearances = MutableStateFlow<Map<String, ProfileAppearanceState>>(emptyMap())
    val peerAppearances: StateFlow<Map<String, ProfileAppearanceState>> = _peerAppearances.asStateFlow()

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _myAppearance.value = ProfileAppearanceState(
            colorPresetId = prefs.getString(KEY_COLOR, null),
            backgroundEmoji = prefs.getString(KEY_EMOJI, null),
            nameColorPresetId = prefs.getString(KEY_NAME_COLOR, null)
        )
    }

    fun updateMyAppearance(context: Context, state: ProfileAppearanceState) {
        loaded = true
        _myAppearance.value = state
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_COLOR, state.colorPresetId)
            .putString(KEY_EMOJI, state.backgroundEmoji)
            .putString(KEY_NAME_COLOR, state.nameColorPresetId)
            .apply()
    }

    fun resetMyAppearance(context: Context) {
        updateMyAppearance(context, ProfileAppearanceState())
    }

    /**
     * Возвращает оформление для профиля собеседника в чате.
     * Если пользователь не задал кастомное оформление для этого ID, используется
     * детерминированный пресет из палитры или пустое (наследующее тему).
     */
    fun appearanceForPeer(peerId: String): ProfileAppearanceState {
        val custom = _peerAppearances.value[peerId]
        if (custom != null) return custom
        val idx = abs(peerId.hashCode()) % (ProfileColorPresets.all.size + 4)
        return if (idx < ProfileColorPresets.all.size) {
            ProfileAppearanceState(colorPresetId = ProfileColorPresets.all[idx].id)
        } else {
            ProfileAppearanceState()
        }
    }

    fun setPeerAppearance(peerId: String, state: ProfileAppearanceState) {
        _peerAppearances.value = _peerAppearances.value + (peerId to state)
    }
}
