package com.anongram.web.features.chats.search

import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.data.ChatListRepository
import com.anongram.web.features.contacts.common.Contact
import com.anongram.web.features.contacts.data.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatSearchResults(
    val hasQuery: Boolean = false,
    val chats: List<IChatSession> = emptyList(),
    val people: List<Contact> = emptyList()
)

class ChatSearchViewModel(
    private val chatsRepository: ChatListRepository,
    private val contactsRepository: ContactRepository,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
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
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ChatSearchResults())

    init {
        scope.launch {
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
