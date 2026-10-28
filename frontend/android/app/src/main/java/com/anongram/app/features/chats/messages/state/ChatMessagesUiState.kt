package com.anongram.app.features.chats.messages.state

import com.anongram.app.features.chats.messages.domain.ChatMessage

/** Режим записи круглой кнопки: нет / голосовое / видео-кружок. */
enum class RecordMode { NONE, VOICE, CIRCLE }

/** Состояние удержания записи: свайп вверх блокирует, свайп влево отменяет. */
data class RecordingState(
    val mode: RecordMode = RecordMode.NONE,
    val isLocked: Boolean = false,
    val elapsedTenths: Int = 0
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
    val editing: ChatMessage? = null,
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
    val hasMoreOlder: Boolean = true
) {
    val canSend: Boolean get() = input.isNotBlank()

    val isSelectionMode: Boolean get() = selection.isNotEmpty()

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
