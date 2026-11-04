package com.anongram.app.features.drafts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatDraftsRepository private constructor() {
    private val _drafts = MutableStateFlow<Map<String, ChatDraft>>(emptyMap())
    val drafts: StateFlow<Map<String, ChatDraft>> = _drafts.asStateFlow()

    fun getDraft(chatId: String): ChatDraft? = _drafts.value[chatId]

    fun saveDraft(draft: ChatDraft) {
        _drafts.update { current ->
            if (draft.isEmpty) {
                current - draft.chatId
            } else {
                val fresh = draft.copy(updatedAtEpochMs = System.currentTimeMillis())
                current + (draft.chatId to fresh)
            }
        }
    }

    fun clearDraft(chatId: String) {
        _drafts.update { it - chatId }
    }

    companion object {
        val instance = ChatDraftsRepository()
    }
}
