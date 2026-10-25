package com.anongram.app.features.contacts.data

import com.anongram.app.features.contacts.common.Contact

/**
 * Контракт локального хранилища контактов. InMemory-реализация обслуживает
 * этап до Rust-моста; сетевая версия (луковый обмен ключами) заменит её
 * за тем же интерфейсом.
 */
interface ContactRepository {
    suspend fun loadContacts(): List<Contact>
}
