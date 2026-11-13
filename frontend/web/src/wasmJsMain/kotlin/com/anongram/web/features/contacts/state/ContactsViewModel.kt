package com.anongram.web.features.contacts.state

import com.anongram.web.features.contacts.common.Contact
import com.anongram.web.features.contacts.data.ContactRepository
import com.anongram.web.features.contacts.data.InMemoryContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactRepository = InMemoryContactRepository(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    init {
        scope.launch {
            _contacts.update { repository.loadContacts() }
        }
    }
}
