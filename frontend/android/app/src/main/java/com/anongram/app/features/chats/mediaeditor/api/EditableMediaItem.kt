package com.anongram.app.features.chats.mediaeditor.api

import android.net.Uri
import com.anongram.app.features.chats.attach.api.MediaTtl
import com.anongram.app.features.chats.mediaeditor.model.PhotoEditState
import com.anongram.app.features.chats.mediaeditor.model.VideoQuality
import java.util.UUID

data class EditableMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val isVideo: Boolean = false,
    val caption: String = "",
    val hd: Boolean = false,
    val ttl: MediaTtl = MediaTtl.Never,
    val rotationDegrees: Int = 0,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val durationMs: Long = 0L,
    val isMuted: Boolean = false,
    val videoQuality: VideoQuality = VideoQuality.HD_720,
    val coverUri: Uri? = null,
    val coverTimeMs: Long? = null,
    val editState: PhotoEditState = PhotoEditState()
)
