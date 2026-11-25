package com.anongram.app.features.chats.mediaeditor.model

import java.util.UUID

enum class TextBackgroundStyle {
    NONE,
    FILLED,
    SEMI,
    OUTLINE
}

enum class TextAlignOption {
    LEFT,
    CENTER,
    RIGHT
}

sealed interface EditorFontSpec {
    val id: String

    data class Basic(
        override val id: String,
        val familyKey: String
    ) : EditorFontSpec

    data class ModuleFont(
        val fontId: String,
        val displayName: String,
        val fileName: String
    ) : EditorFontSpec {
        override val id: String get() = "mod:$fontId"
    }

    companion object {
        val defaultBasic = Basic(id = "basic_roboto", familyKey = "roboto")
        val basicFonts: List<Basic> = listOf(
            Basic(id = "basic_roboto", familyKey = "roboto"),
            Basic(id = "basic_italic", familyKey = "italic"),
            Basic(id = "basic_serif", familyKey = "serif"),
            Basic(id = "basic_condensed", familyKey = "condensed"),
            Basic(id = "basic_mono", familyKey = "mono"),
            Basic(id = "basic_bold", familyKey = "bold")
        )
    }
}

data class TextLayerItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    /** Normalized [0f..1f] center position in media box. */
    val normX: Float = 0.5f,
    val normY: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val colorArgb: Long = 0xFFFFFFFF,
    val align: TextAlignOption = TextAlignOption.CENTER,
    val bgStyle: TextBackgroundStyle = TextBackgroundStyle.NONE,
    val fontSpec: EditorFontSpec = EditorFontSpec.defaultBasic
)
