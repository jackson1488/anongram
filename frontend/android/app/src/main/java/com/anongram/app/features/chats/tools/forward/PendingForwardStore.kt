package com.anongram.app.features.chats.tools.forward

import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingForwardItem(
    val key: String,
    val resolvedMessage: ChatMessage
)

data class PendingForwardState(
    val sourceSession: IChatSession,
    val targetSessionId: String,
    val items: List<PendingForwardItem>,
    val excludedKeys: Set<String> = emptySet()
) {
    val activeItems: List<PendingForwardItem>
        get() = items.filterNot { it.key in excludedKeys }
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
        items: List<PendingForwardItem>
    ) {
        if (items.isEmpty()) return
        _state.value = PendingForwardState(
            sourceSession = sourceSession,
            targetSessionId = targetSessionId,
            items = items
        )
    }

    /** Переключает галочку включения отдельного сообщения/фото; если сняты все — отменяет. */
    fun toggleInclude(key: String): Boolean {
        val current = _state.value ?: return false
        val nextExcluded = if (key in current.excludedKeys) {
            current.excludedKeys - key
        } else {
            current.excludedKeys + key
        }
        val remaining = current.items.filterNot { it.key in nextExcluded }
        return if (remaining.isEmpty()) {
            _state.value = null
            true
        } else {
            _state.value = current.copy(excludedKeys = nextExcluded)
            false
        }
    }

    fun clear() {
        _state.value = null
    }
}
