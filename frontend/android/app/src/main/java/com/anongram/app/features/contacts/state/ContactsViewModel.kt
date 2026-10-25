package com.anongram.app.features.contacts.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.contacts.data.ContactRepository
import com.anongram.app.features.contacts.data.InMemoryContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State-холдер раздела контактов. */
class ContactsViewModel(private val repository: ContactRepository) : ViewModel() {

    /** Пустой конструктор для viewModel() до внедрения DI-контейнера. */
    constructor() : this(InMemoryContactRepository())

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    init {
        viewModelScope.launch {
            _contacts.update { repository.loadContacts() }
        }
    }
}
