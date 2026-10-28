package com.anongram.app.features.mediaeditor.api

import android.net.Uri
import com.anongram.app.features.attach.api.MediaTtl
import java.util.UUID

/**
 * Элемент для редактирования в редакторе медиа (ТЗ, mediaeditor: items).
 */
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
    val trimEndMs: Long = 0L
)
