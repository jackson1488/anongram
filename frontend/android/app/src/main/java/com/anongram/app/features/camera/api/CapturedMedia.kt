package com.anongram.app.features.camera.api

import android.net.Uri

/**
 * Результат съёмки камеры (ТЗ, camera: CapturedMedia).
 * Сохраняется в приватный кэш приложения, НЕ в системную галерею.
 * Геометки в EXIF не пишутся.
 */
data class CapturedMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)
