package com.anongram.web.features.contacts.data

import com.anongram.web.features.contacts.common.Contact

interface ContactRepository {
    suspend fun loadContacts(): List<Contact>
}
