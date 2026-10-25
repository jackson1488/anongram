package com.anongram.app.features.contacts.common

/**
 * Контакт локального адресного буфера AnonGram. Идентичность — Ed25519
 * отпечаток; SAS-верификация подтверждает отсутствие MITM при личной встрече.
 */
data class Contact(
    val id: String,
    val name: String,
    val fingerprint: String,
    val isOnline: Boolean = false,
    val isSasVerified: Boolean = false
) {
    val monogram: String
        get() = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}
