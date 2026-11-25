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
    val quickColors: List<Long> = listOf(
        0xFFFFFFFF, // White
        0xFF000000, // Black
        0xFFFF3B30, // Red
        0xFFFF9500, // Orange
        0xFFFFCC00, // Yellow
        0xFF34C759, // Green
        0xFF00C7BE, // Teal
        0xFF32ADE6, // Cyan
        0xFF007AFF, // Blue
        0xFF5856D6, // Indigo
        0xFFAF52DE, // Purple
        0xFFFF2D55, // Pink
        0xFFA2845E, // Brown
        0xFF8E8E93  // Gray
    )

    fun colorFromArgb(argb: Long): Color = Color(argb.toInt())
}
