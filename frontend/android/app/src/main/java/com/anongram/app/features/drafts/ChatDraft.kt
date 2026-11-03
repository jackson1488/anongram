package com.anongram.app.features.drafts

data class ChatDraft(
    val chatId: String,
    val text: String = "",
    val voiceUri: String? = null,
    val voiceDurationLabel: String? = null,
    val voiceWaveform: List<Int> = emptyList(),
    val videoUri: String? = null
) {
    val isEmpty: Boolean
        get() = text.isBlank() && voiceUri == null && videoUri == null

    val previewText: String
        get() = when {
            text.isNotBlank() -> text
            voiceUri != null -> "Голосовое сообщение"
            videoUri != null -> "Видеосообщение"
            else -> ""
        }
}
