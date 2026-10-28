package com.anongram.app.features.mediaeditor.api

import android.net.Uri
import com.anongram.app.features.attach.api.MediaTtl

/**
 * Результат обработки медиафайла в редакторе (ТЗ, mediaeditor: onSend(List<EditedMedia>)).
 * Правки применяются к копии в приватном кэше. Метаданные (EXIF/GPS) очищены.
 */
data class EditedMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val caption: String = "",
    val hd: Boolean = false,
    val ttl: MediaTtl = MediaTtl.Never,
    val width: Int = 0,
    val height: Int = 0
)
