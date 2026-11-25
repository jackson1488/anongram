package com.anongram.app.features.chats.mediaeditor.api

import android.net.Uri
import com.anongram.app.features.chats.attach.api.MediaTtl

/**
 * Результат обработки медиафайла в редакторе.
 * Правки применяются к копии в приватном кэше. Метаданные (EXIF/GPS) очищены.
 */
data class EditedMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val caption: String = "",
    val hd: Boolean = false,
    val ttl: MediaTtl = MediaTtl.Never,
    val width: Int = 0,
    val height: Int = 0,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val isMuted: Boolean = false,
    val coverUri: Uri? = null,
    val quality: String = "HD"
)
