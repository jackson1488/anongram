package com.anongram.app.features.drafts

data class ChatDraft(
    val chatId: String,
    val text: String = "",
    val replyToMessageId: String? = null,
    val replyToSubIndex: Int? = null,
    val voiceUri: String? = null,
    val voiceDurationLabel: String? = null,
    val voiceWaveform: List<Int> = emptyList(),
    val videoUri: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean
        get() = text.isBlank() && replyToMessageId == null && voiceUri == null && videoUri == null

    val previewText: String
        get() {
            return when {
                text.isNotBlank() -> text
                voiceUri != null -> "Голосовое сообщение"
                videoUri != null -> "Видеосообщение"
                else -> ""
            }
        }
}
