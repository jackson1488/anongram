package com.anongram.app.features.chats.mediaeditor.model

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isFull: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    val width: Float get() = (right - left).coerceAtLeast(0.05f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.05f)
}

data class CropTransform(
    val cropRect: CropRect = CropRect(),
    val baseRotation90: Int = 0,
    val straightenDegrees: Float = 0f,
    val mirrorHorizontal: Boolean = false
) {
    val totalRotationDegrees: Float get() = baseRotation90 + straightenDegrees
    val isModified: Boolean
        get() = !cropRect.isFull || baseRotation90 % 360 != 0 ||
            kotlin.math.abs(straightenDegrees) > 0.05f || mirrorHorizontal
}

data class PhotoAdjustments(
    val smoothSkin: Float = 0f,   // 0..1
    val enhance: Float = 0f,      // 0..1
    val exposure: Float = 0f,     // -1..1
    val contrast: Float = 0f,     // -1..1
    val saturation: Float = 0f,   // -1..1
    val warmth: Float = 0f,       // -1..1
    val fade: Float = 0f,         // 0..1
    val highlights: Float = 0f,   // -1..1
    val shadows: Float = 0f,      // -1..1
    val vignette: Float = 0f,     // 0..1
    val grain: Float = 0f,        // 0..1
    val sharpen: Float = 0f       // 0..1
) {
    val isModified: Boolean
        get() = smoothSkin > 0.01f || enhance > 0.01f ||
            kotlin.math.abs(exposure) > 0.01f || kotlin.math.abs(contrast) > 0.01f ||
            kotlin.math.abs(saturation) > 0.01f || kotlin.math.abs(warmth) > 0.01f ||
            fade > 0.01f || kotlin.math.abs(highlights) > 0.01f ||
            kotlin.math.abs(shadows) > 0.01f || vignette > 0.01f ||
            grain > 0.01f || sharpen > 0.01f
}

enum class VideoQuality(val label: String, val maxHeight: Int) {
    SD_480("SD 480", 480),
    HD_720("HD 720", 720),
    FHD_1080("1080p", 1080)
}

data class PhotoEditState(
    val crop: CropTransform = CropTransform(),
    val adjustments: PhotoAdjustments = PhotoAdjustments(),
    val blur: BlurEffectState = BlurEffectState(),
    val curves: ToneCurvesState = ToneCurvesState(),
    val strokes: List<BrushStroke> = emptyList(),
    val textLayers: List<TextLayerItem> = emptyList()
) {
    val hasAnyAdjustments: Boolean
        get() = adjustments.isModified || blur.isModified || curves.isModified

    val hasAnyPaintOrText: Boolean
        get() = strokes.isNotEmpty() || textLayers.isNotEmpty()

    val hasAnyVideoOverlayOrFilter: Boolean
        get() = crop.isModified || hasAnyAdjustments || hasAnyPaintOrText
}

/**
 * Реестр применённого редактирования видео (кисточка, текст, кроп/поворот, цветокоррекция, кривые, размытие),
 * чтобы все изменения применялись ко ВСЕМ кадрам воспроизводимого видео (в редакторе, в пузыре чата и в плеере),
 * а не только к одному статичному кадру-обложке.
 */
object VideoLiveEditRegistry {
    private val statesByUri = java.util.concurrent.ConcurrentHashMap<String, PhotoEditState>()
    private val overlayBitmapsByUri = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()
    private val blurMaskBitmapsByUri = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()

    fun put(
        uriString: String,
        state: PhotoEditState,
        overlayBitmap: android.graphics.Bitmap? = null,
        blurMaskBitmap: android.graphics.Bitmap? = null
    ) {
        if (uriString.isBlank()) return
        if (state.hasAnyVideoOverlayOrFilter) {
            statesByUri[uriString] = state
            if (overlayBitmap != null) {
                overlayBitmapsByUri[uriString] = overlayBitmap
            } else {
                overlayBitmapsByUri.remove(uriString)
            }
            if (blurMaskBitmap != null) {
                blurMaskBitmapsByUri[uriString] = blurMaskBitmap
            } else {
                blurMaskBitmapsByUri.remove(uriString)
            }
        } else {
            statesByUri.remove(uriString)
            overlayBitmapsByUri.remove(uriString)
            blurMaskBitmapsByUri.remove(uriString)
        }
    }

    fun getState(uriString: String?): PhotoEditState? {
        if (uriString.isNullOrBlank()) return null
        return statesByUri[uriString]
    }

    fun getOverlayBitmap(uriString: String?): android.graphics.Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return overlayBitmapsByUri[uriString]
    }

    fun getBlurMaskBitmap(uriString: String?): android.graphics.Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return blurMaskBitmapsByUri[uriString]
    }
}

