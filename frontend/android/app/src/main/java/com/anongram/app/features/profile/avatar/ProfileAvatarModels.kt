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

import org.json.JSONArray
import org.json.JSONObject

object ProfileAvatarStore {
    private const val PREFS = "anongram_profile_avatar"
    private const val KEY_AVATARS_JSON = "avatars_json"
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

    private val _avatars = MutableStateFlow<List<ProfileAvatar>>(emptyList())
    val avatars: StateFlow<List<ProfileAvatar>> = _avatars.asStateFlow()

    private val _avatar = MutableStateFlow<ProfileAvatar>(ProfileAvatar.None)
    val avatar: StateFlow<ProfileAvatar> = _avatar.asStateFlow()

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rawJson = p.getString(KEY_AVATARS_JSON, null)
        val list = if (!rawJson.isNullOrBlank()) {
            decodeList(rawJson)
        } else {
            val legacy = loadLegacySingle(p)
            if (legacy != ProfileAvatar.None) listOf(legacy) else emptyList()
        }
        applyList(list)
    }

    fun setAvatar(context: Context, avatar: ProfileAvatar) {
        ensureLoaded(context)
        if (avatar is ProfileAvatar.None) {
            saveList(context, emptyList())
        } else {
            addAvatar(context, avatar)
        }
    }

    fun addAvatar(context: Context, avatar: ProfileAvatar) {
        if (avatar is ProfileAvatar.None) return
        ensureLoaded(context)
        val updated = listOf(avatar) + _avatars.value
        saveList(context, updated)
    }

    fun setPrimaryAvatar(context: Context, index: Int) {
        ensureLoaded(context)
        val current = _avatars.value
        if (index !in current.indices || index == 0) return
        val chosen = current[index]
        val updated = listOf(chosen) + current.filterIndexed { i, _ -> i != index }
        saveList(context, updated)
    }

    fun removeAvatarAt(context: Context, index: Int) {
        ensureLoaded(context)
        val current = _avatars.value
        if (index !in current.indices) return
        val updated = current.filterIndexed { i, _ -> i != index }
        saveList(context, updated)
    }

    private fun applyList(list: List<ProfileAvatar>) {
        _avatars.value = list
        _avatar.value = list.firstOrNull() ?: ProfileAvatar.None
    }

    private fun saveList(context: Context, list: List<ProfileAvatar>) {
        loaded = true
        applyList(list)
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putString(KEY_AVATARS_JSON, encodeList(list)).apply()
    }

    private fun loadLegacySingle(p: android.content.SharedPreferences): ProfileAvatar {
        return when (p.getString(KEY_TYPE, "none")) {
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

    private fun encodeList(list: List<ProfileAvatar>): String {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            when (item) {
                is ProfileAvatar.None -> continue
                is ProfileAvatar.Photo -> {
                    obj.put(KEY_TYPE, "photo")
                    obj.put(KEY_URI, item.uriString)
                    obj.put(KEY_ZOOM, item.zoom.toDouble())
                    obj.put(KEY_PAN_X, item.panX.toDouble())
                    obj.put(KEY_PAN_Y, item.panY.toDouble())
                    obj.put(KEY_ROT, item.rotationDegrees)
                    obj.put(KEY_MIRROR, item.mirrorHorizontal)
                }
                is ProfileAvatar.Video -> {
                    obj.put(KEY_TYPE, "video")
                    obj.put(KEY_URI, item.uriString)
                    obj.put(KEY_TRIM_START, item.trimStartMs)
                    obj.put(KEY_TRIM_END, item.trimEndMs)
                    obj.put(KEY_THUMB_MS, item.thumbnailFrameMs)
                    obj.put(KEY_ZOOM, item.zoom.toDouble())
                    obj.put(KEY_PAN_X, item.panX.toDouble())
                    obj.put(KEY_PAN_Y, item.panY.toDouble())
                    obj.put(KEY_ROT, item.rotationDegrees)
                    obj.put(KEY_MIRROR, item.mirrorHorizontal)
                }
                is ProfileAvatar.Emoji -> {
                    obj.put(KEY_TYPE, "emoji")
                    obj.put(KEY_EMOJI, item.emoji)
                    obj.put(KEY_BG_START, item.bgStartArgb)
                    obj.put(KEY_BG_END, item.bgEndArgb)
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeList(raw: String): List<ProfileAvatar> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    when (obj.optString(KEY_TYPE)) {
                        "photo" -> {
                            val uri = obj.optString(KEY_URI)
                            if (uri.isNotBlank()) {
                                add(
                                    ProfileAvatar.Photo(
                                        uriString = uri,
                                        zoom = obj.optDouble(KEY_ZOOM, 1.0).toFloat(),
                                        panX = obj.optDouble(KEY_PAN_X, 0.0).toFloat(),
                                        panY = obj.optDouble(KEY_PAN_Y, 0.0).toFloat(),
                                        rotationDegrees = obj.optInt(KEY_ROT, 0),
                                        mirrorHorizontal = obj.optBoolean(KEY_MIRROR, false)
                                    )
                                )
                            }
                        }
                        "video" -> {
                            val uri = obj.optString(KEY_URI)
                            if (uri.isNotBlank()) {
                                add(
                                    ProfileAvatar.Video(
                                        uriString = uri,
                                        trimStartMs = obj.optLong(KEY_TRIM_START, 0L),
                                        trimEndMs = obj.optLong(KEY_TRIM_END, MAX_PROFILE_VIDEO_MS),
                                        thumbnailFrameMs = obj.optLong(KEY_THUMB_MS, 0L),
                                        zoom = obj.optDouble(KEY_ZOOM, 1.0).toFloat(),
                                        panX = obj.optDouble(KEY_PAN_X, 0.0).toFloat(),
                                        panY = obj.optDouble(KEY_PAN_Y, 0.0).toFloat(),
                                        rotationDegrees = obj.optInt(KEY_ROT, 0),
                                        mirrorHorizontal = obj.optBoolean(KEY_MIRROR, false)
                                    )
                                )
                            }
                        }
                        "emoji" -> add(
                            ProfileAvatar.Emoji(
                                emoji = obj.optString(KEY_EMOJI, "🦆"),
                                bgStartArgb = obj.optLong(KEY_BG_START, 0xFF62C8FA),
                                bgEndArgb = obj.optLong(KEY_BG_END, 0xFF2B6FD9)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}
