package com.anongram.app.features.chats.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.data.ChatListRepository
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.contacts.data.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Результаты локального поиска по чатам и людям. */
data class ChatSearchResults(
    val hasQuery: Boolean = false,
    val chats: List<IChatSession> = emptyList(),
    val people: List<Contact> = emptyList()
)

/**
 * State-холдер экрана поиска: локальный поиск по чатам (название, отпечаток,
 * превью) и людям (контакты). Глобальный сетевой поиск добавляется отдельным
 * источником после появления сервера.
 */
class ChatSearchViewModel(
    private val chatsRepository: ChatListRepository,
    private val contactsRepository: ContactRepository
) : ViewModel() {

    val query = MutableStateFlow("")

    private val sessions = MutableStateFlow<List<IChatSession>>(emptyList())
    private val contacts = MutableStateFlow<List<Contact>>(emptyList())

    val results: StateFlow<ChatSearchResults> =
        combine(query, sessions, contacts) { q, chats, people ->
            val trimmed = q.trim()
            if (trimmed.isEmpty()) {
                ChatSearchResults(hasQuery = false)
            } else {
                ChatSearchResults(
                    hasQuery = true,
                    chats = chats.filter {
                        it.title.contains(trimmed, true) ||
                            it.fingerprint.contains(trimmed, true) ||
                            it.lastMessagePreview.contains(trimmed, true)
                    }.take(SEARCH_LIMIT),
                    people = people.filter {
                        it.name.contains(trimmed, true) ||
                            it.fingerprint.contains(trimmed, true)
                    }.take(SEARCH_LIMIT)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatSearchResults())

    init {
        viewModelScope.launch {
            sessions.value = chatsRepository.loadSessions()
            contacts.value = contactsRepository.loadContacts()
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    private companion object {
        const val SEARCH_LIMIT = 20
    }
}
