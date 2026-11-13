package com.anongram.web.features.contacts.common

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
            .joinToString("") { it.first().uppercaseChar().toString() }
}
