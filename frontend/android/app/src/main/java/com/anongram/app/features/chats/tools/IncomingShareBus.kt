package com.anongram.app.features.chats.tools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharedIncomingPayload(
    val text: String? = null,
    val uris: List<String> = emptyList()
)

/**
 * Шина входящего контента из системного меню «Поделиться» Android (ACTION_SEND / ACTION_SEND_MULTIPLE).
 */
object IncomingShareBus {
    private val _payload = MutableStateFlow<SharedIncomingPayload?>(null)
    val payload: StateFlow<SharedIncomingPayload?> = _payload.asStateFlow()

    fun emit(text: String?, uris: List<String>) {
        if (text.isNullOrBlank() && uris.isEmpty()) return
        _payload.value = SharedIncomingPayload(text = text?.trim(), uris = uris)
    }

    fun clear() {
        _payload.value = null
    }
}
