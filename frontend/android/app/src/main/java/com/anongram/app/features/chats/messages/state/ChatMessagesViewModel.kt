package com.anongram.app.features.chats.messages.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.chats.messages.data.ChatMessagesRepository
import com.anongram.app.features.chats.messages.data.InMemoryChatMessagesRepository
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State-холдер открытого чата (логика Telegram): отправка с эволюцией статуса
 * доставки, ответы-реплаи, удаление, очистка истории и живой ответ собеседника
 * с индикатором «печатает…». Compose-слой только читает [uiState] и зовёт интенты.
 */
class ChatMessagesViewModel(
    private val sessionId: String,
    private val repository: ChatMessagesRepository
) : ViewModel() {

    /** Пустой конструктор для viewModel() до внедрения DI-контейнера. */
    constructor() : this("", InMemoryChatMessagesRepository())

    private val _uiState = MutableStateFlow(ChatMessagesUiState())
    val uiState: StateFlow<ChatMessagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.loadMessages(sessionId)
            _uiState.update { it.copy(messages = loaded) }
        }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(input = value) }

    fun startReply(message: ChatMessage) = _uiState.update { it.copy(replyTo = message) }

    fun cancelReply() = _uiState.update { it.copy(replyTo = null) }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(sessionId, messageId)
            _uiState.update { state ->
                state.copy(messages = state.messages.filterNot { it.id == messageId })
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory(sessionId)
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    fun send() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty()) return
        val replySnippet = _uiState.value.replyTo?.text?.take(SNIPPET_LIMIT)
        val message = ChatMessage(
            id = "out_${System.nanoTime()}",
            direction = MessageDirection.OUTGOING,
            text = text,
            epochMs = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            replyToSnippet = replySnippet
        )
        viewModelScope.launch {
            repository.addMessage(sessionId, message)
            _uiState.update { state ->
                state.copy(messages = state.messages + message, input = "", replyTo = null)
            }
            runDeliveryLifecycle(message.id)
        }
    }

    /** Часы → галочка → двойная галочка → «печатает…» → ответ собеседника. */
    private suspend fun runDeliveryLifecycle(messageId: String) {
        delay(600)
        setStatus(messageId, MessageStatus.SENT)
        delay(1_100)
        setStatus(messageId, MessageStatus.READ)
        _uiState.update { it.copy(isPeerTyping = true) }
        delay(1_800)
        val reply = repository.generateAutoReply(sessionId)
        _uiState.update { state ->
            state.copy(
                isPeerTyping = false,
                messages = reply?.let { state.messages + it } ?: state.messages
            )
        }
    }

    private suspend fun setStatus(messageId: String, status: MessageStatus) {
        repository.updateStatus(sessionId, messageId, status)
        _uiState.update { state ->
            state.copy(messages = state.messages.map {
                if (it.id == messageId) it.copy(status = status) else it
            })
        }
    }

    private companion object {
        const val SNIPPET_LIMIT = 80
    }
}
