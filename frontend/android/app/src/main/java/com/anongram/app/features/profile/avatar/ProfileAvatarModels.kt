package com.anongram.app.features.profile.avatar

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val MAX_PROFILE_VIDEO_MS = 20_000L

/**
 * Активный аватар собственного профиля (фото, видео до 20с, эмодзи или монограмма).
 */
sealed interface ProfileAvatar {
    data object None : ProfileAvatar

    data class Photo(
        val uriString: String,
        val zoom: Float = 1f,
        val panX: Float = 0f,
        val panY: Float = 0f,
        val rotationDegrees: Int = 0,
        val mirrorHorizontal: Boolean = false
    ) : ProfileAvatar {
        val uri: Uri get() = Uri.parse(uriString)
    }

    data class Video(
        val uriString: String,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = MAX_PROFILE_VIDEO_MS,
        val thumbnailFrameMs: Long = 0L,
        val zoom: Float = 1f,
        val panX: Float = 0f,
        val panY: Float = 0f,
        val rotationDegrees: Int = 0,
        val mirrorHorizontal: Boolean = false
    ) : ProfileAvatar {
        val uri: Uri get() = Uri.parse(uriString)
    }

    data class Emoji(
        val emoji: String,
        val bgStartArgb: Long,
        val bgEndArgb: Long
    ) : ProfileAvatar {
        val bgStart: Color get() = Color(bgStartArgb.toInt())
        val bgEnd: Color get() = Color(bgEndArgb.toInt())
    }
}

object ProfileAvatarStore {
    private const val PREFS = "anongram_profile_avatar"
    private const val KEY_TYPE = "type"
    private const val KEY_URI = "uri"
    private const val KEY_ZOOM = "zoom"
    private const val KEY_PAN_X = "pan_x"
    private const val KEY_PAN_Y = "pan_y"
    private const val KEY_ROT = "rot"
    private const val KEY_MIRROR = "mirror"
    private const val KEY_TRIM_START = "trim_start"
    private const val KEY_TRIM_END = "trim_end"
    private const val KEY_THUMB_MS = "thumb_ms"
    private const val KEY_EMOJI = "emoji"
    private const val KEY_BG_START = "bg_start"
    private const val KEY_BG_END = "bg_end"

    private val _avatar = MutableStateFlow<ProfileAvatar>(ProfileAvatar.None)
    val avatar: StateFlow<ProfileAvatar> = _avatar.asStateFlow()

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _avatar.value = when (p.getString(KEY_TYPE, "none")) {
            "photo" -> {
                val uri = p.getString(KEY_URI, null)
                if (uri.isNullOrBlank()) ProfileAvatar.None
                else ProfileAvatar.Photo(
                    uriString = uri,
                    zoom = p.getFloat(KEY_ZOOM, 1f),
                    panX = p.getFloat(KEY_PAN_X, 0f),
                    panY = p.getFloat(KEY_PAN_Y, 0f),
                    rotationDegrees = p.getInt(KEY_ROT, 0),
                    mirrorHorizontal = p.getBoolean(KEY_MIRROR, false)
                )
            }
            "video" -> {
                val uri = p.getString(KEY_URI, null)
                if (uri.isNullOrBlank()) ProfileAvatar.None
                else ProfileAvatar.Video(
                    uriString = uri,
                    trimStartMs = p.getLong(KEY_TRIM_START, 0L),
                    trimEndMs = p.getLong(KEY_TRIM_END, MAX_PROFILE_VIDEO_MS),
                    thumbnailFrameMs = p.getLong(KEY_THUMB_MS, 0L),
                    zoom = p.getFloat(KEY_ZOOM, 1f),
                    panX = p.getFloat(KEY_PAN_X, 0f),
                    panY = p.getFloat(KEY_PAN_Y, 0f),
                    rotationDegrees = p.getInt(KEY_ROT, 0),
                    mirrorHorizontal = p.getBoolean(KEY_MIRROR, false)
                )
            }
            "emoji" -> ProfileAvatar.Emoji(
                emoji = p.getString(KEY_EMOJI, "🦆") ?: "🦆",
                bgStartArgb = p.getLong(KEY_BG_START, 0xFF62C8FA),
                bgEndArgb = p.getLong(KEY_BG_END, 0xFF2B6FD9)
            )
            else -> ProfileAvatar.None
        }
    }

    fun setAvatar(context: Context, avatar: ProfileAvatar) {
        loaded = true
        _avatar.value = avatar
        val editor = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        when (avatar) {
            is ProfileAvatar.None -> editor.clear().putString(KEY_TYPE, "none")
            is ProfileAvatar.Photo -> editor
                .putString(KEY_TYPE, "photo")
                .putString(KEY_URI, avatar.uriString)
                .putFloat(KEY_ZOOM, avatar.zoom)
                .putFloat(KEY_PAN_X, avatar.panX)
                .putFloat(KEY_PAN_Y, avatar.panY)
                .putInt(KEY_ROT, avatar.rotationDegrees)
                .putBoolean(KEY_MIRROR, avatar.mirrorHorizontal)
            is ProfileAvatar.Video -> editor
                .putString(KEY_TYPE, "video")
                .putString(KEY_URI, avatar.uriString)
                .putLong(KEY_TRIM_START, avatar.trimStartMs)
                .putLong(KEY_TRIM_END, avatar.trimEndMs)
                .putLong(KEY_THUMB_MS, avatar.thumbnailFrameMs)
                .putFloat(KEY_ZOOM, avatar.zoom)
                .putFloat(KEY_PAN_X, avatar.panX)
                .putFloat(KEY_PAN_Y, avatar.panY)
                .putInt(KEY_ROT, avatar.rotationDegrees)
                .putBoolean(KEY_MIRROR, avatar.mirrorHorizontal)
            is ProfileAvatar.Emoji -> editor
                .putString(KEY_TYPE, "emoji")
                .putString(KEY_EMOJI, avatar.emoji)
                .putLong(KEY_BG_START, avatar.bgStartArgb)
                .putLong(KEY_BG_END, avatar.bgEndArgb)
        }
        editor.apply()
    }
}
