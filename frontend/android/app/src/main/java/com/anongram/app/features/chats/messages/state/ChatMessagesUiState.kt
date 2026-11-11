package com.anongram.app.features.chats.messages.state

import com.anongram.app.features.chats.messages.domain.ChatMessage

/** Режим записи круглой кнопки: нет / голосовое / видео-кружок. */
enum class RecordMode { NONE, VOICE, CIRCLE }

/** Состояние удержания записи: свайп вверх блокирует, свайп влево отменяет. */
data class RecordingState(
    val mode: RecordMode = RecordMode.NONE,
    val isLocked: Boolean = false,
    val elapsedTenths: Int = 0,
    val finishRequested: Boolean = false
) {
    val seconds: Int get() = elapsedTenths / 10
    val tenths: Int get() = elapsedTenths % 10

    /** «0:07,3» — таймер записи как в ТЗ. */
    val timeLabel: String get() = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')},$tenths"

    val durationLabel: String get() = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** Таймер самоуничтожения сообщений чата (шаг 18). */
enum class SelfDestruct(val label: String, val ms: Long) {
    OFF("Выкл", 0L),
    S5("5 сек", 5_000L),
    M1("1 мин", 60_000L),
    H1("1 час", 3_600_000L),
    D1("1 день", 86_400_000L),
    W1("1 неделя", 604_800_000L)
}

/** Неизменяемое состояние экрана чата: единственный источник истины для UI. */
data class ChatMessagesUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val replyTo: ChatMessage? = null,
    val replyToSubIndex: Int? = null,
    val editing: ChatMessage? = null,
    val editingSubIndex: Int? = null,
    val actionMenuFor: ChatMessage? = null,
    val selection: Set<String> = emptySet(),
    val pinnedId: String? = null,
    val pinnedIds: List<String> = emptyList(),
    val pinnedIndex: Int = 0,
    val limitWarning: String? = null,
    val isPinnedMessagesScreenOpen: Boolean = false,
    val firstUnreadId: String? = null,
    val unreadBadge: Int = 0,
    val isPeerTyping: Boolean = false,
    val peerActivity: String? = null,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchMatches: List<String> = emptyList(),
    val searchIndex: Int = 0,
    val highlightedMessageId: String? = null,
    val recording: RecordingState = RecordingState(),
    val isCameraMode: Boolean = false,
    val isAttachOpen: Boolean = false,
    val isEmojiOpen: Boolean = false,
    val isGeoOpen: Boolean = false,
    val isKeyCheckOpen: Boolean = false,
    val isNotificationsOpen: Boolean = false,
    val isNotificationsMuteDialogOpen: Boolean = false,
    val wallpaperStyle: Int = 0,
    val selfDestruct: SelfDestruct = SelfDestruct.OFF,
    val isFlagSecure: Boolean = false,
    val deleteDialogFor: List<String>? = null,
    val isLoadingOlder: Boolean = false,
    val hasMoreOlder: Boolean = true,
    val expandedVideoCircle: com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle? = null,
    val voiceReviewDraft: com.anongram.app.features.chats.messages.domain.MessageAttachment.Voice? = null,
    val videoReviewDraft: com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle? = null
) {
    val canSend: Boolean
        get() = input.isNotBlank() ||
            (editing?.attachment != null && editing.attachment !is com.anongram.app.features.chats.messages.domain.MessageAttachment.Voice)

    val isSelectionMode: Boolean get() = selection.isNotEmpty()

    /**
     * Строго равноценный обмен 1 на 1: карандаш замены в верхней панели выбора
     * появляется ТОЛЬКО когда выбрано ровно 1 фото (одиночное или 1 ячейка из альбома)
     * либо ровно 1 файл / 1 трек. При выборе 2+ элементов карандаш скрывается.
     */
    val canEditSelectedSingleMedia: Boolean
        get() {
            if (selection.size != 1) return false
            val key = selection.first()
            if ("#photo_" in key || "#file_" in key || "#music_" in key) return true
            val msg = messages.firstOrNull { it.id == key } ?: return false
            return when (val att = msg.attachment) {
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.Photo,
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.File,
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.Music -> true
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.MediaAlbum -> att.items.size == 1
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.FileGroup -> att.items.size == 1
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.MusicGroup -> att.items.size == 1
                else -> false
            }
        }

    val isSingleSelectedPhoto: Boolean
        get() {
            if (selection.size != 1) return false
            val key = selection.first()
            if ("#photo_" in key) return true
            val msg = messages.firstOrNull { it.id == key } ?: return false
            return when (val att = msg.attachment) {
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.Photo -> true
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.MediaAlbum -> att.items.size == 1
                else -> false
            }
        }

    val isSingleSelectedFileOrMusic: Boolean
        get() {
            if (selection.size != 1) return false
            val key = selection.first()
            if ("#file_" in key || "#music_" in key) return true
            val msg = messages.firstOrNull { it.id == key } ?: return false
            return when (val att = msg.attachment) {
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.File,
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.Music -> true
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.FileGroup -> att.items.size == 1
                is com.anongram.app.features.chats.messages.domain.MessageAttachment.MusicGroup -> att.items.size == 1
                else -> false
            }
        }

    fun getSingleSelectedFile(): com.anongram.app.features.chats.messages.domain.MessageAttachment.File? {
        if (selection.size != 1) return null
        val key = selection.first()
        if ("#file_" in key) {
            val msgId = key.substringBefore("#file_")
            val idx = key.substringAfter("#file_").toIntOrNull() ?: 0
            val msg = messages.firstOrNull { it.id == msgId } ?: return null
            val fg = msg.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.FileGroup
            return fg?.items?.getOrNull(idx)
        }
        val msg = messages.firstOrNull { it.id == key } ?: return null
        return when (val att = msg.attachment) {
            is com.anongram.app.features.chats.messages.domain.MessageAttachment.File -> att
            is com.anongram.app.features.chats.messages.domain.MessageAttachment.FileGroup -> att.items.firstOrNull()
            else -> null
        }
    }

    fun getSingleSelectedMusic(): com.anongram.app.features.chats.messages.domain.MessageAttachment.Music? {
        if (selection.size != 1) return null
        val key = selection.first()
        if ("#music_" in key) {
            val msgId = key.substringBefore("#music_")
            val idx = key.substringAfter("#music_").toIntOrNull() ?: 0
            val msg = messages.firstOrNull { it.id == msgId } ?: return null
            val mg = msg.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.MusicGroup
            return mg?.items?.getOrNull(idx)
        }
        val msg = messages.firstOrNull { it.id == key } ?: return null
        return when (val att = msg.attachment) {
            is com.anongram.app.features.chats.messages.domain.MessageAttachment.Music -> att
            is com.anongram.app.features.chats.messages.domain.MessageAttachment.MusicGroup -> att.items.firstOrNull()
            else -> null
        }
    }

    val isSingleSelectedVideoCircle: Boolean
        get() {
            if (selection.size != 1) return false
            val key = selection.first()
            val msg = messages.firstOrNull { it.id == key } ?: return false
            return msg.attachment is com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle
        }

    val isSingleSelectedVoice: Boolean
        get() {
            if (selection.size != 1) return false
            val key = selection.first()
            val msg = messages.firstOrNull { it.id == key } ?: return false
            return msg.attachment is com.anongram.app.features.chats.messages.domain.MessageAttachment.Voice
        }

    fun getSingleSelectedVideoCircle(): com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle? {
        if (selection.size != 1) return null
        val key = selection.first()
        val msg = messages.firstOrNull { it.id == key } ?: return null
        return msg.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle
    }

    fun getSingleSelectedVoice(): com.anongram.app.features.chats.messages.domain.MessageAttachment.Voice? {
        if (selection.size != 1) return null
        val key = selection.first()
        val msg = messages.firstOrNull { it.id == key } ?: return null
        return msg.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.Voice
    }

    val selectedMessages: List<ChatMessage>
        get() = messages.filter { it.id in selection }

    val pinnedMessages: List<ChatMessage>
        get() = messages.filter { it.id in pinnedIds }

    val pinnedMessage: ChatMessage?
        get() {
            val targetId = pinnedIds.getOrNull(pinnedIndex) ?: pinnedId ?: pinnedIds.lastOrNull()
            return messages.firstOrNull { it.id == targetId }
        }

    /** «3 из 12» для панели поиска. */
    val searchCounter: String
        get() = if (searchMatches.isEmpty() || searchQuery.isBlank()) "" else "${searchIndex + 1} из ${searchMatches.size}"

    /** Редактирование разрешено только своим сообщениям и в пределах 48 часов. */
    fun isEditable(message: ChatMessage, nowMs: Long): Boolean =
        message.direction == com.anongram.app.features.chats.messages.domain.MessageDirection.OUTGOING &&
            nowMs - message.epochMs <= EDIT_WINDOW_MS

    private companion object {
        const val EDIT_WINDOW_MS = 48 * 3_600_000L
    }
}
