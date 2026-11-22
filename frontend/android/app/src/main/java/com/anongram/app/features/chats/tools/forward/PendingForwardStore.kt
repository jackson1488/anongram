package com.anongram.app.features.chats.tools.forward

import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingForwardState(
    val sourceSession: IChatSession,
    val targetSessionId: String,
    val items: List<ChatMessage>,
    val excludedIds: Set<String> = emptySet()
) {
    val activeItems: List<ChatMessage>
        get() = items.filterNot { it.id in excludedIds }
}

/**
 * Состояние отложенной анонимной пересылки через встроенную плашку поля ввода.
 */
object PendingForwardStore {
    private val _state = MutableStateFlow<PendingForwardState?>(null)
    val state: StateFlow<PendingForwardState?> = _state.asStateFlow()

    fun start(
        sourceSession: IChatSession,
        targetSessionId: String,
        items: List<ChatMessage>
    ) {
        if (items.isEmpty()) return
        _state.value = PendingForwardState(
            sourceSession = sourceSession,
            targetSessionId = targetSessionId,
            items = items
        )
    }

    /** Переключает галочку включения отдельного сообщения/фото; если сняты все — отменяет и возвращает исходный чат. */
    fun toggleInclude(messageId: String): IChatSession? {
        val current = _state.value ?: return null
        val nextExcluded = if (messageId in current.excludedIds) {
            current.excludedIds - messageId
        } else {
            current.excludedIds + messageId
        }
        val remaining = current.items.filterNot { it.id in nextExcluded }
        return if (remaining.isEmpty()) {
            _state.value = null
            current.sourceSession
        } else {
            _state.value = current.copy(excludedIds = nextExcluded)
            null
        }
    }

    /** Отменяет всю пересылку и возвращает исходный чат. */
    fun cancelAll(): IChatSession? {
        val current = _state.value ?: return null
        _state.value = null
        return current.sourceSession
    }

    fun clear() {
        _state.value = null
    }
}
