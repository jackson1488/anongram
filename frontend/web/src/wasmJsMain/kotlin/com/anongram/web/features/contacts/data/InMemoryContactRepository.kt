package com.anongram.web.features.contacts.data

import com.anongram.web.features.contacts.common.Contact

class InMemoryContactRepository : ContactRepository {

    private val contacts = listOf(
        Contact(id = "c1", name = "Алиса", fingerprint = "0x4D9F…88E1", isOnline = true, isSasVerified = true),
        Contact(id = "c2", name = "Боб", fingerprint = "0x1B77…D3A0", isSasVerified = true),
        Contact(id = "c3", name = "Вера Криптон", fingerprint = "0xA50C…12F7", isOnline = true),
        Contact(id = "c4", name = "Дельта", fingerprint = "0x77E0…B4A9")
    )

    override suspend fun loadContacts(): List<Contact> = contacts
}
