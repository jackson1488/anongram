package com.anongram.app.features.chats.mediaeditor.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class BrushType {
    PEN,
    ARROW,
    MARKER,
    NEON,
    ERASER,
    BLUR
}

data class BrushStroke(
    val id: String,
    val type: BrushType,
    val colorArgb: Long,
    /** Normalized width relative to canvas min dimension (e.g. 0.005f .. 0.06f). */
    val strokeWidthNorm: Float,
    /** Points normalized to [0f..1f] in the displayed media box. */
    val points: List<Offset>
)

object EditorPalette {
    // Top row (7) + Bottom row (7) matching Telegram 2x7 palette capsule
    val quickColors: List<Long> = listOf(
        // Row 1
        0xFFFF3B30, // Red
        0xFFFF9500, // Orange
        0xFFFFD60A, // Yellow
        0xFF34C759, // Green
        0xFF64D2FF, // Cyan
        0xFF0A84FF, // Blue
        0xFFBF5AF2, // Purple
        // Row 2
        0xFF000000, // Black
        0xFFFFFFFF, // White
        0xFFD4A373, // Tan
        0xFF70A1FF, // Light Blue
        0xFFA29BFE, // Lavender
        0xFFE899B8, // Pink
        0xFFB8E994  // Light Green
    )

    fun colorFromArgb(argb: Long): Color = Color(argb.toInt())
}
